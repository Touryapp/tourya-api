package com.tourya.api.agents.backoffice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tourya.api._utils.Utils;
import com.tourya.api.agents.shared.AgentAuditEntry;
import com.tourya.api.agents.shared.AgentAuditWriter;
import com.tourya.api.agents.shared.AgentLlmResponse;
import com.tourya.api.agents.shared.BudgetGuard;
import com.tourya.api.agents.shared.ILlmClient;
import com.tourya.api.agents.shared.ModelPricing;
import com.tourya.api.agents.shared.PromptTemplate;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.AccountPayable;
import com.tourya.api.models.Payment;
import com.tourya.api.models.ProviderPayoutOrder;
import com.tourya.api.models.ProviderPayoutOrderReservation;
import com.tourya.api.models.RequestProvider;
import com.tourya.api.models.RequestProviderDocumentType;
import com.tourya.api.models.RequestProviderGallery;
import com.tourya.api.models.Reservation;
import com.tourya.api.models.ShoppingCartItem;
import com.tourya.api.models.ShoppingCartItemDetail;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourCancellationPolicy;
import com.tourya.api.models.TourGallery;
import com.tourya.api.models.TranslatedField;
import com.tourya.api.models.User;
import com.tourya.api.repository.AccountPayableRepository;
import com.tourya.api.repository.PaymentRepository;
import com.tourya.api.repository.ProviderPayoutOrderRepository;
import com.tourya.api.repository.ProviderPayoutOrderReservationRepository;
import com.tourya.api.repository.RequestProviderDocumentTypeRepository;
import com.tourya.api.repository.RequestProviderGalleryRepository;
import com.tourya.api.repository.RequestProviderRepository;
import com.tourya.api.repository.ReservationRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import com.tourya.api.repository.TourCancellationPolicyRepository;
import com.tourya.api.repository.TourGalleryRepository;
import com.tourya.api.repository.TourRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * IA-08: Agente 5 del doc 16 — Backoffice Support.
 *
 * <p>Responsabilidades MVP (ver doc 16 §Agente 5):</p>
 * <ol>
 *   <li>Checklist KYB pre-verificado del {@link RequestProvider} — el ADMIN
 *       decide {@code pre-approve/approve} despues ({@link #kybChecklist}).</li>
 *   <li>Pre-validacion de tour: RN-011 (espanol), RN-013 (galeria) y politica
 *       de cancelacion definida ({@link #tourPrevalidation}).</li>
 *   <li>Borrador de manifiesto DIMAR (RN-054) para un provider + fecha
 *       ({@link #dimarDraft}, sin LLM).</li>
 *   <li>Anomalias en {@link ProviderPayoutOrder} vs {@link AccountPayable}
 *       (RN-042, {@link #payoutAnomalies}).</li>
 * </ol>
 *
 * <p><b>Guardrails no negociables (implementados en codigo, no solo en el prompt):</b></p>
 * <ul>
 *   <li>Rol ADMIN o BACKOFFICE_OPERATION — mismo guard que IA-11
 *       ({@code Utils.isTouryaBackoffice}).</li>
 *   <li>Deny-list de secretos identica a IA-02/IA-07 en cualquier input textual —
 *       la request se rechaza y se audita como {@code rejected}.</li>
 *   <li>Scrub de {@code providerPrice}/{@code slotPercentageTourya}/{@code porcentajeTourya}
 *       en todo JSON que se pase al LLM.</li>
 *   <li>Budget guard: si {@link BudgetGuard#canRun} devuelve {@code false} el
 *       agente igual devuelve la parte deterministica cuando existe (checklist
 *       KYB por presencia, issues deterministicos del tour, filas DIMAR, listado
 *       de anomalias) — solo se pierde el {@code reasoning}/explanation del LLM.</li>
 * </ul>
 *
 * <p><b>El agente NUNCA aprueba/rechaza/modifica</b>: RN-010 y RN-046 reservan
 * la aprobacion KYB/tour a ADMIN; RN-042 blinda el {@code amount} del payout.
 * El agente 5 solo asiste — el humano decide en cada uno de los 4 flujos.</p>
 */
@Slf4j
@Service
public class BackofficeSupportService {

    private static final String AGENT_NAME = "BackofficeSupport";
    private static final String PROMPT_VERSION = "v1";
    private static final String DEFAULT_MODEL = ModelPricing.GEMINI_2_5_PRO;

    /** Deny-list identica a IA-02/IA-07 (TODO IA-11: extraer a GuardrailUtils). */
    private static final String[] SECRET_KEYWORDS = {
            "WOMPI_INTEGRITY_SECRET",
            "WOMPI_EVENTS_SECRET",
            "JWT_SECRET",
            "ANTHROPIC_API_KEY",
            "FIREBASE_ADMIN_SDK_JSON",
            "GEMINI_API_KEY"
    };

    /** Campos internos que NUNCA deben llegar al LLM (defense-in-depth). */
    private static final String[] SCRUBBED_FIELDS = {
            "slotPercentageTourya", "slotPorcentajeTourya",
            "providerPrice", "porcentajeTourya", "providerUnitPrice"
    };

    private final ILlmClient llmClient;
    private final AgentAuditWriter auditWriter;
    private final BudgetGuard budgetGuard;
    private final RequestProviderRepository requestProviderRepository;
    private final RequestProviderGalleryRepository requestProviderGalleryRepository;
    private final RequestProviderDocumentTypeRepository documentTypeRepository;
    private final TourRepository tourRepository;
    private final TourGalleryRepository tourGalleryRepository;
    private final TourCancellationPolicyRepository tourCancellationPolicyRepository;
    private final ReservationRepository reservationRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProviderPayoutOrderRepository payoutOrderRepository;
    private final ProviderPayoutOrderReservationRepository payoutOrderReservationRepository;
    private final AccountPayableRepository accountPayableRepository;
    private final ObjectMapper objectMapper;

    private final PromptTemplate kybPrompt;
    private final PromptTemplate tourPrevalidationPrompt;
    private final PromptTemplate payoutAnomalyPrompt;

    public BackofficeSupportService(
            ILlmClient llmClient,
            AgentAuditWriter auditWriter,
            BudgetGuard budgetGuard,
            RequestProviderRepository requestProviderRepository,
            RequestProviderGalleryRepository requestProviderGalleryRepository,
            RequestProviderDocumentTypeRepository documentTypeRepository,
            TourRepository tourRepository,
            TourGalleryRepository tourGalleryRepository,
            TourCancellationPolicyRepository tourCancellationPolicyRepository,
            ReservationRepository reservationRepository,
            ShoppingCartItemRepository shoppingCartItemRepository,
            PaymentRepository paymentRepository,
            ProviderPayoutOrderRepository payoutOrderRepository,
            ProviderPayoutOrderReservationRepository payoutOrderReservationRepository,
            AccountPayableRepository accountPayableRepository,
            ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.auditWriter = auditWriter;
        this.budgetGuard = budgetGuard;
        this.requestProviderRepository = requestProviderRepository;
        this.requestProviderGalleryRepository = requestProviderGalleryRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.tourRepository = tourRepository;
        this.tourGalleryRepository = tourGalleryRepository;
        this.tourCancellationPolicyRepository = tourCancellationPolicyRepository;
        this.reservationRepository = reservationRepository;
        this.shoppingCartItemRepository = shoppingCartItemRepository;
        this.paymentRepository = paymentRepository;
        this.payoutOrderRepository = payoutOrderRepository;
        this.payoutOrderReservationRepository = payoutOrderReservationRepository;
        this.accountPayableRepository = accountPayableRepository;
        this.objectMapper = objectMapper;
        this.kybPrompt = PromptTemplate.load("backoffice-support/kyb-checklist.v1");
        this.tourPrevalidationPrompt = PromptTemplate.load("backoffice-support/tour-prevalidation.v1");
        this.payoutAnomalyPrompt = PromptTemplate.load("backoffice-support/payout-anomaly.v1");
    }

    // ============================================================
    // Endpoint 1 — KYB checklist
    // ============================================================

    public KybChecklistResponse kybChecklist(Integer requestProviderId, Authentication auth) {
        requireBackofficeRole(auth);

        RequestProvider request = requestProviderRepository.findById(requestProviderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RequestProvider not found: id=" + requestProviderId));

        List<RequestProviderDocumentType> catalog = safeMandatoryDocs();
        List<RequestProviderGallery> uploads = safeUploads(requestProviderId);

        // Guardrail: input del operador podria intentar leak (descripcion libre).
        for (RequestProviderGallery upload : uploads) {
            if (containsSecret(upload.getDescription())) {
                log.warn("IA-08 secret keyword detected in KYB upload requestProviderId={}", requestProviderId);
                writeAudit("kyb_checklist", "request_provider", requestProviderId.longValue(),
                        auth, AgentLlmResponse.disabled(), 0L,
                        "rejected", "secret_keyword_in_upload_description", true);
                return escalatedKyb(requestProviderId, catalog);
            }
        }

        // 1) Deterministic checklist — no depende del LLM.
        List<KybChecklistItem> deterministic = buildDeterministicChecklist(catalog, uploads);
        String overallDeterministic = computeOverallStatus(deterministic);

        // 2) LLM opcional: reasoning + posibles refinamientos del status.
        if (!budgetGuard.canRun(AGENT_NAME)) {
            writeAudit("kyb_checklist", "request_provider", requestProviderId.longValue(),
                    auth, AgentLlmResponse.disabled(), 0L,
                    "suggestion", "budget_exhausted", false);
            return KybChecklistResponse.builder()
                    .requestProviderId(requestProviderId)
                    .overallStatus(overallDeterministic)
                    .items(deterministic)
                    .reasoning("Presupuesto mensual del agente agotado — se devuelve el checklist deterministico.")
                    .escalatedToHuman(false)
                    .build();
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("requestProviderId", requestProviderId);
        vars.put("requestProviderStatus", request.getStatus() == null ? "" : request.getStatus().name());
        vars.put("mandatoryCatalog", renderCatalog(catalog));
        vars.put("uploadedDocs", renderUploads(uploads));

        String prompt = kybPrompt.render(vars);

        Instant start = Instant.now();
        AgentLlmResponse response = llmClient.complete(prompt, DEFAULT_MODEL);
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        if (response.isError()) {
            writeAudit("kyb_checklist", "request_provider", requestProviderId.longValue(),
                    auth, response, durationMs, "error", response.error(), true);
            return KybChecklistResponse.builder()
                    .requestProviderId(requestProviderId)
                    .overallStatus(overallDeterministic)
                    .items(deterministic)
                    .reasoning("El agente no pudo generar el resumen; usa el checklist deterministico.")
                    .escalatedToHuman(true)
                    .build();
        }

        KybChecklistResponse merged = mergeKybLlmResponse(
                requestProviderId, deterministic, overallDeterministic, response.content());

        writeAudit("kyb_checklist", "request_provider", requestProviderId.longValue(),
                auth, response, durationMs,
                merged.escalatedToHuman() ? "error" : "suggestion",
                merged.escalatedToHuman() ? "malformed_llm_json" : null,
                merged.escalatedToHuman());
        return merged;
    }

    // ============================================================
    // Endpoint 2 — Tour pre-validation
    // ============================================================

    public TourPrevalidationResponse tourPrevalidation(Integer tourId, Authentication auth) {
        requireBackofficeRole(auth);

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: id=" + tourId));

        List<TourIssueItem> issues = buildDeterministicTourIssues(tour);
        boolean canApprove = issues.stream()
                .noneMatch(i -> TourIssueItem.SEVERITY_CRITICAL.equals(i.severity()));

        // Guardrail — el nombre del tour podria contener secretos.
        String tourNameEs = tourEs(tour.getName());
        if (containsSecret(tourNameEs) || containsSecret(tourEs(tour.getDescription()))) {
            log.warn("IA-08 secret keyword detected in tour prevalidation tourId={}", tourId);
            writeAudit("tour_prevalidation", "tour", tourId.longValue(),
                    auth, AgentLlmResponse.disabled(), 0L,
                    "rejected", "secret_keyword_in_tour_content", true);
            return TourPrevalidationResponse.builder()
                    .tourId(tourId)
                    .canApprove(false)
                    .issues(issues)
                    .reasoning("Se detecto una palabra clave sensible en el contenido del tour; " +
                            "escalar a humano.")
                    .escalatedToHuman(true)
                    .build();
        }

        if (!budgetGuard.canRun(AGENT_NAME)) {
            writeAudit("tour_prevalidation", "tour", tourId.longValue(),
                    auth, AgentLlmResponse.disabled(), 0L,
                    "suggestion", "budget_exhausted", false);
            return TourPrevalidationResponse.builder()
                    .tourId(tourId)
                    .canApprove(canApprove)
                    .issues(issues)
                    .reasoning(defaultTourReasoning(issues))
                    .escalatedToHuman(false)
                    .build();
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("tourId", tourId);
        vars.put("tourName", tourNameEs);
        vars.put("issuesJson", renderIssuesJson(issues));

        String prompt = tourPrevalidationPrompt.render(vars);
        Instant start = Instant.now();
        AgentLlmResponse response = llmClient.complete(prompt, DEFAULT_MODEL);
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        String reasoning;
        boolean escalated;
        if (response.isError()) {
            reasoning = defaultTourReasoning(issues);
            escalated = true;
        } else {
            String parsed = parseReasoning(response.content());
            if (parsed == null || parsed.isBlank()) {
                reasoning = defaultTourReasoning(issues);
                escalated = true;
            } else {
                reasoning = parsed;
                escalated = false;
            }
        }

        writeAudit("tour_prevalidation", "tour", tourId.longValue(),
                auth, response, durationMs,
                escalated && response.isError() ? "error" : "suggestion",
                escalated && response.isError() ? response.error() : null,
                escalated);
        return TourPrevalidationResponse.builder()
                .tourId(tourId)
                .canApprove(canApprove)
                .issues(issues)
                .reasoning(reasoning)
                .escalatedToHuman(escalated)
                .build();
    }

    // ============================================================
    // Endpoint 3 — DIMAR draft (sin LLM)
    // ============================================================

    public DimarDraftResponse dimarDraft(LocalDate date, Integer providerId, Authentication auth) {
        requireBackofficeRole(auth);

        if (date == null || providerId == null) {
            throw new IllegalArgumentException("date y providerId son obligatorios.");
        }

        List<Reservation> reservations = reservationRepository
                .findConfirmedForProviderOnDate(providerId, date);

        List<DimarPassengerRow> rows = new ArrayList<>();
        for (Reservation r : reservations) {
            rows.addAll(buildPassengerRows(r));
        }
        int totalPassengers = rows.stream()
                .mapToInt(row -> row.quantity() == null ? 0 : row.quantity())
                .sum();

        String notes = rows.isEmpty()
                ? "No hay reservas CONFIRMED/DELIVERED para " + date + " (provider=" + providerId
                    + "). No hay manifiesto que subir."
                : "Basado en " + reservations.size() + " reservas CONFIRMED/DELIVERED del "
                    + date + ". Revisa el listado antes de subirlo a DIMAR (RN-054).";

        // Audit sin costo LLM.
        writeAudit("dimar_draft", "provider", providerId.longValue(),
                auth, AgentLlmResponse.disabled(), 0L,
                "suggestion", null, false);

        return DimarDraftResponse.builder()
                .date(date)
                .providerId(providerId)
                .totalPassengers(totalPassengers)
                .passengers(rows)
                .notes(notes)
                .build();
    }

    // ============================================================
    // Endpoint 4 — Payout anomalies
    // ============================================================

    public List<PayoutAnomalyResponse> payoutAnomalies(LocalDate from, LocalDate to, Authentication auth) {
        requireBackofficeRole(auth);
        if (from == null || to == null) {
            throw new IllegalArgumentException("from y to son obligatorios.");
        }
        if (from.isAfter(to)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }

        OffsetDateTime fromTs = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);

        List<ProviderPayoutOrder> orders = payoutOrderRepository
                .findFiltered(null, null, fromTs, toTs);

        List<PayoutAnomalyResponse> out = new ArrayList<>();
        boolean canUseLlm = budgetGuard.canRun(AGENT_NAME);

        for (ProviderPayoutOrder order : orders) {
            List<ProviderPayoutOrderReservation> lines = payoutOrderReservationRepository
                    .findByPayoutOrderId(order.getId());
            List<PayoutAnomalyItem> anomalies = buildPayoutAnomalyItems(order, lines);
            if (anomalies.isEmpty()) continue;

            String severity = anomalies.stream()
                    .anyMatch(a -> PayoutAnomalyItem.CODE_TOTAL_DRIFT.equals(a.code())
                            || PayoutAnomalyItem.CODE_MISSING_ACCOUNT_PAYABLE.equals(a.code()))
                    ? PayoutAnomalyResponse.SEVERITY_CRITICAL
                    : PayoutAnomalyResponse.SEVERITY_WARN;

            String explanation = defaultAnomalyExplanation(anomalies);
            long durationMs = 0L;
            AgentLlmResponse response = AgentLlmResponse.disabled();

            if (canUseLlm) {
                Map<String, Object> vars = new HashMap<>();
                vars.put("payoutOrderId", order.getId());
                vars.put("providerId", order.getProviderId());
                vars.put("payDate", order.getPayDate());
                vars.put("amountTotalOrder", order.getAmountTotal() == null
                        ? "" : order.getAmountTotal().toPlainString());
                vars.put("itemsJson", renderAnomalyItemsJson(anomalies));

                String prompt = payoutAnomalyPrompt.render(vars);
                Instant start = Instant.now();
                response = llmClient.complete(prompt, DEFAULT_MODEL);
                durationMs = Duration.between(start, Instant.now()).toMillis();

                if (!response.isError()) {
                    String parsed = parseExplanation(response.content());
                    if (parsed != null && !parsed.isBlank()) {
                        explanation = parsed;
                    }
                }
            }

            writeAudit("payout_anomalies", "provider_payout_order", order.getId(),
                    auth, response, durationMs,
                    response.isError() ? "error" : "suggestion",
                    response.isError() ? response.error() : null,
                    response.isError());

            out.add(PayoutAnomalyResponse.builder()
                    .payoutOrderId(order.getId())
                    .providerId(order.getProviderId())
                    .payDate(order.getPayDate())
                    .amountTotalOrder(order.getAmountTotal())
                    .items(anomalies)
                    .explanation(explanation)
                    .severity(severity)
                    .build());
        }
        return out;
    }

    // ============================================================
    // Authorization
    // ============================================================

    private void requireBackofficeRole(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new InsufficientPrivilegesException("Se requiere rol ADMIN o BACKOFFICE_OPERATION.");
        }
        if (!Utils.isTouryaBackoffice(user.getRoles())) {
            throw new InsufficientPrivilegesException("Se requiere rol ADMIN o BACKOFFICE_OPERATION.");
        }
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
     * Elimina campos internos (precios provider, comisiones) recursivamente
     * del JsonNode antes de mandarlo al LLM. Mismo patron que IA-02/IA-07.
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
    // KYB helpers
    // ============================================================

    private List<RequestProviderDocumentType> safeMandatoryDocs() {
        try {
            List<RequestProviderDocumentType> all = documentTypeRepository
                    .getAllRequestProviderDocumentTypeList();
            if (all == null) return List.of();
            return all.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getMandatory()))
                    .toList();
        } catch (Exception ex) {
            log.warn("IA-08 mandatory doc catalog lookup failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<RequestProviderGallery> safeUploads(Integer requestProviderId) {
        try {
            List<RequestProviderGallery> list = requestProviderGalleryRepository
                    .findByRequestProviderId(requestProviderId);
            return list == null ? List.of() : list;
        } catch (Exception ex) {
            log.warn("IA-08 gallery lookup failed requestProviderId={}: {}",
                    requestProviderId, ex.getMessage());
            return List.of();
        }
    }

    List<KybChecklistItem> buildDeterministicChecklist(
            List<RequestProviderDocumentType> catalog,
            List<RequestProviderGallery> uploads) {
        List<KybChecklistItem> out = new ArrayList<>();
        for (RequestProviderDocumentType doc : catalog) {
            String name = doc.getName() == null ? "UNKNOWN" : doc.getName();
            List<RequestProviderGallery> matches = uploads.stream()
                    .filter(u -> u.getDocumentType() != null
                            && doc.getId() != null
                            && doc.getId().equals(u.getDocumentType().getId()))
                    .toList();
            boolean present = !matches.isEmpty();
            List<String> issues = new ArrayList<>();
            String status;
            if (!present) {
                issues.add("documento faltante");
                status = KybChecklistItem.STATUS_CRITICAL;
            } else {
                if (matches.size() > 1) {
                    issues.add("se subieron " + matches.size() + " imagenes; se espera 1 documento");
                }
                boolean allBlank = matches.stream()
                        .allMatch(m -> m.getDescription() == null || m.getDescription().isBlank());
                if (allBlank) {
                    issues.add("descripcion vacia — sin nota del operador");
                }
                status = issues.isEmpty() ? KybChecklistItem.STATUS_OK : KybChecklistItem.STATUS_WARN;
            }
            out.add(KybChecklistItem.builder()
                    .documentType(name)
                    .present(present)
                    .issues(issues)
                    .status(status)
                    .build());
        }
        return out;
    }

    static String computeOverallStatus(List<KybChecklistItem> items) {
        if (items.isEmpty()) return KybChecklistResponse.STATUS_INCOMPLETE;
        long criticals = items.stream()
                .filter(i -> KybChecklistItem.STATUS_CRITICAL.equals(i.status()))
                .count();
        if (criticals >= 3) return KybChecklistResponse.STATUS_REJECTED;
        boolean allOk = items.stream()
                .allMatch(i -> KybChecklistItem.STATUS_OK.equals(i.status()));
        return allOk ? KybChecklistResponse.STATUS_COMPLETE : KybChecklistResponse.STATUS_INCOMPLETE;
    }

    KybChecklistResponse mergeKybLlmResponse(Integer requestProviderId,
                                             List<KybChecklistItem> deterministic,
                                             String deterministicOverall,
                                             String llmContent) {
        try {
            String json = stripFences(llmContent);
            JsonNode root = objectMapper.readTree(json);
            String llmOverall = root.path("overallStatus").asText("");
            String reasoning = root.path("reasoning").asText("");
            String overall = normalizeOverall(llmOverall, deterministicOverall);
            String finalReasoning = reasoning.isBlank()
                    ? defaultKybReasoning(deterministic)
                    : reasoning;
            return KybChecklistResponse.builder()
                    .requestProviderId(requestProviderId)
                    .overallStatus(overall)
                    .items(deterministic)
                    .reasoning(finalReasoning)
                    .escalatedToHuman(false)
                    .build();
        } catch (Exception ex) {
            log.warn("IA-08 malformed kyb_checklist JSON: {}", ex.getMessage());
            return KybChecklistResponse.builder()
                    .requestProviderId(requestProviderId)
                    .overallStatus(deterministicOverall)
                    .items(deterministic)
                    .reasoning(defaultKybReasoning(deterministic))
                    .escalatedToHuman(true)
                    .build();
        }
    }

    private static String normalizeOverall(String raw, String fallback) {
        if (raw == null) return fallback;
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "COMPLETE" -> KybChecklistResponse.STATUS_COMPLETE;
            case "INCOMPLETE" -> KybChecklistResponse.STATUS_INCOMPLETE;
            case "REJECTED" -> KybChecklistResponse.STATUS_REJECTED;
            default -> fallback;
        };
    }

    private static String defaultKybReasoning(List<KybChecklistItem> items) {
        long missing = items.stream()
                .filter(i -> KybChecklistItem.STATUS_CRITICAL.equals(i.status()))
                .count();
        if (missing == 0) {
            return "Todos los documentos obligatorios estan presentes; revisa contenido antes de aprobar.";
        }
        return "Faltan " + missing + " documento(s) obligatorio(s). Solicita al operador reenviar antes de decidir.";
    }

    private KybChecklistResponse escalatedKyb(Integer requestProviderId,
                                              List<RequestProviderDocumentType> catalog) {
        List<KybChecklistItem> stub = catalog.stream()
                .map(d -> KybChecklistItem.builder()
                        .documentType(d.getName() == null ? "UNKNOWN" : d.getName())
                        .present(false)
                        .issues(List.of("revision manual requerida"))
                        .status(KybChecklistItem.STATUS_WARN)
                        .build())
                .toList();
        return KybChecklistResponse.builder()
                .requestProviderId(requestProviderId)
                .overallStatus(KybChecklistResponse.STATUS_INCOMPLETE)
                .items(stub)
                .reasoning("El agente rechazo el input por politica de seguridad. Revisa manualmente.")
                .escalatedToHuman(true)
                .build();
    }

    private String renderCatalog(List<RequestProviderDocumentType> catalog) {
        if (catalog.isEmpty()) return "(catalogo vacio)";
        StringBuilder sb = new StringBuilder();
        for (RequestProviderDocumentType d : catalog) {
            sb.append("- ").append(d.getName()).append("\n");
        }
        return sb.toString();
    }

    private String renderUploads(List<RequestProviderGallery> uploads) {
        if (uploads.isEmpty()) return "(sin uploads)";
        StringBuilder sb = new StringBuilder();
        for (RequestProviderGallery u : uploads) {
            String type = u.getDocumentType() == null ? "UNKNOWN" : u.getDocumentType().getName();
            String desc = u.getDescription() == null ? "" : u.getDescription();
            sb.append("- documentType=").append(type)
                    .append(", orderIndex=").append(u.getOrderIndex())
                    .append(", description=\"").append(jsonEscape(desc)).append("\"\n");
        }
        return sb.toString();
    }

    // ============================================================
    // Tour prevalidation helpers
    // ============================================================

    List<TourIssueItem> buildDeterministicTourIssues(Tour tour) {
        List<TourIssueItem> issues = new ArrayList<>();

        // RN-011: espanol obligatorio.
        if (isSpanishMissing(tour.getName())) {
            issues.add(TourIssueItem.builder()
                    .severity(TourIssueItem.SEVERITY_CRITICAL)
                    .code("MISSING_ES_NAME")
                    .field("name")
                    .message("El nombre del tour no tiene texto en espanol (RN-011).")
                    .build());
        }
        if (isSpanishMissing(tour.getDescription())) {
            issues.add(TourIssueItem.builder()
                    .severity(TourIssueItem.SEVERITY_CRITICAL)
                    .code("MISSING_ES_DESCRIPTION")
                    .field("description")
                    .message("La descripcion del tour no tiene texto en espanol (RN-011).")
                    .build());
        }

        // RN-013: galeria — horizontal, min 3 imagenes recomendado.
        List<TourGallery> gallery = safeGallery(tour.getId());
        if (gallery.isEmpty()) {
            issues.add(TourIssueItem.builder()
                    .severity(TourIssueItem.SEVERITY_CRITICAL)
                    .code("GALLERY_EMPTY")
                    .field("gallery")
                    .message("El tour no tiene imagenes cargadas (RN-013 exige galeria).")
                    .build());
        } else if (gallery.size() < 3) {
            issues.add(TourIssueItem.builder()
                    .severity(TourIssueItem.SEVERITY_WARN)
                    .code("GALLERY_FEW_IMAGES")
                    .field("gallery")
                    .message("Solo hay " + gallery.size()
                            + " imagen(es) — se recomiendan 3 o mas para un tour publicable.")
                    .build());
        }

        // Politica de cancelacion.
        List<TourCancellationPolicy> policies = safeCancellationPolicy(tour.getId());
        if (policies.isEmpty()) {
            issues.add(TourIssueItem.builder()
                    .severity(TourIssueItem.SEVERITY_CRITICAL)
                    .code("CANCELLATION_POLICY_MISSING")
                    .field("cancellationPolicy")
                    .message("No hay politica de cancelacion definida (RN-046).")
                    .build());
        } else {
            TourCancellationPolicy p = policies.get(0);
            if (p.getCancellationPolicyType() == null) {
                issues.add(TourIssueItem.builder()
                        .severity(TourIssueItem.SEVERITY_CRITICAL)
                        .code("CANCELLATION_POLICY_TYPE_MISSING")
                        .field("cancellationPolicy.cancellationPolicyType")
                        .message("La politica de cancelacion no tiene tipo asignado.")
                        .build());
            }
        }

        return issues;
    }

    private List<TourGallery> safeGallery(Integer tourId) {
        try {
            List<TourGallery> list = tourGalleryRepository.findByTourIdOrderByOrderIndexAsc(tourId);
            return list == null ? List.of() : list;
        } catch (Exception ex) {
            log.debug("IA-08 gallery lookup failed tourId={}: {}", tourId, ex.getMessage());
            return List.of();
        }
    }

    private List<TourCancellationPolicy> safeCancellationPolicy(Integer tourId) {
        try {
            List<TourCancellationPolicy> list = tourCancellationPolicyRepository.findByTourId(tourId);
            return list == null ? List.of() : list;
        } catch (Exception ex) {
            log.debug("IA-08 cancellation policy lookup failed tourId={}: {}",
                    tourId, ex.getMessage());
            return List.of();
        }
    }

    private static boolean isSpanishMissing(TranslatedField field) {
        return field == null || field.getEs() == null || field.getEs().isBlank();
    }

    private static String tourEs(TranslatedField field) {
        if (field == null || field.getEs() == null) return "";
        return field.getEs();
    }

    private static String defaultTourReasoning(List<TourIssueItem> issues) {
        if (issues.isEmpty()) {
            return "El tour cumple los controles automaticos; revisar contenido antes de aprobar.";
        }
        long criticals = issues.stream()
                .filter(i -> TourIssueItem.SEVERITY_CRITICAL.equals(i.severity()))
                .count();
        if (criticals == 0) {
            return "Sin bloqueantes automaticos. Revisar los WARN antes de aprobar.";
        }
        return "Se detectaron " + criticals + " incumplimiento(s) critico(s) — el tour no debe aprobarse hasta corregirlos.";
    }

    String renderIssuesJson(List<TourIssueItem> issues) {
        try {
            ArrayNode arr = objectMapper.createArrayNode();
            for (TourIssueItem it : issues) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("severity", it.severity());
                node.put("code", it.code());
                node.put("field", it.field());
                node.put("message", it.message());
                arr.add(node);
            }
            // Aunque los issues no traen precios, corremos scrub por consistencia
            // (defense-in-depth).
            scrubSensitiveFields(arr);
            return objectMapper.writeValueAsString(arr);
        } catch (Exception ex) {
            return "[]";
        }
    }

    // ============================================================
    // DIMAR helpers
    // ============================================================

    List<DimarPassengerRow> buildPassengerRows(Reservation reservation) {
        List<DimarPassengerRow> rows = new ArrayList<>();

        Long itemId = reservation.getItemId();
        ShoppingCartItem item = itemId == null
                ? null
                : shoppingCartItemRepository.findById(itemId).orElse(null);

        String tourName = "";
        List<ShoppingCartItemDetail> details = List.of();
        if (item != null) {
            details = item.getDetails() == null ? List.of() : item.getDetails();
            if (item.getTourSchedule() != null && item.getTourSchedule().getTour() != null) {
                Tour tour = item.getTourSchedule().getTour();
                tourName = tourEs(tour.getName());
            } else if (item.getProductId() != null) {
                Optional<Tour> maybeTour = tourRepository.findById(item.getProductId());
                if (maybeTour.isPresent()) {
                    tourName = tourEs(maybeTour.get().getName());
                }
            }
        }

        Payment payment = reservation.getPaymentId() == null
                ? null
                : paymentRepository.findById(reservation.getPaymentId()).orElse(null);
        String payerName = payment == null ? "" : nullSafeString(payment.getPayerName());
        String docType = payment == null ? "" : nullSafeString(payment.getPayerDocumentType());
        String docNumber = payment == null ? "" : nullSafeString(payment.getPayerDocumentNumber());

        if (details.isEmpty()) {
            rows.add(DimarPassengerRow.builder()
                    .reservationId(reservation.getReservationId())
                    .tourName(tourName)
                    .payerName(payerName)
                    .documentType(docType)
                    .documentNumber(docNumber)
                    .ageType("ADULT")
                    .quantity(1)
                    .build());
            return rows;
        }
        for (ShoppingCartItemDetail d : details) {
            rows.add(DimarPassengerRow.builder()
                    .reservationId(reservation.getReservationId())
                    .tourName(tourName)
                    .payerName(payerName)
                    .documentType(docType)
                    .documentNumber(docNumber)
                    .ageType(d.getAgeType() == null ? "ADULT" : d.getAgeType().name())
                    .quantity(d.getQuantity() == null ? 0 : d.getQuantity())
                    .build());
        }
        return rows;
    }

    private static String nullSafeString(String s) {
        return s == null ? "" : s;
    }

    // ============================================================
    // Payout anomaly helpers
    // ============================================================

    List<PayoutAnomalyItem> buildPayoutAnomalyItems(ProviderPayoutOrder order,
                                                    List<ProviderPayoutOrderReservation> lines) {
        List<PayoutAnomalyItem> anomalies = new ArrayList<>();
        BigDecimal sumLines = BigDecimal.ZERO;

        for (ProviderPayoutOrderReservation line : lines) {
            BigDecimal actual = line.getAmount() == null ? BigDecimal.ZERO : line.getAmount();
            sumLines = sumLines.add(actual);

            AccountPayable expectedAp = null;
            if (line.getAccountPayableId() != null) {
                expectedAp = accountPayableRepository
                        .findById(line.getAccountPayableId())
                        .orElse(null);
            }
            if (expectedAp == null) {
                anomalies.add(PayoutAnomalyItem.builder()
                        .reservationId(line.getReservationId())
                        .accountPayableId(line.getAccountPayableId())
                        .expectedAmount(null)
                        .actualAmount(actual)
                        .discrepancy(null)
                        .code(PayoutAnomalyItem.CODE_MISSING_ACCOUNT_PAYABLE)
                        .build());
                continue;
            }
            BigDecimal expected = expectedAp.getAmount() == null ? BigDecimal.ZERO : expectedAp.getAmount();
            if (expected.compareTo(actual) != 0) {
                anomalies.add(PayoutAnomalyItem.builder()
                        .reservationId(line.getReservationId())
                        .accountPayableId(expectedAp.getId())
                        .expectedAmount(expected)
                        .actualAmount(actual)
                        .discrepancy(actual.subtract(expected))
                        .code(PayoutAnomalyItem.CODE_MISMATCH)
                        .build());
            }
        }

        BigDecimal amountTotal = order.getAmountTotal() == null ? BigDecimal.ZERO : order.getAmountTotal();
        if (amountTotal.compareTo(sumLines) != 0 && !lines.isEmpty()) {
            anomalies.add(PayoutAnomalyItem.builder()
                    .reservationId(null)
                    .accountPayableId(null)
                    .expectedAmount(sumLines)
                    .actualAmount(amountTotal)
                    .discrepancy(amountTotal.subtract(sumLines))
                    .code(PayoutAnomalyItem.CODE_TOTAL_DRIFT)
                    .build());
        }
        return anomalies;
    }

    private String renderAnomalyItemsJson(List<PayoutAnomalyItem> anomalies) {
        try {
            ArrayNode arr = objectMapper.createArrayNode();
            for (PayoutAnomalyItem a : anomalies) {
                ObjectNode n = objectMapper.createObjectNode();
                n.put("reservationId", a.reservationId());
                n.put("accountPayableId", a.accountPayableId());
                n.put("expectedAmount", a.expectedAmount() == null
                        ? null : a.expectedAmount().toPlainString());
                n.put("actualAmount", a.actualAmount() == null
                        ? null : a.actualAmount().toPlainString());
                n.put("discrepancy", a.discrepancy() == null
                        ? null : a.discrepancy().toPlainString());
                n.put("code", a.code());
                arr.add(n);
            }
            // Scrub por consistencia; los items no exponen providerPrice hoy.
            scrubSensitiveFields(arr);
            return objectMapper.writeValueAsString(arr);
        } catch (Exception ex) {
            return "[]";
        }
    }

    static String defaultAnomalyExplanation(List<PayoutAnomalyItem> anomalies) {
        if (anomalies.isEmpty()) {
            return "Sin anomalias detectadas — la orden concilia con las AccountPayable.";
        }
        long missing = anomalies.stream()
                .filter(a -> PayoutAnomalyItem.CODE_MISSING_ACCOUNT_PAYABLE.equals(a.code()))
                .count();
        long mismatches = anomalies.stream()
                .filter(a -> PayoutAnomalyItem.CODE_MISMATCH.equals(a.code()))
                .count();
        long totalDrift = anomalies.stream()
                .filter(a -> PayoutAnomalyItem.CODE_TOTAL_DRIFT.equals(a.code()))
                .count();
        StringBuilder sb = new StringBuilder("Anomalias detectadas: ");
        if (mismatches > 0) sb.append(mismatches).append(" mismatch(es), ");
        if (missing > 0) sb.append(missing).append(" sin AccountPayable, ");
        if (totalDrift > 0) sb.append(totalDrift).append(" desviacion(es) del total, ");
        // Trim trailing comma+space.
        String s = sb.toString();
        if (s.endsWith(", ")) s = s.substring(0, s.length() - 2);
        s += ". Revisar la de mayor discrepancia absoluta primero — RN-042 blinda que amount = providerPrice x quantity.";
        return s;
    }

    // ============================================================
    // Parsing helpers
    // ============================================================

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

    String parseReasoning(String llmContent) {
        try {
            JsonNode root = objectMapper.readTree(stripFences(llmContent));
            String reasoning = root.path("reasoning").asText("");
            return reasoning.isBlank() ? null : reasoning;
        } catch (Exception ex) {
            log.warn("IA-08 malformed tour_prevalidation JSON: {}", ex.getMessage());
            return null;
        }
    }

    String parseExplanation(String llmContent) {
        try {
            JsonNode root = objectMapper.readTree(stripFences(llmContent));
            String explanation = root.path("explanation").asText("");
            return explanation.isBlank() ? null : explanation;
        } catch (Exception ex) {
            log.warn("IA-08 malformed payout_anomaly JSON: {}", ex.getMessage());
            return null;
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
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
            log.error("IA-08 audit write failed capability={}", capability, ex);
        }
    }

    private static Integer extractUserId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof User u) return u.getId();
        return null;
    }

    // ============================================================
    // Test hooks — expose deterministic helpers for unit tests
    // ============================================================

    /** Test hook: KYB deterministic status aggregation. */
    public static String computeOverallStatusForTest(List<KybChecklistItem> items) {
        return computeOverallStatus(items);
    }

    /** Test hook: default anomaly explanation. */
    public static String defaultAnomalyExplanationForTest(List<PayoutAnomalyItem> items) {
        return defaultAnomalyExplanation(items);
    }

    /** Test hook: expose default tour reasoning. */
    public static String defaultTourReasoningForTest(List<TourIssueItem> issues) {
        return defaultTourReasoning(Collections.unmodifiableList(issues));
    }
}
