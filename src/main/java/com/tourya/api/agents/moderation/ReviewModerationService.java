package com.tourya.api.agents.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tourya.api.agents.shared.AgentAuditEntry;
import com.tourya.api.agents.shared.AgentAuditWriter;
import com.tourya.api.agents.shared.AgentLlmResponse;
import com.tourya.api.agents.shared.BudgetGuard;
import com.tourya.api.agents.shared.ILlmClient;
import com.tourya.api.agents.shared.ModelPricing;
import com.tourya.api.agents.shared.PromptTemplate;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Review;
import com.tourya.api.models.ReviewAttachment;
import com.tourya.api.models.Tour;
import com.tourya.api.models.User;
import com.tourya.api.repository.ReviewAttachmentRepository;
import com.tourya.api.repository.ReviewRepository;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * IA-10: Agente 6 del doc 16 — Moderacion de reseñas del turista.
 *
 * <p><b>Rol:</b> moderador asistido por LLM. Cuando una reseña se persiste
 * ({@code ReviewService.createReview}), se publica un
 * {@link ReviewModerationEvent} y este service evalua texto + medios y devuelve
 * un {@link ModerationResult} con {@code decision} +
 * {@code flags[]} + {@code reasoning}. El resultado se persiste como metadata
 * paralela en las columnas {@code review.moderation_*} (migracion 093).</p>
 *
 * <p><b>Contrato de negocio (RN-050 hoy):</b></p>
 * <ul>
 *   <li>El agente <b>NUNCA</b> cambia {@code review.status} — la reseña sigue
 *       publicandose por default (PUBLISHED al crear).</li>
 *   <li>El agente <b>NUNCA</b> borra ni modifica {@code review.comment} — el
 *       texto del turista queda intacto.</li>
 *   <li>El agente <b>NUNCA</b> contacta al turista — el veredicto solo se lee
 *       desde el backoffice.</li>
 *   <li>El agente <b>auto-flaggea</b> — backoffice filtra por
 *       {@code moderation_status = PENDING|REJECTED} en su listado.</li>
 * </ul>
 *
 * <p><b>Guardrails no negociables (implementados en codigo, no solo en el prompt):</b></p>
 * <ul>
 *   <li>Deny-list de secretos identico a IA-02/IA-07 en el texto de la reseña —
 *       si aparece un secret keyword se rechaza sin llamar al LLM y se audita
 *       como {@code rejected}. El veredicto pasa a PENDING para revision humana
 *       (no marca REJECTED — el turista podria haber pegado un ejemplo, no
 *       necesariamente es spam).</li>
 *   <li>Budget guard: si {@link BudgetGuard#canRun} devuelve {@code false} se
 *       persiste PENDING (escalatedToHuman=true) sin llamar al LLM.</li>
 *   <li>Modo no-op: si {@code agents.moderation.enabled=false} el service NO
 *       persiste nada (respeta review pre-existentes) y se audita como
 *       {@code rejected} con {@code disabled} — mismo patron que IA-09.</li>
 * </ul>
 *
 * <p><b>Modelo:</b> {@code gemini-2.5-flash} — clasificacion + baja latencia +
 * ~4x mas barato que Gemini 2.5 Pro. Consistente con la decision Sprint 8/9
 * (IA-02 y IA-07 usan Vertex AI Gemini via ADC).</p>
 *
 * <p><b>Costo esperado:</b> $0.30 - $0.60 / mes con volumen actual (docenas de
 * reseñas / mes). Cap por BudgetGuard = $10 / mes (default en
 * {@code BudgetGuard.DEFAULT_MONTHLY_CAP} para {@code ReviewModerator}).</p>
 */
@Slf4j
@Service
public class ReviewModerationService {

    private static final String AGENT_NAME = "ReviewModerator";
    private static final String PROMPT_VERSION = "v1";

    /** Deny-list identica a IA-02 / IA-07 (fuente unica cuando se refactorice a GuardrailUtils — TODO IA-11c). */
    private static final String[] SECRET_KEYWORDS = {
            "WOMPI_INTEGRITY_SECRET",
            "WOMPI_EVENTS_SECRET",
            "JWT_SECRET",
            "ANTHROPIC_API_KEY",
            "FIREBASE_ADMIN_SDK_JSON",
            "GEMINI_API_KEY"
    };

    private final ILlmClient llmClient;
    private final AgentAuditWriter auditWriter;
    private final BudgetGuard budgetGuard;
    private final ReviewRepository reviewRepository;
    private final ReviewAttachmentRepository reviewAttachmentRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final ReviewModerationApplier applier;
    private final ObjectMapper objectMapper;
    private final PromptTemplate moderatePrompt;
    private final boolean enabled;
    private final String defaultModel;

    public ReviewModerationService(
            ILlmClient llmClient,
            AgentAuditWriter auditWriter,
            BudgetGuard budgetGuard,
            ReviewRepository reviewRepository,
            ReviewAttachmentRepository reviewAttachmentRepository,
            TourRepository tourRepository,
            UserRepository userRepository,
            ReviewModerationApplier applier,
            ObjectMapper objectMapper,
            @Value("${agents.moderation.enabled:true}") boolean enabled,
            @Value("${agents.moderation.model:gemini-2.5-flash}") String defaultModel) {
        this.llmClient = llmClient;
        this.auditWriter = auditWriter;
        this.budgetGuard = budgetGuard;
        this.reviewRepository = reviewRepository;
        this.reviewAttachmentRepository = reviewAttachmentRepository;
        this.tourRepository = tourRepository;
        this.userRepository = userRepository;
        this.applier = applier;
        this.objectMapper = objectMapper;
        this.moderatePrompt = PromptTemplate.load("review-moderation/moderate.v1");
        this.enabled = enabled;
        this.defaultModel = (defaultModel == null || defaultModel.isBlank())
                ? ModelPricing.GEMINI_2_5_FLASH : defaultModel;
    }

    // ================================================================
    // Public entry-points
    // ================================================================

    /**
     * Version async invocada por el {@link ReviewModerationEventListener} tras
     * {@code createReview}. Fire-and-forget — no lanza excepciones al caller.
     * El listener corre en AFTER_COMMIT, este metodo corre en su propio hilo.
     */
    @Async
    public void moderateAsync(Long reviewId) {
        try {
            ModerationResult result = moderateInternal(reviewId, /* fromAdmin */ false);
            applier.applyAsync(reviewId, result, flagsToJsonString(result.flags()));
        } catch (Exception ex) {
            log.error("IA-10 async moderation failed reviewId={}", reviewId, ex);
        }
    }

    /**
     * Version sincrona para el endpoint admin de re-moderacion.
     * Devuelve el resultado al caller que responde el HTTP.
     *
     * @throws ResourceNotFoundException si la reseña no existe.
     */
    public ModerationResult moderate(Long reviewId) {
        ModerationResult result = moderateInternal(reviewId, /* fromAdmin */ true);
        applier.applySync(reviewId, result, flagsToJsonString(result.flags()));
        return result;
    }

    // ================================================================
    // Core moderation logic
    // ================================================================

    private ModerationResult moderateInternal(Long reviewId, boolean fromAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: id=" + reviewId));

        // 1. Feature flag: modo no-op degradado (audit + PENDING escalado).
        if (!enabled) {
            log.debug("IA-10 disabled — reviewId={} skipping LLM call", reviewId);
            writeAudit(reviewId, null, AgentLlmResponse.disabled(), 0L,
                    "rejected", "moderation_disabled", true, fromAdmin);
            return pendingEscalated("El servicio de moderacion esta desactivado.");
        }

        // 2. Extraer texto de la reseña (comment.es — el turista escribe en su idioma,
        //    el LLM detecta idioma solo si es necesario).
        String reviewText = extractText(review);

        // 3. Deny-list — el turista podria pegar un secreto por accidente o intencion.
        //    NO clasificamos SPAM directamente (podria ser paste inocente) — escalamos
        //    a PENDING y auditamos "rejected/secret_keyword_in_review".
        if (containsSecret(reviewText)) {
            log.warn("IA-10 secret keyword detected in review reviewId={} — escalating without LLM call",
                    reviewId);
            writeAudit(reviewId, review.getTourId(), AgentLlmResponse.disabled(), 0L,
                    "rejected", "secret_keyword_in_review", true, fromAdmin);
            return pendingEscalated("El comentario contiene una palabra clave sensible; requiere revision humana.");
        }

        // 4. Budget guard — mismo patron que los otros agentes.
        if (!budgetGuard.canRun(AGENT_NAME)) {
            writeAudit(reviewId, review.getTourId(), AgentLlmResponse.disabled(), 0L,
                    "rejected", "budget_exhausted", true, fromAdmin);
            return pendingEscalated("Presupuesto mensual del agente agotado; se requiere revision manual.");
        }

        // 5. Enriquecer contexto para el prompt (tour + provider + author + medios).
        ModerationRequest request = buildRequest(review, reviewText);

        Map<String, Object> vars = new HashMap<>();
        vars.put("reviewId", request.reviewId());
        vars.put("reviewText", jsonEscape(nullSafe(request.reviewText())));
        vars.put("rating", request.rating() == null ? "" : request.rating().toPlainString());
        vars.put("tourName", jsonEscape(nullSafe(request.tourName())));
        vars.put("tourSubcategory", nullSafe(request.tourSubcategory()));
        vars.put("providerName", jsonEscape(nullSafe(request.providerName())));
        vars.put("authorEmailDomain", authorEmailDomain(request.authorEmail()));
        vars.put("mediaCount", request.mediaUrls() == null ? 0 : request.mediaUrls().size());

        String prompt = moderatePrompt.render(vars);

        Instant start = Instant.now();
        AgentLlmResponse response = llmClient.complete(prompt, defaultModel);
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        // 6. Manejo de error del LLM (network, disabled, rate limit) — nunca throws.
        if (response.isError()) {
            log.warn("IA-10 LLM error reviewId={} error={}", reviewId, response.error());
            writeAudit(reviewId, review.getTourId(), response, durationMs,
                    "error", response.error(), true, fromAdmin);
            return pendingEscalated("El agente no pudo evaluar la reseña en este momento; requiere revision manual.");
        }

        // 7. Parsear con fallback conservador (PENDING escalado si el JSON esta roto).
        ModerationResult result = parseOrEscalate(response.content());

        writeAudit(reviewId, review.getTourId(), response, durationMs,
                result.escalatedToHuman() ? "error" : "suggestion",
                result.escalatedToHuman() ? "malformed_llm_json" : null,
                result.escalatedToHuman(), fromAdmin);

        return result;
    }

    // ================================================================
    // Context enrichment
    // ================================================================

    private ModerationRequest buildRequest(Review review, String reviewText) {
        String tourName = null;
        String tourSubcategory = null;
        String providerName = null;
        if (review.getTourId() != null) {
            Tour tour = tourRepository.findById(review.getTourId()).orElse(null);
            if (tour != null) {
                tourName = tour.getName() == null ? null : tour.getName().getEs();
                tourSubcategory = tour.getSubCategory() == null ? null : tour.getSubCategory().getValue();
                Provider provider = tour.getProvider();
                if (provider != null) {
                    providerName = provider.getName();
                }
            }
        }
        String authorEmail = null;
        if (review.getUserId() != null) {
            User user = userRepository.findById(review.getUserId()).orElse(null);
            if (user != null) {
                authorEmail = user.getEmail();
            }
        }
        List<String> mediaUrls = loadMediaUrls(review.getId());

        return ModerationRequest.builder()
                .reviewId(review.getId())
                .reviewText(reviewText)
                .rating(review.getRating())
                .mediaUrls(mediaUrls)
                .tourName(tourName)
                .tourSubcategory(tourSubcategory)
                .providerName(providerName)
                .authorEmail(authorEmail)
                .build();
    }

    private List<String> loadMediaUrls(Long reviewId) {
        try {
            List<ReviewAttachment> attachments = reviewAttachmentRepository.findByReviewId(reviewId);
            if (attachments == null || attachments.isEmpty()) return List.of();
            List<String> urls = new ArrayList<>(attachments.size());
            for (ReviewAttachment att : attachments) {
                if (att.getFileUrl() != null) urls.add(att.getFileUrl());
            }
            return urls;
        } catch (Exception ex) {
            log.debug("IA-10 attachment lookup failed reviewId={}: {}", reviewId, ex.getMessage());
            return List.of();
        }
    }

    private static String extractText(Review review) {
        if (review.getComment() == null) return "";
        String es = review.getComment().getEs();
        return es == null ? "" : es;
    }

    /**
     * Solo exponemos el dominio del email (no el local-part) al LLM — cero PII
     * en el prompt. El {@code POTENTIAL_FRAUD} puede beneficiarse de saber si
     * el email viene de un dominio descartable ({@code mailinator.com},
     * {@code tempmail.com}, etc.), no necesita el usuario completo.
     */
    private static String authorEmailDomain(String email) {
        if (email == null || email.isBlank()) return "";
        int at = email.indexOf('@');
        return at >= 0 && at < email.length() - 1 ? email.substring(at + 1) : "";
    }

    // ================================================================
    // Parsing defensivo
    // ================================================================

    ModerationResult parseOrEscalate(String content) {
        try {
            String json = stripFences(content);
            JsonNode root = objectMapper.readTree(json);
            String decisionRaw = root.path("decision").asText("");
            List<String> flags = readStringArray(root.path("flags"));
            String reasoning = root.path("reasoning").asText("");
            ModerationResult.Decision decision = parseDecision(decisionRaw);
            if (decision == null) {
                log.warn("IA-10 malformed decision in LLM response: '{}'", decisionRaw);
                return pendingEscalated("El agente devolvio un veredicto no valido.");
            }
            // Sanidad: si APPROVED con flags, forzamos PENDING (el LLM se contradijo).
            if (decision == ModerationResult.Decision.APPROVED && !flags.isEmpty()) {
                log.warn("IA-10 LLM returned APPROVED with flags={}, forcing PENDING", flags);
                return ModerationResult.builder()
                        .decision(ModerationResult.Decision.PENDING)
                        .flags(flags)
                        .reasoning("Contradiccion en la respuesta del agente: APPROVED con flags. Requiere revision.")
                        .escalatedToHuman(true)
                        .build();
            }
            return ModerationResult.builder()
                    .decision(decision)
                    .flags(sanitizeFlags(flags))
                    .reasoning(reasoning)
                    .escalatedToHuman(false)
                    .build();
        } catch (Exception ex) {
            log.warn("IA-10 malformed LLM JSON: {}", ex.getMessage());
            return pendingEscalated("El agente devolvio una respuesta ilegible; requiere revision manual.");
        }
    }

    private static ModerationResult.Decision parseDecision(String raw) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase(Locale.ROOT);
        return switch (up) {
            case "APPROVED" -> ModerationResult.Decision.APPROVED;
            case "PENDING", "PENDING_MODERATION" -> ModerationResult.Decision.PENDING;
            case "REJECTED" -> ModerationResult.Decision.REJECTED;
            default -> null;
        };
    }

    private static List<String> sanitizeFlags(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        List<String> out = new ArrayList<>(raw.size());
        for (String f : raw) {
            if (f == null) continue;
            String up = f.trim().toUpperCase(Locale.ROOT);
            if (up.equals(ModerationResult.Flag.SPAM)
                    || up.equals(ModerationResult.Flag.OFFENSIVE)
                    || up.equals(ModerationResult.Flag.OFF_TOPIC)
                    || up.equals(ModerationResult.Flag.POTENTIAL_FRAUD)
                    || up.equals(ModerationResult.Flag.INAPPROPRIATE_MEDIA)) {
                if (!out.contains(up)) out.add(up);
            }
            // Flags fuera del dominio se ignoran (defensivo — el LLM no debe inventar).
        }
        return out;
    }

    private static List<String> readStringArray(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(el -> {
                if (el != null && el.isTextual()) out.add(el.asText());
            });
        }
        return out;
    }

    private static String stripFences(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            int lastBackticks = trimmed.lastIndexOf("```");
            if (firstNl > 0 && lastBackticks > firstNl) {
                trimmed = trimmed.substring(firstNl + 1, lastBackticks).trim();
            }
        }
        return trimmed;
    }

    // ================================================================
    // Guardrails
    // ================================================================

    boolean containsSecret(String input) {
        if (input == null) return false;
        String upper = input.toUpperCase(Locale.ROOT);
        for (String kw : SECRET_KEYWORDS) {
            if (upper.contains(kw)) return true;
        }
        return false;
    }

    // ================================================================
    // Fallback + audit
    // ================================================================

    private static ModerationResult pendingEscalated(String reasoning) {
        return ModerationResult.builder()
                .decision(ModerationResult.Decision.PENDING)
                .flags(List.of())
                .reasoning(reasoning)
                .escalatedToHuman(true)
                .build();
    }

    private void writeAudit(Long reviewId, Integer tourId, AgentLlmResponse response,
                            long durationMs, String resultType, String errorMessage,
                            boolean escalated, boolean fromAdmin) {
        try {
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("capability", "moderate_review");
            metadata.put("escalated_to_human", escalated);
            metadata.put("from_admin_endpoint", fromAdmin);
            if (tourId != null) metadata.put("tour_id", tourId);

            AgentAuditEntry entry = AgentAuditEntry.builder()
                    .agentName(AGENT_NAME)
                    .model(response.model() == null ? defaultModel : response.model())
                    .promptVersion(PROMPT_VERSION)
                    .inputTokens(response.inputTokens())
                    .outputTokens(response.outputTokens())
                    .costUsd(response.costUsd())
                    .durationMs(durationMs)
                    .entityType("review")
                    .entityId(reviewId)
                    .userId(null)
                    .resultType(resultType)
                    .resultJson(null)
                    .promptInput(null)
                    .errorMessage(errorMessage)
                    .metadata(objectMapper.writeValueAsString(metadata))
                    .build();
            auditWriter.register(entry);
        } catch (Exception ex) {
            log.error("IA-10 audit write failed reviewId={}", reviewId, ex);
        }
    }

    // ================================================================
    // Misc helpers
    // ================================================================

    String flagsToJsonString(List<String> flags) {
        try {
            ArrayNode arr = objectMapper.createArrayNode();
            if (flags != null) flags.forEach(arr::add);
            return objectMapper.writeValueAsString(arr);
        } catch (Exception ex) {
            log.warn("IA-10 flagsToJsonString failed — persisting empty array: {}", ex.getMessage());
            return "[]";
        }
    }

    private static String nullSafe(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }

    // ================================================================
    // Test hooks (package-private)
    // ================================================================

    ModerationResult moderateForTest(Long reviewId) {
        return moderateInternal(reviewId, /* fromAdmin */ false);
    }

    /** Escapa el resultado para que los tests puedan invocar el parser directo. */
    ModerationResult parseForTest(String llmContent) {
        return parseOrEscalate(llmContent);
    }
}
