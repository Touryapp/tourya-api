package com.tourya.api.agents.operator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tourya.api._utils.Utils;
import com.tourya.api.agents.shared.AgentAuditEntry;
import com.tourya.api.agents.shared.AgentAuditWriter;
import com.tourya.api.agents.shared.AgentLlmResponse;
import com.tourya.api.agents.shared.BudgetGuard;
import com.tourya.api.agents.shared.ILlmClient;
import com.tourya.api.agents.shared.ModelPricing;
import com.tourya.api.agents.shared.PromptTemplate;
import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Review;
import com.tourya.api.models.Role;
import com.tourya.api.models.Tour;
import com.tourya.api.models.User;
import com.tourya.api.models.responses.TourTagResponse;
import com.tourya.api.repository.OperatorSupportRepository;
import com.tourya.api.repository.ReviewRepository;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.services.AppConfigService;
import com.tourya.api.services.GalleryValidator;
import com.tourya.api.services.ProviderService;
import com.tourya.api.services.TourTagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * IA-07: Agente 4 del doc 16 — Operator Support.
 *
 * <p>Responsabilidades MVP (ver doc 16 §Agente 4):</p>
 * <ol>
 *   <li>Sugerir nombre + descripcion SEO-friendly + tags para el wizard del
 *       tour ({@link #suggestTourContent}).</li>
 *   <li>Alertar sobre precio desalineado vs tours comparables — <b>solo alerta</b>
 *       (RN-014, {@link #priceAlert}).</li>
 *   <li>Borrador de respuesta a una reseña ({@link #draftReviewReply}).</li>
 *   <li>Validacion pre-upload de galeria — sin LLM
 *       ({@link #validateGallery}).</li>
 * </ol>
 *
 * <p><b>Guardrails no negociables (implementados en codigo, no solo en el prompt):</b></p>
 * <ul>
 *   <li>Deny-list de secretos identico a IA-02 en cualquier input textual —
 *       la request se rechaza y se audita como {@code rejected}.</li>
 *   <li>Scrub de {@code providerPrice} / {@code slotPercentageTourya} en todo
 *       JSON que va al LLM (defense-in-depth).</li>
 *   <li>Autorizacion owner-based: el operador solo puede operar sobre tours de
 *       su provider (grupo o principal).</li>
 *   <li>Budget guard: si {@link BudgetGuard#canRun} devuelve {@code false} se
 *       escala a humano sin llamar al LLM.</li>
 * </ul>
 *
 * <p><b>Traduccion es→en/pt</b> (IA-09, Google Cloud Translation, 2026-08-15):
 * este service sigue devolviendo la sugerencia en espanol (RN-011: el operador
 * aprueba en su idioma), pero cuando el tour se persiste el
 * {@link com.tourya.api.services.translation.TourTranslationEventListener}
 * traduce automaticamente los campos JSONB a en/pt-BR en background. No hay
 * coupling directo entre IA-07 e IA-09 — el hook vive en
 * {@link com.tourya.api.services.TourService#saveCreateOrUpdateFullData}.</p>
 */
@Slf4j
@Service
public class OperatorSupportService {

    private static final String AGENT_NAME = "OperatorSupport";
    private static final String PROMPT_VERSION = "v1";
    /** Razonamiento + redaccion — Gemini 2.5 Pro. */
    private static final String DEFAULT_MODEL = ModelPricing.GEMINI_2_5_PRO;

    /** Limite defensivo para el set de comparables — 20 tours es mas que suficiente. */
    private static final int COMPARABLES_MAX = 20;

    /** Deny-list identica a IA-02 (fuente unica cuando se refactorice a GuardrailUtils — TODO IA-11). */
    private static final String[] SECRET_KEYWORDS = {
            "WOMPI_INTEGRITY_SECRET",
            "WOMPI_EVENTS_SECRET",
            "JWT_SECRET",
            "ANTHROPIC_API_KEY",
            "FIREBASE_ADMIN_SDK_JSON",
            "GEMINI_API_KEY"
    };

    /** Campos internos que NUNCA deben llegar al LLM. */
    private static final String[] SCRUBBED_FIELDS = {
            "slotPercentageTourya", "slotPorcentajeTourya",
            "providerPrice", "porcentajeTourya"
    };

    private final ILlmClient llmClient;
    private final AgentAuditWriter auditWriter;
    private final BudgetGuard budgetGuard;
    private final TourRepository tourRepository;
    private final ReviewRepository reviewRepository;
    private final ProviderService providerService;
    private final TourTagService tourTagService;
    private final GalleryValidator galleryValidator;
    private final AppConfigService appConfigService;
    private final OperatorSupportRepository operatorSupportRepository;
    private final ObjectMapper objectMapper;

    private final PromptTemplate suggestContentPrompt;
    private final PromptTemplate priceAlertPrompt;
    private final PromptTemplate draftReplyPrompt;

    public OperatorSupportService(
            ILlmClient llmClient,
            AgentAuditWriter auditWriter,
            BudgetGuard budgetGuard,
            TourRepository tourRepository,
            ReviewRepository reviewRepository,
            ProviderService providerService,
            TourTagService tourTagService,
            GalleryValidator galleryValidator,
            AppConfigService appConfigService,
            OperatorSupportRepository operatorSupportRepository,
            ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.auditWriter = auditWriter;
        this.budgetGuard = budgetGuard;
        this.tourRepository = tourRepository;
        this.reviewRepository = reviewRepository;
        this.providerService = providerService;
        this.tourTagService = tourTagService;
        this.galleryValidator = galleryValidator;
        this.appConfigService = appConfigService;
        this.operatorSupportRepository = operatorSupportRepository;
        this.objectMapper = objectMapper;
        this.suggestContentPrompt = PromptTemplate.load("operator-support/suggest-tour-content.v1");
        this.priceAlertPrompt = PromptTemplate.load("operator-support/price-alert.v1");
        this.draftReplyPrompt = PromptTemplate.load("operator-support/draft-review-reply.v1");
    }

    // ============================================================
    // Endpoint 1 — suggest tour content
    // ============================================================

    /**
     * Sugiere nombres + descripcion SEO en espanol + tags.
     *
     * @param request draft con lo que el operador cargo en el wizard.
     * @param auth    JWT del PROVIDER / PROVIDER_OPERATOR autenticado.
     */
    public TourContentSuggestion suggestTourContent(SuggestTourContentRequest request, Authentication auth) {
        requireProviderRole(auth);

        // Autorizacion — si trae tourId, validar ownership.
        if (request.getTourId() != null) {
            requireTourOwnership(request.getTourId(), auth);
        }

        // Deny-list — el operador podria intentar leak (defense-in-depth).
        TourDraft draft = request.getDraft();
        if (draft != null && (containsSecret(draft.name()) || containsSecret(draft.currentDescription()))) {
            log.warn("IA-07 secret keyword detected in suggest-tour-content input tourId={}",
                    request.getTourId());
            writeAudit("suggest_tour_content", "tour",
                    request.getTourId() == null ? null : request.getTourId().longValue(),
                    auth, AgentLlmResponse.disabled(), 0L,
                    "rejected", "secret_keyword_in_input", true);
            return escalatedSuggestion();
        }

        if (!budgetGuard.canRun(AGENT_NAME)) {
            writeAudit("suggest_tour_content", "tour",
                    request.getTourId() == null ? null : request.getTourId().longValue(),
                    auth, AgentLlmResponse.disabled(), 0L,
                    "rejected", "budget_exhausted", true);
            return escalatedSuggestion();
        }

        // Cargar catalogo de tags (scrub no aplica — es publico).
        String tagsListing = renderTagsCatalog();

        // Rendereo del prompt.
        Map<String, Object> vars = new HashMap<>();
        vars.put("draftName", nullSafe(draft == null ? null : draft.name()));
        vars.put("draftCategoryId", nullSafe(draft == null ? null : draft.categoryId()));
        vars.put("draftSubcategory", nullSafe(draft == null ? null : draft.subcategory()));
        vars.put("draftDurationMinutes", nullSafe(draft == null ? null : draft.durationMinutes()));
        vars.put("draftMinAge", nullSafe(draft == null ? null : draft.minAge()));
        vars.put("draftPriceType", nullSafe(draft == null ? null : draft.priceType()));
        vars.put("draftIsUnlimitedCapacity", nullSafe(draft == null ? null : draft.isUnlimitedCapacity()));
        vars.put("draftMaxPeople", nullSafe(draft == null ? null : draft.maxPeople()));
        vars.put("draftCurrentDescription", nullSafe(draft == null ? null : draft.currentDescription()));
        vars.put("availableTags", tagsListing);

        String prompt = suggestContentPrompt.render(vars);

        Instant start = Instant.now();
        AgentLlmResponse response = llmClient.complete(prompt, DEFAULT_MODEL);
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        if (response.isError()) {
            log.warn("IA-07 suggest_tour_content LLM error: {}", response.error());
            writeAudit("suggest_tour_content", "tour",
                    request.getTourId() == null ? null : request.getTourId().longValue(),
                    auth, response, durationMs, "error", response.error(), true);
            return escalatedSuggestion();
        }

        TourContentSuggestion suggestion = parseSuggestionOrEscalate(response.content());

        writeAudit("suggest_tour_content", "tour",
                request.getTourId() == null ? null : request.getTourId().longValue(),
                auth, response, durationMs,
                suggestion.escalatedToHuman() ? "error" : "suggestion",
                suggestion.escalatedToHuman() ? "malformed_llm_json" : null,
                suggestion.escalatedToHuman());

        return suggestion;
    }

    // ============================================================
    // Endpoint 2 — price alert
    // ============================================================

    public PriceAlert priceAlert(Integer tourId, Authentication auth) {
        requireProviderRole(auth);
        Tour tour = requireTourOwnership(tourId, auth);

        if (!budgetGuard.canRun(AGENT_NAME)) {
            writeAudit("price_alert", "tour", tourId.longValue(), auth,
                    AgentLlmResponse.disabled(), 0L, "rejected", "budget_exhausted", true);
            return escalatedPriceAlert(null, null, 0);
        }

        // 1. Precio publico promedio del tour actual (ADULT).
        BigDecimal currentAvg = safeAvgPrice(tourId);

        // 2. Tours comparables (misma subcategoria, otros tours accepted).
        List<BigDecimal> comparablePrices = comparablesForTour(tour);
        int count = comparablePrices.size();

        if (count < 3) {
            String reason = "No hay tours comparables suficientes (encontrados: " + count + "). "
                    + "El agente necesita al menos 3 tours de la misma subcategoria para emitir una alerta confiable.";
            PriceAlert unknown = PriceAlert.builder()
                    .severity(PriceAlert.Severity.UNKNOWN)
                    .currentAvgPrice(currentAvg)
                    .comparablePriceRange(null)
                    .comparablesCount(count)
                    .reasoning(reason)
                    .escalatedToHuman(false)
                    .build();
            // Auditamos igual — sabemos que el agente respondio UNKNOWN sin llamar al LLM.
            writeAudit("price_alert", "tour", tourId.longValue(), auth,
                    AgentLlmResponse.disabled(), 0L, "suggestion", "insufficient_comparables", false);
            return unknown;
        }

        PriceAlert.PriceRange range = computeRange(comparablePrices);

        // 3. Llamar al LLM para reasoning + severity (con guardrails).
        Map<String, Object> vars = new HashMap<>();
        vars.put("tourId", tourId);
        vars.put("tourName", tour.getName() == null ? "" : nullSafe(tour.getName().getEs()));
        vars.put("tourSubcategory", tour.getSubCategory() == null ? "" : tour.getSubCategory().getValue());
        vars.put("tourCategoryId", tour.getTourCategory() == null ? "" : tour.getTourCategory().getId());
        vars.put("tourProviderPrice", currentAvg == null ? "" : currentAvg.toPlainString());
        vars.put("comparablesJson", pricesToJsonArray(comparablePrices));
        vars.put("compMin", range.min().toPlainString());
        vars.put("compMax", range.max().toPlainString());
        vars.put("compMedian", range.median().toPlainString());
        vars.put("compCount", count);

        String prompt = priceAlertPrompt.render(vars);

        Instant start = Instant.now();
        AgentLlmResponse response = llmClient.complete(prompt, DEFAULT_MODEL);
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        if (response.isError()) {
            log.warn("IA-07 price_alert LLM error: {}", response.error());
            writeAudit("price_alert", "tour", tourId.longValue(), auth,
                    response, durationMs, "error", response.error(), true);
            return escalatedPriceAlert(currentAvg, range, count);
        }

        PriceAlert alert = parsePriceAlertOrHeuristic(response.content(), currentAvg, range, count);

        writeAudit("price_alert", "tour", tourId.longValue(), auth,
                response, durationMs,
                alert.escalatedToHuman() ? "error" : "suggestion",
                alert.escalatedToHuman() ? "malformed_llm_json" : null,
                alert.escalatedToHuman());

        return alert;
    }

    // ============================================================
    // Endpoint 3 — draft review reply
    // ============================================================

    public DraftReviewReplyResponse draftReviewReply(Long reviewId, Authentication auth) {
        requireProviderRole(auth);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: id=" + reviewId));

        // Autorizacion — el review debe corresponder a un tour del provider del user.
        if (review.getTourId() == null) {
            throw new InsufficientPrivilegesException("La reseña no esta asociada a un tour valido.");
        }
        Tour tour = requireTourOwnership(review.getTourId(), auth);

        String reviewComment = review.getComment() == null ? "" : nullSafe(review.getComment().getEs());
        // Deny-list en el input del turista (el turista podria dejar leak intentado).
        if (containsSecret(reviewComment)) {
            log.warn("IA-07 secret keyword detected in review comment reviewId={}", reviewId);
            writeAudit("draft_review_reply", "review", reviewId, auth,
                    AgentLlmResponse.disabled(), 0L,
                    "rejected", "secret_keyword_in_review_comment", true);
            return escalatedDraftReply();
        }

        if (!budgetGuard.canRun(AGENT_NAME)) {
            writeAudit("draft_review_reply", "review", reviewId, auth,
                    AgentLlmResponse.disabled(), 0L,
                    "rejected", "budget_exhausted", true);
            return escalatedDraftReply();
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("reviewId", reviewId);
        vars.put("reviewRating", review.getRating() == null ? "" : review.getRating().toPlainString());
        vars.put("reviewComment", jsonEscape(reviewComment));
        vars.put("tourName", tour.getName() == null ? "" : nullSafe(tour.getName().getEs()));
        vars.put("tourSubcategory", tour.getSubCategory() == null ? "" : tour.getSubCategory().getValue());

        String prompt = draftReplyPrompt.render(vars);

        Instant start = Instant.now();
        AgentLlmResponse response = llmClient.complete(prompt, DEFAULT_MODEL);
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        if (response.isError()) {
            log.warn("IA-07 draft_review_reply LLM error: {}", response.error());
            writeAudit("draft_review_reply", "review", reviewId, auth,
                    response, durationMs, "error", response.error(), true);
            return escalatedDraftReply();
        }

        DraftReviewReplyResponse draft = parseDraftReplyOrEscalate(response.content());
        writeAudit("draft_review_reply", "review", reviewId, auth,
                response, durationMs,
                draft.escalatedToHuman() ? "error" : "suggestion",
                draft.escalatedToHuman() ? "malformed_llm_json" : null,
                draft.escalatedToHuman());

        return draft;
    }

    // ============================================================
    // Endpoint 4 — validate gallery (SIN LLM)
    // ============================================================

    /**
     * Valida metadata de imagenes usando las mismas reglas que
     * {@link GalleryValidator} — sin subir bytes al backend (cero costo LLM).
     */
    public ValidateGalleryResponse validateGallery(ValidateGalleryRequest request, Authentication auth) {
        requireProviderRole(auth);

        List<ValidateGalleryResponse.GalleryIssue> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        int maxImagesPerTour = appConfigService.getInt(ConfigKeyEnum.GALLERY_MAX_IMAGES_PER_TOUR, 7);
        long maxSizeMb = appConfigService.getInt(ConfigKeyEnum.GALLERY_MAX_SIZE_MB, 5);
        int minWidthPx = appConfigService.getInt(ConfigKeyEnum.GALLERY_MIN_WIDTH_PX, 800);
        long maxSizeBytes = maxSizeMb * 1024L * 1024L;

        List<ValidateGalleryRequest.ImageMetadata> images = request.getImages();

        if (images.size() > maxImagesPerTour) {
            issues.add(new ValidateGalleryResponse.GalleryIssue(
                    ValidateGalleryResponse.Severity.ERROR,
                    "TOO_MANY_IMAGES",
                    "Se enviaron " + images.size() + " imagenes; el maximo por tour es " + maxImagesPerTour + ".",
                    -1));
        }

        for (int i = 0; i < images.size(); i++) {
            ValidateGalleryRequest.ImageMetadata img = images.get(i);
            String filename = img.getFilename() == null ? "imagen_" + (i + 1) : img.getFilename();

            String format = img.getFormat() == null ? "" : img.getFormat().toLowerCase(Locale.ROOT);
            boolean formatOk = format.contains("jpeg") || format.contains("jpg")
                    || format.contains("png") || format.contains("webp");
            if (!formatOk) {
                issues.add(new ValidateGalleryResponse.GalleryIssue(
                        ValidateGalleryResponse.Severity.ERROR,
                        "INVALID_FORMAT",
                        filename + ": formato " + img.getFormat() + " no soportado (usa JPEG, PNG o WebP).",
                        i));
                continue;
            }

            if (img.getSizeBytes() != null && img.getSizeBytes() > maxSizeBytes) {
                long actualMb = Math.round(img.getSizeBytes() * 10.0 / (1024.0 * 1024.0)) / 10;
                issues.add(new ValidateGalleryResponse.GalleryIssue(
                        ValidateGalleryResponse.Severity.ERROR,
                        "TOO_LARGE",
                        filename + ": " + actualMb + " MB excede el maximo de " + maxSizeMb + " MB.",
                        i));
                continue;
            }

            Integer w = img.getWidthPx();
            Integer h = img.getHeightPx();
            if (w != null && h != null) {
                if (w < h) {
                    issues.add(new ValidateGalleryResponse.GalleryIssue(
                            ValidateGalleryResponse.Severity.ERROR,
                            "NOT_LANDSCAPE",
                            filename + ": orientacion vertical (" + w + "x" + h + "). Se requiere horizontal.",
                            i));
                    continue;
                }
                if (w < minWidthPx) {
                    issues.add(new ValidateGalleryResponse.GalleryIssue(
                            ValidateGalleryResponse.Severity.ERROR,
                            "WIDTH_TOO_SMALL",
                            filename + ": ancho " + w + "px es menor al minimo de " + minWidthPx + "px.",
                            i));
                    continue;
                }
                // Advisory: se recomienda 1920px de ancho como piso.
                if (w < 1920) {
                    issues.add(new ValidateGalleryResponse.GalleryIssue(
                            ValidateGalleryResponse.Severity.WARNING,
                            "WIDTH_BELOW_RECOMMENDED",
                            filename + ": ancho " + w + "px es aceptable pero se recomienda 1920px o mas.",
                            i));
                }
            }
        }

        if (images.size() < 3) {
            suggestions.add("Sube al menos 3 imagenes para que el tour se vea completo en la galeria.");
        }
        suggestions.add("Usa imagenes horizontales de alta resolucion (1920x1080 o mas).");

        boolean isValid = issues.stream()
                .noneMatch(i -> i.severity() == ValidateGalleryResponse.Severity.ERROR);

        // Audit sin costo LLM.
        writeAudit("validate_gallery", "tour", null, auth,
                AgentLlmResponse.disabled(), 0L,
                "suggestion", null, false);

        return ValidateGalleryResponse.builder()
                .isValid(isValid)
                .issues(issues)
                .suggestions(suggestions)
                .build();
    }

    // ============================================================
    // IA-09 — Traduccion es -> en / pt-BR (cerrado 2026-08-15)
    // ============================================================
    //
    // El agente sigue sugiriendo solo espanol (RN-011: el operador aprueba en
    // su idioma). La traduccion a en/pt-BR ocurre downstream cuando el tour se
    // guarda: TourService.saveCreateOrUpdateFullData -> TourTranslationEvent ->
    // TourTranslationEventListener -> GoogleCloudTranslationService. El
    // operador no tiene que aprobar traducciones por separado.
    //
    // Si el operador ya escribio en/pt manualmente (o edito lo que IA-07
    // sugirio), TourTranslationApplier respeta su input y no lo sobrescribe.

    // ============================================================
    // Autorizacion helpers
    // ============================================================

    private void requireProviderRole(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new InsufficientPrivilegesException("Autenticacion requerida.");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new InsufficientPrivilegesException("Autenticacion requerida.");
        }
        List<Role> roles = user.getRoles();
        if (!Utils.isProviderSide(roles)) {
            throw new InsufficientPrivilegesException(
                    "Se requiere rol PROVIDER o PROVIDER_OPERATOR.");
        }
    }

    /**
     * Verifica ownership tour → provider del user autenticado. Devuelve el
     * {@link Tour} cargado para reuso en el service. Lanza
     * {@link InsufficientPrivilegesException} si el tour no pertenece al
     * provider (semantica 403 via {@link com.tourya.api.handler.GlobalExceptionHandler}).
     */
    private Tour requireTourOwnership(Integer tourId, Authentication auth) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: id=" + tourId));
        if (tour.getProvider() == null) {
            throw new InsufficientPrivilegesException("El tour no tiene proveedor asignado.");
        }
        User user = (User) auth.getPrincipal();
        Provider provider;
        try {
            provider = providerService.findByUser(user);
        } catch (Exception ex) {
            log.debug("IA-07 provider lookup failed for userId={}: {}", user.getId(), ex.getMessage());
            provider = null;
        }
        if (provider == null || tour.getProvider().getId() == null
                || !tour.getProvider().getId().equals(provider.getId())) {
            throw new InsufficientPrivilegesException(
                    "No tienes permisos sobre el tour id=" + tourId + ".");
        }
        return tour;
    }

    // ============================================================
    // Guardrails
    // ============================================================

    boolean containsSecret(String input) {
        if (input == null) return false;
        String upper = input.toUpperCase(Locale.ROOT);
        for (String kw : SECRET_KEYWORDS) {
            if (upper.contains(kw)) return true;
        }
        return false;
    }

    /**
     * Elimina campos internos (precios provider, comisiones) de cualquier JsonNode
     * antes de mandarlo al LLM. Recursivo — objetos + arrays. Mismo patron que
     * IA-02.
     */
    JsonNode scrubSensitiveFields(JsonNode node) {
        if (node == null || node.isNull()) return node;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            for (String field : SCRUBBED_FIELDS) {
                if (obj.has(field)) obj.remove(field);
            }
            obj.fields().forEachRemaining(entry -> scrubSensitiveFields(entry.getValue()));
        } else if (node.isArray()) {
            node.forEach(this::scrubSensitiveFields);
        }
        return node;
    }

    // ============================================================
    // Parsing defensivo — LLM JSON
    // ============================================================

    TourContentSuggestion parseSuggestionOrEscalate(String content) {
        try {
            String json = stripFences(content);
            JsonNode root = objectMapper.readTree(json);
            List<String> names = readStringArray(root.path("nameSuggestions"));
            String description = root.path("descriptionSuggestion").asText("");
            List<String> tags = readStringArray(root.path("tagSuggestions"));
            String reasoning = root.path("reasoning").asText("");
            if (names.isEmpty() || description.isBlank()) {
                return escalatedSuggestion();
            }
            return TourContentSuggestion.builder()
                    .nameSuggestions(names)
                    .descriptionSuggestion(description)
                    .tagSuggestions(tags)
                    .reasoning(reasoning)
                    .escalatedToHuman(false)
                    .build();
        } catch (Exception ex) {
            log.warn("IA-07 malformed suggest_tour_content JSON: {}", ex.getMessage());
            return escalatedSuggestion();
        }
    }

    PriceAlert parsePriceAlertOrHeuristic(String content, BigDecimal currentAvg,
                                          PriceAlert.PriceRange range, int count) {
        // Heuristica fallback: si el LLM no responde JSON valido, computamos
        // severity aca. La FE recibe reasoning generico.
        try {
            String json = stripFences(content);
            JsonNode root = objectMapper.readTree(json);
            String sevRaw = root.path("severity").asText("");
            String reasoning = root.path("reasoning").asText("");
            PriceAlert.Severity severity = parseSeverity(sevRaw, currentAvg, range);
            if (reasoning.isBlank()) {
                reasoning = defaultPricingReasoning(severity, currentAvg, range);
            }
            return PriceAlert.builder()
                    .severity(severity)
                    .currentAvgPrice(currentAvg)
                    .comparablePriceRange(range)
                    .comparablesCount(count)
                    .reasoning(reasoning)
                    .escalatedToHuman(false)
                    .build();
        } catch (Exception ex) {
            log.warn("IA-07 malformed price_alert JSON — usando heuristica: {}", ex.getMessage());
            PriceAlert.Severity severity = heuristicSeverity(currentAvg, range);
            return PriceAlert.builder()
                    .severity(severity)
                    .currentAvgPrice(currentAvg)
                    .comparablePriceRange(range)
                    .comparablesCount(count)
                    .reasoning(defaultPricingReasoning(severity, currentAvg, range))
                    .escalatedToHuman(true)
                    .build();
        }
    }

    DraftReviewReplyResponse parseDraftReplyOrEscalate(String content) {
        try {
            String json = stripFences(content);
            JsonNode root = objectMapper.readTree(json);
            String draft = root.path("draftText").asText("");
            String locale = root.path("detectedLocale").asText("es");
            String toneRaw = root.path("tone").asText("PROFESSIONAL");
            String reasoning = root.path("reasoning").asText("");
            if (draft.isBlank()) {
                return escalatedDraftReply();
            }
            DraftReviewReplyResponse.Tone tone;
            try {
                tone = DraftReviewReplyResponse.Tone.valueOf(toneRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                tone = DraftReviewReplyResponse.Tone.PROFESSIONAL;
            }
            return DraftReviewReplyResponse.builder()
                    .draftText(draft)
                    .detectedLocale(locale.toLowerCase(Locale.ROOT))
                    .tone(tone)
                    .reasoning(reasoning)
                    .escalatedToHuman(false)
                    .build();
        } catch (Exception ex) {
            log.warn("IA-07 malformed draft_review_reply JSON: {}", ex.getMessage());
            return escalatedDraftReply();
        }
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

    private static List<String> readStringArray(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(el -> {
                if (el != null && el.isTextual()) out.add(el.asText());
            });
        }
        return out;
    }

    // ============================================================
    // Pricing helpers
    // ============================================================

    private BigDecimal safeAvgPrice(Integer tourId) {
        try {
            return operatorSupportRepository.findAvgAdultPriceByTourId(tourId);
        } catch (Exception ex) {
            log.debug("IA-07 avg price lookup failed tourId={}: {}", tourId, ex.getMessage());
            return null;
        }
    }

    private List<BigDecimal> comparablesForTour(Tour tour) {
        if (tour.getSubCategory() == null) {
            return List.of();
        }
        try {
            return operatorSupportRepository.findComparableAdultPricesBySubcategory(
                    tour.getSubCategory().getValue(), tour.getId(), COMPARABLES_MAX);
        } catch (Exception ex) {
            log.warn("IA-07 comparables lookup failed tourId={}: {}", tour.getId(), ex.getMessage());
            return List.of();
        }
    }

    static PriceAlert.PriceRange computeRange(List<BigDecimal> pricesAsc) {
        if (pricesAsc == null || pricesAsc.isEmpty()) return null;
        List<BigDecimal> sorted = new ArrayList<>(pricesAsc);
        sorted.sort(Comparator.naturalOrder());
        BigDecimal min = sorted.get(0);
        BigDecimal max = sorted.get(sorted.size() - 1);
        BigDecimal median;
        int n = sorted.size();
        if (n % 2 == 1) {
            median = sorted.get(n / 2);
        } else {
            median = sorted.get(n / 2 - 1)
                    .add(sorted.get(n / 2))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
        return new PriceAlert.PriceRange(min, max, median);
    }

    private PriceAlert.Severity parseSeverity(String raw, BigDecimal currentAvg,
                                              PriceAlert.PriceRange range) {
        if (raw == null) return heuristicSeverity(currentAvg, range);
        String up = raw.trim().toUpperCase(Locale.ROOT);
        return switch (up) {
            case "OK" -> PriceAlert.Severity.OK;
            case "WARN" -> PriceAlert.Severity.WARN;
            case "CRITICAL" -> PriceAlert.Severity.CRITICAL;
            case "UNKNOWN" -> PriceAlert.Severity.UNKNOWN;
            default -> heuristicSeverity(currentAvg, range);
        };
    }

    static PriceAlert.Severity heuristicSeverity(BigDecimal currentAvg, PriceAlert.PriceRange range) {
        if (currentAvg == null || range == null || range.median() == null
                || range.median().compareTo(BigDecimal.ZERO) == 0) {
            return PriceAlert.Severity.UNKNOWN;
        }
        BigDecimal diff = currentAvg.subtract(range.median()).abs();
        BigDecimal pct = diff.divide(range.median(), 4, RoundingMode.HALF_UP);
        double p = pct.doubleValue();
        if (p <= 0.15) return PriceAlert.Severity.OK;
        if (p <= 0.35) return PriceAlert.Severity.WARN;
        return PriceAlert.Severity.CRITICAL;
    }

    private static String defaultPricingReasoning(PriceAlert.Severity sev, BigDecimal currentAvg,
                                                  PriceAlert.PriceRange range) {
        if (sev == PriceAlert.Severity.UNKNOWN || range == null || currentAvg == null) {
            return "No hay comparables suficientes para determinar si el precio esta alineado.";
        }
        String direction = currentAvg.compareTo(range.median()) > 0 ? "por encima" : "por debajo";
        return "El precio esta " + direction + " de la mediana del mercado (mediana="
                + range.median().toPlainString() + ", tu precio=" + currentAvg.toPlainString() + ").";
    }

    private static String pricesToJsonArray(List<BigDecimal> prices) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < prices.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(prices.get(i).toPlainString());
        }
        sb.append("]");
        return sb.toString();
    }

    // ============================================================
    // Tag catalog rendering
    // ============================================================

    private String renderTagsCatalog() {
        try {
            List<TourTagResponse> tags = tourTagService.getAllTags();
            if (tags == null || tags.isEmpty()) {
                return "(catalogo vacio — usa tags genericos alineados al subcategory)";
            }
            StringBuilder sb = new StringBuilder();
            for (TourTagResponse t : tags) {
                sb.append("- ").append(t.getSlug()).append(" (")
                        .append(t.getName() == null ? "" : nullSafe(t.getName().getEs()))
                        .append(")\n");
            }
            return sb.toString();
        } catch (Exception ex) {
            log.debug("IA-07 tag catalog load failed: {}", ex.getMessage());
            return "(catalogo no disponible)";
        }
    }

    // ============================================================
    // Escalation fallbacks
    // ============================================================

    private static TourContentSuggestion escalatedSuggestion() {
        return TourContentSuggestion.builder()
                .nameSuggestions(List.of())
                .descriptionSuggestion("")
                .tagSuggestions(List.of())
                .reasoning("El agente no pudo generar sugerencias en este momento. "
                        + "Intenta nuevamente o edita el tour manualmente.")
                .escalatedToHuman(true)
                .build();
    }

    private static PriceAlert escalatedPriceAlert(BigDecimal currentAvg, PriceAlert.PriceRange range, int count) {
        return PriceAlert.builder()
                .severity(PriceAlert.Severity.UNKNOWN)
                .currentAvgPrice(currentAvg)
                .comparablePriceRange(range)
                .comparablesCount(count)
                .reasoning("No fue posible analizar el precio en este momento. "
                        + "Puedes revisar tours similares manualmente.")
                .escalatedToHuman(true)
                .build();
    }

    private static DraftReviewReplyResponse escalatedDraftReply() {
        return DraftReviewReplyResponse.builder()
                .draftText("")
                .detectedLocale("es")
                .tone(DraftReviewReplyResponse.Tone.PROFESSIONAL)
                .reasoning("El agente no pudo generar el borrador. Responde manualmente al turista.")
                .escalatedToHuman(true)
                .build();
    }

    // ============================================================
    // Audit
    // ============================================================

    private void writeAudit(String capability, String entityType, Long entityId,
                            Authentication auth, AgentLlmResponse response, long durationMs,
                            String resultType, String errorMessage, boolean escalated) {
        try {
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("capability", capability);
            metadata.put("escalated_to_human", escalated);

            Integer userId = extractUserId(auth);

            AgentAuditEntry entry = AgentAuditEntry.builder()
                    .agentName(AGENT_NAME)
                    .model(response.model() == null ? DEFAULT_MODEL : response.model())
                    .promptVersion(PROMPT_VERSION)
                    .inputTokens(response.inputTokens())
                    .outputTokens(response.outputTokens())
                    .costUsd(response.costUsd())
                    .durationMs(durationMs)
                    .entityType(entityType)
                    .entityId(entityId)
                    .userId(userId)
                    .resultType(resultType)
                    .resultJson(null)
                    .promptInput(null)
                    .errorMessage(errorMessage)
                    .metadata(objectMapper.writeValueAsString(metadata))
                    .build();
            auditWriter.register(entry);
        } catch (Exception ex) {
            log.error("IA-07 audit write failed capability={}", capability, ex);
        }
    }

    private static Integer extractUserId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof User u) return u.getId();
        return null;
    }

    // ============================================================
    // Misc helpers
    // ============================================================

    private static String nullSafe(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }

    /** Test hook — expone {@link #computeRange} publicamente para tests. */
    public static PriceAlert.PriceRange computeRangeForTest(List<BigDecimal> pricesAsc) {
        return computeRange(pricesAsc);
    }

    /** Test hook — expone {@link #heuristicSeverity} publicamente para tests. */
    public static PriceAlert.Severity heuristicSeverityForTest(BigDecimal currentAvg, PriceAlert.PriceRange range) {
        return heuristicSeverity(currentAvg, range);
    }

    /** Test hook — muestra {@link Optional} usage helper para consumers. */
    Optional<Tour> loadTourById(Integer tourId) {
        return tourRepository.findById(tourId);
    }
}
