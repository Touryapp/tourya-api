package com.tourya.api.agents.concierge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tourya.api.agents.shared.AgentAuditEntry;
import com.tourya.api.agents.shared.AgentAuditWriter;
import com.tourya.api.agents.shared.AgentLlmResponse;
import com.tourya.api.agents.shared.BudgetGuard;
import com.tourya.api.agents.shared.ILlmClient;
import com.tourya.api.agents.shared.ModelPricing;
import com.tourya.api.agents.shared.PromptTemplate;
import com.tourya.api.models.User;
import com.tourya.api.models.request.AddItemToCartRequest;
import com.tourya.api.models.request.AddMultipleItemsToCartRequest;
import com.tourya.api.models.request.ConfigQuantityRequest;
import com.tourya.api.models.request.PublicTourScheduleSearchRequest;
import com.tourya.api.models.request.SlotRequest;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.models.responses.ShoppingCartResponse;
import com.tourya.api.models.responses.TourFullDataResponse;
import com.tourya.api.services.SearchTourScheduleFullService;
import com.tourya.api.services.ShoppingCartService;
import com.tourya.api.services.TourService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IA-02: Agente 1 del doc 16 — Travel Concierge.
 *
 * <p>Responsabilidades (ver doc 16 §Agente 1):</p>
 * <ol>
 *   <li>Explicar resultados de busqueda en lenguaje natural.</li>
 *   <li>Responder dudas pre-compra ancladas en la ficha real
 *       ({@link TourFullDataResponse}) — NUNCA inventa politica de cancelacion
 *       ni precio.</li>
 *   <li>Function calling contra el carrito ({@code search_tours},
 *       {@code get_tour_detail}, {@code add_to_cart}, {@code get_cart}).</li>
 *   <li>Sugerencia de metodo de pago alterno si Wompi falla — informativa,
 *       jamas procesa datos de tarjeta.</li>
 * </ol>
 *
 * <p><b>Guardrails no negociables (implementados en codigo, no solo en el prompt):</b></p>
 * <ul>
 *   <li>Deny-list de secretos: si el {@code userMessage} contiene
 *       {@code WOMPI_INTEGRITY_SECRET} o {@code JWT_SECRET} → responde generico
 *       + registra {@code escalated_to_human=true} en el audit log.</li>
 *   <li>Scrub de contexto pre-envio: elimina {@code slotPercentageTourya} /
 *       {@code providerPrice} de cualquier JSON antes de mandarlo al LLM.</li>
 *   <li>Contador de intentos de pago fallidos por sesion (in-memory ConcurrentHashMap):
 *       cuando {@code >= 3} setea {@code fraud_suspected=true} en el metadata
 *       sin mencionarlo al turista.</li>
 * </ul>
 */
@Slf4j
@Service
public class TravelConciergeService {

    private static final String AGENT_NAME = "TravelConcierge";
    private static final String PROMPT_VERSION = "v1";
    /** Modelo por defecto: Gemini 2.5 Pro (razonamiento + tool use). */
    private static final String DEFAULT_MODEL = ModelPricing.GEMINI_2_5_PRO;
    /** Loop cap para evitar function calling infinito (guardrail del doc 16). */
    private static final int MAX_ITERATIONS = 5;
    /** Umbral de intentos de pago fallidos que dispara {@code fraud_suspected}. */
    private static final int FRAUD_THRESHOLD = 3;
    private static final String[] SECRET_KEYWORDS = {
            "WOMPI_INTEGRITY_SECRET",
            "WOMPI_EVENTS_SECRET",
            "JWT_SECRET",
            "ANTHROPIC_API_KEY",
            "FIREBASE_ADMIN_SDK_JSON"
    };
    private static final String[] SCRUBBED_FIELDS = {
            "slotPercentageTourya", "slotPorcentajeTourya",
            "providerPrice", "porcentajeTourya"
    };

    private final ILlmClient llmClient;
    private final AgentAuditWriter auditWriter;
    private final BudgetGuard budgetGuard;
    private final TourService tourService;
    private final ShoppingCartService shoppingCartService;
    private final SearchTourScheduleFullService searchService;
    private final ObjectMapper objectMapper;
    private final PromptTemplate promptTemplate;

    /**
     * Contador de pagos fallidos por sesion. En Cloud Run con auto-scale este
     * mapa no se comparte entre replicas — es aceptable para el MVP porque el
     * cliente mantiene el mismo sessionId contra la misma replica durante una
     * conversacion corta. Migrar a Redis si aparece contencion real.
     */
    private final Map<String, Integer> failedPaymentCounter = new ConcurrentHashMap<>();

    public TravelConciergeService(
            ILlmClient llmClient,
            AgentAuditWriter auditWriter,
            BudgetGuard budgetGuard,
            TourService tourService,
            ShoppingCartService shoppingCartService,
            SearchTourScheduleFullService searchService,
            ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.auditWriter = auditWriter;
        this.budgetGuard = budgetGuard;
        this.tourService = tourService;
        this.shoppingCartService = shoppingCartService;
        this.searchService = searchService;
        this.objectMapper = objectMapper;
        this.promptTemplate = PromptTemplate.load("concierge.v1");
    }

    /**
     * Ejecuta una vuelta de conversacion del turista con el agente Concierge.
     *
     * @param request DTO validado a nivel controller.
     * @param auth    JWT del turista — usado para operaciones sobre su carrito.
     * @return respuesta natural + lista de acciones ejecutadas + flags de estado.
     */
    public ConciergeChatResponse chat(ConciergeChatRequest request, Authentication auth) {
        // 1. Presupuesto — si el mes se agoto, escalamos a humano.
        if (!budgetGuard.canRun(AGENT_NAME)) {
            log.warn("IA-02 budget exhausted — escalating to human sessionId={}", request.getSessionId());
            writeAudit(request, auth, AgentLlmResponse.disabled(), 0L,
                    "rejected", "budget_exhausted",
                    List.of(), false, true);
            return ConciergeChatResponse.builder()
                    .assistantMessage("Estoy con capacidad limitada por ahora, un agente humano te contactara pronto.")
                    .actionsExecuted(List.of())
                    .fraudSuspected(false)
                    .escalatedToHuman(true)
                    .sessionId(request.getSessionId())
                    .build();
        }

        // 2. Guardrail deny-list: intento de prompt injection buscando secretos.
        if (containsSecret(request.getUserMessage())) {
            log.warn("IA-02 secret keyword detected in userMessage sessionId={} — escalating",
                    request.getSessionId());
            writeAudit(request, auth, AgentLlmResponse.disabled(), 0L,
                    "rejected", "secret_keyword_in_input",
                    List.of(), false, true);
            return ConciergeChatResponse.builder()
                    .assistantMessage("No puedo procesar esa consulta. Te derivo con un agente humano.")
                    .actionsExecuted(List.of())
                    .fraudSuspected(false)
                    .escalatedToHuman(true)
                    .sessionId(request.getSessionId())
                    .build();
        }

        // 3. Cargar contexto inicial (tour focus + cart), scrub de campos internos.
        String tourDetailJson = "";
        if (request.getTourId() != null) {
            tourDetailJson = safeLoadTourDetail(request.getTourId(), auth);
        }
        String cartJson = safeLoadCart(auth);

        // 4. Loop de function calling — max MAX_ITERATIONS.
        List<ConciergeAction> actions = new ArrayList<>();
        String assistantMessage = "";
        AgentLlmResponse lastResponse = null;
        long totalDurationMs = 0L;
        String currentUserMessage = request.getUserMessage();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            String renderedPrompt = renderPrompt(request, currentUserMessage, tourDetailJson, cartJson);

            Instant start = Instant.now();
            AgentLlmResponse llmResponse = llmClient.complete(renderedPrompt, DEFAULT_MODEL);
            totalDurationMs += Duration.between(start, Instant.now()).toMillis();
            lastResponse = llmResponse;

            if (llmResponse.isError()) {
                log.warn("IA-02 LLM error at iteration {} — escalating: {}", iteration, llmResponse.error());
                break;
            }

            ParsedLlmOutput parsed = parseLlmContent(llmResponse.content());
            if (parsed.functionCall != null) {
                ConciergeAction action = executeFunction(parsed.functionCall, auth);
                actions.add(action);
                if ("get_cart".equals(action.name()) && action.success()) {
                    cartJson = safeLoadCart(auth); // Refresh
                }
                if ("get_tour_detail".equals(action.name()) && action.success()
                        && action.input().get("tourId") instanceof Number n) {
                    tourDetailJson = safeLoadTourDetail(n.intValue(), auth);
                }
                if ("add_to_cart".equals(action.name()) && action.success()) {
                    cartJson = safeLoadCart(auth);
                }
                // Alimentar el proximo turno con el resultado (mensaje del sistema).
                currentUserMessage = request.getUserMessage()
                        + "\n[SYSTEM: resultado de " + action.name()
                        + " → success=" + action.success()
                        + (action.error() != null ? " error=" + action.error() : "")
                        + "]";
                continue;
            }
            if (parsed.assistantMessage != null && !parsed.assistantMessage.isBlank()) {
                assistantMessage = parsed.assistantMessage;
                break;
            }
            // Sin function_call ni assistant_message — usamos el texto crudo como fallback.
            assistantMessage = llmResponse.content();
            break;
        }
        if (assistantMessage.isBlank() && lastResponse != null && lastResponse.isError()) {
            assistantMessage = "Estoy teniendo problemas para procesar tu mensaje. Intenta de nuevo en un momento.";
        }

        // 5. Actualizar contador de pagos fallidos si el turista lo menciono.
        boolean fraudSuspected = updateFraudCounter(request.getSessionId(), request.getUserMessage());

        // 6. Auditar SIEMPRE (Principio rector #4).
        writeAudit(request, auth, lastResponse != null ? lastResponse : AgentLlmResponse.disabled(),
                totalDurationMs, "autonomous_action", null,
                actions, fraudSuspected, false);

        return ConciergeChatResponse.builder()
                .assistantMessage(assistantMessage)
                .actionsExecuted(actions)
                .fraudSuspected(fraudSuspected)
                .escalatedToHuman(false)
                .sessionId(request.getSessionId())
                .build();
    }

    // ============================================================
    // Guardrails
    // ============================================================

    private boolean containsSecret(String input) {
        if (input == null) return false;
        String upper = input.toUpperCase();
        for (String kw : SECRET_KEYWORDS) {
            if (upper.contains(kw)) return true;
        }
        return false;
    }

    /**
     * Scrub sensible-fields de cualquier JsonNode antes de mandarlo al LLM.
     * Recorre recursivamente object + arrays. Devuelve el input mutado para
     * conveniencia. Nunca lanza — best-effort.
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

    private boolean updateFraudCounter(String sessionId, String userMessage) {
        if (sessionId == null || userMessage == null) return false;
        String lower = userMessage.toLowerCase();
        // Heuristica simple: menciones de "pago fallido", "payment failed", "no me cobra",
        // "wompi error" cuentan como un intento. Suficiente para el MVP.
        boolean paymentFailure = lower.contains("pago fall") || lower.contains("payment fail")
                || lower.contains("wompi error") || lower.contains("no me cobra")
                || lower.contains("tarjeta rechaz");
        if (!paymentFailure) {
            return failedPaymentCounter.getOrDefault(sessionId, 0) >= FRAUD_THRESHOLD;
        }
        int updated = failedPaymentCounter.merge(sessionId, 1, Integer::sum);
        boolean flag = updated >= FRAUD_THRESHOLD;
        if (flag) {
            log.warn("IA-02 fraud_suspected sessionId={} failedPayments={}", sessionId, updated);
        }
        return flag;
    }

    // ============================================================
    // Prompt rendering + parsing
    // ============================================================

    private String renderPrompt(ConciergeChatRequest request, String userMessage,
                                String tourDetailJson, String cartJson) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sessionId", request.getSessionId() == null ? "" : request.getSessionId());
        vars.put("locale", request.getLocale() == null ? "es" : request.getLocale());
        vars.put("tourId", request.getTourId() == null ? "" : request.getTourId());
        vars.put("cartJson", cartJson == null ? "{}" : cartJson);
        vars.put("tourDetailJson", tourDetailJson == null ? "" : tourDetailJson);
        vars.put("userMessage", userMessage);
        return promptTemplate.render(vars);
    }

    /**
     * El prompt le pide al LLM devolver JSON con {@code function_call} o
     * {@code assistant_message}. Este metodo parsea eso; si viene texto libre
     * (LLM no siguio la instruccion) devuelve el texto como assistantMessage.
     */
    ParsedLlmOutput parseLlmContent(String content) {
        if (content == null || content.isBlank()) return new ParsedLlmOutput(null, null);
        String trimmed = content.trim();
        // Algunos modelos envuelven el JSON en ```json ... ``` — quitamos las cercas.
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            int lastBackticks = trimmed.lastIndexOf("```");
            if (firstNl > 0 && lastBackticks > firstNl) {
                trimmed = trimmed.substring(firstNl + 1, lastBackticks).trim();
            }
        }
        if (!trimmed.startsWith("{")) {
            return new ParsedLlmOutput(null, content);
        }
        try {
            JsonNode root = objectMapper.readTree(trimmed);
            JsonNode fc = root.path("function_call");
            if (fc.isObject() && fc.hasNonNull("name")) {
                String name = fc.path("name").asText();
                JsonNode args = fc.path("arguments");
                Map<String, Object> arguments = args.isObject()
                        ? objectMapper.convertValue(args, Map.class)
                        : Collections.emptyMap();
                return new ParsedLlmOutput(new FunctionCall(name, arguments), null);
            }
            JsonNode msg = root.path("assistant_message");
            if (msg.isTextual()) {
                return new ParsedLlmOutput(null, msg.asText());
            }
        } catch (Exception ex) {
            log.debug("IA-02 LLM output no es JSON valido — se trata como texto libre: {}", ex.getMessage());
        }
        return new ParsedLlmOutput(null, content);
    }

    // ============================================================
    // Function dispatch
    // ============================================================

    @SuppressWarnings("unchecked")
    private ConciergeAction executeFunction(FunctionCall call, Authentication auth) {
        try {
            switch (call.name) {
                case "search_tours" -> {
                    PublicTourScheduleSearchRequest filters = new PublicTourScheduleSearchRequest();
                    if (call.arguments.get("textSearch") instanceof String txt) filters.setTextSearch(txt);
                    if (call.arguments.get("query") instanceof String q && filters.getTextSearch() == null) filters.setTextSearch(q);
                    if (call.arguments.get("categoryIds") instanceof List<?> ids) {
                        List<Integer> intIds = new ArrayList<>();
                        for (Object o : ids) if (o instanceof Number n) intIds.add(n.intValue());
                        filters.setCategoryIds(intIds);
                    }
                    if (call.arguments.get("dateFrom") instanceof String from) {
                        try { filters.setStartDate(LocalDate.parse(from)); } catch (Exception ignored) {}
                    }
                    if (call.arguments.get("dateTo") instanceof String to) {
                        try { filters.setEndDate(LocalDate.parse(to)); } catch (Exception ignored) {}
                    }
                    Pageable pageable = PageRequest.of(0, 10);
                    Page<SearchTourScheduleFullResponse> page = searchService.searchTourSchedule(filters, pageable, auth);
                    // Scrub del resultado antes de exponerlo al log (no lo mandamos al LLM,
                    // el LLM lo recibe en la proxima iteracion via safeLoadCart/tourDetail
                    // — el resultado directo de search NO se re-inyecta al prompt).
                    log.debug("IA-02 search_tours OK count={}", page.getTotalElements());
                    return new ConciergeAction(call.name, call.arguments, true, null);
                }
                case "get_tour_detail" -> {
                    Object tourIdObj = call.arguments.get("tourId");
                    if (!(tourIdObj instanceof Number n)) {
                        return new ConciergeAction(call.name, call.arguments, false, "tourId invalido o ausente");
                    }
                    TourFullDataResponse detail = tourService.getTourDetailsById(n.intValue(), auth);
                    log.debug("IA-02 get_tour_detail OK tourId={}", n.intValue());
                    return new ConciergeAction(call.name, call.arguments, detail != null,
                            detail == null ? "tour_not_found" : null);
                }
                case "add_to_cart" -> {
                    if (auth == null) {
                        return new ConciergeAction(call.name, call.arguments, false, "usuario no autenticado");
                    }
                    AddItemToCartRequest item = buildAddItemRequest(call.arguments);
                    if (item == null) {
                        return new ConciergeAction(call.name, call.arguments, false, "argumentos incompletos");
                    }
                    AddMultipleItemsToCartRequest req = AddMultipleItemsToCartRequest.builder()
                            .items(List.of(item))
                            .build();
                    shoppingCartService.addMultipleItemsToCart(req, auth);
                    return new ConciergeAction(call.name, call.arguments, true, null);
                }
                case "get_cart" -> {
                    if (auth == null) {
                        return new ConciergeAction(call.name, call.arguments, false, "usuario no autenticado");
                    }
                    ShoppingCartResponse cart = shoppingCartService.getActiveShoppingCartByUser(auth);
                    log.debug("IA-02 get_cart OK cartExists={}", cart != null);
                    return new ConciergeAction(call.name, call.arguments, true, null);
                }
                default -> {
                    return new ConciergeAction(call.name, call.arguments, false, "funcion_no_soportada");
                }
            }
        } catch (Exception ex) {
            log.warn("IA-02 function {} fallo: {}", call.name, ex.getMessage());
            // Mensaje generico para no exponer stack internos al cliente.
            return new ConciergeAction(call.name, call.arguments, false,
                    ex.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    private AddItemToCartRequest buildAddItemRequest(Map<String, Object> args) {
        try {
            AddItemToCartRequest req = new AddItemToCartRequest();
            if (args.get("productId") instanceof Number n) req.setProductId(n.intValue());
            req.setProductType("TOUR");
            if (args.get("tourScheduleId") instanceof Number n) req.setTourScheduleId(n.intValue());
            if (args.get("scheduleDate") instanceof String d) {
                try { req.setScheduleDate(LocalDate.parse(d)); } catch (Exception ignored) {}
            }
            if (args.get("slotId") instanceof Number slotIdN) {
                SlotRequest slot = new SlotRequest();
                slot.setId(slotIdN.longValue());
                List<ConfigQuantityRequest> details = new ArrayList<>();
                if (args.get("details") instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> m) {
                            ConfigQuantityRequest cq = new ConfigQuantityRequest();
                            Object at = m.get("ageType");
                            Object qty = m.get("quantity");
                            if (at instanceof String s) cq.setAgeType(s);
                            if (qty instanceof Number qn) cq.setQuantity(qn.intValue());
                            if (cq.getAgeType() != null && cq.getQuantity() != null) details.add(cq);
                        }
                    }
                }
                slot.setConfigQuantity(details);
                req.setSlot(slot);
            }
            if (req.getProductId() == null || req.getTourScheduleId() == null
                    || req.getScheduleDate() == null || req.getSlot() == null) {
                return null;
            }
            return req;
        } catch (Exception ex) {
            log.warn("IA-02 add_to_cart args parse fail: {}", ex.getMessage());
            return null;
        }
    }

    // ============================================================
    // Context loaders (scrub aplicado)
    // ============================================================

    private String safeLoadTourDetail(Integer tourId, Authentication auth) {
        try {
            TourFullDataResponse detail = tourService.getTourDetailsById(tourId, auth);
            if (detail == null) return "";
            JsonNode node = objectMapper.valueToTree(detail);
            scrubSensitiveFields(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            log.debug("IA-02 safeLoadTourDetail failed tourId={}: {}", tourId, ex.getMessage());
            return "";
        }
    }

    private String safeLoadCart(Authentication auth) {
        if (auth == null) return "{}";
        try {
            ShoppingCartResponse cart = shoppingCartService.getActiveShoppingCartByUser(auth);
            if (cart == null) return "{}";
            JsonNode node = objectMapper.valueToTree(cart);
            scrubSensitiveFields(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            log.debug("IA-02 safeLoadCart failed: {}", ex.getMessage());
            return "{}";
        }
    }

    // ============================================================
    // Audit
    // ============================================================

    private void writeAudit(ConciergeChatRequest request, Authentication auth,
                            AgentLlmResponse response, long durationMs,
                            String resultType, String errorMessage,
                            List<ConciergeAction> actions, boolean fraudSuspected,
                            boolean escalated) {
        try {
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("session_id", request.getSessionId());
            metadata.put("fraud_suspected", fraudSuspected);
            metadata.put("escalated_to_human", escalated);
            metadata.set("actions_executed", objectMapper.valueToTree(
                    actions.stream().map(a -> Map.of(
                            "name", a.name(),
                            "success", a.success()
                    )).toList()));
            Integer userId = extractUserId(auth);
            AgentAuditEntry entry = AgentAuditEntry.builder()
                    .agentName(AGENT_NAME)
                    .model(response.model() == null ? DEFAULT_MODEL : response.model())
                    .promptVersion(PROMPT_VERSION)
                    .inputTokens(response.inputTokens())
                    .outputTokens(response.outputTokens())
                    .costUsd(response.costUsd())
                    .durationMs(durationMs)
                    .entityType("user")
                    .entityId(userId == null ? null : userId.longValue())
                    .userId(userId)
                    .resultType(resultType)
                    .resultJson(null)
                    .promptInput(truncate(request.getUserMessage(), 2000))
                    .errorMessage(errorMessage)
                    .metadata(objectMapper.writeValueAsString(metadata))
                    .build();
            auditWriter.register(entry);
        } catch (Exception ex) {
            log.error("IA-02 audit write failed sessionId={}", request.getSessionId(), ex);
        }
    }

    private Integer extractUserId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof User u) return u.getId();
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ============================================================
    // Parse helpers
    // ============================================================

    record FunctionCall(String name, Map<String, Object> arguments) {}
    record ParsedLlmOutput(FunctionCall functionCall, String assistantMessage) {}
}
