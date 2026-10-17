package com.tourya.api.agents.concierge;

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
    private final com.tourya.api.repository.AgentAuditLogRepository auditLogRepository;
    private final PromptTemplate promptTemplate;

    /**
     * v24 (issue #39 Luis 2026-10-06): cuantos turns previos cargamos de la
     * misma sessionId para reinyectar como historial al prompt. Luis reporto
     * que el agente "repite las mismas preguntas" — sin memoria entre POSTs
     * el LLM no sabia los slots ya confirmados (fechas / viajeros / interes).
     */
    private static final int HISTORY_TURNS = 6;

    /**
     * Contador de pagos fallidos por sesion. En Cloud Run con auto-scale este
     * mapa no se comparte entre replicas — es aceptable para el MVP porque el
     * cliente mantiene el mismo sessionId contra la misma replica durante una
     * conversacion corta. Migrar a Redis si aparece contencion real.
     */
    private final Map<String, Integer> failedPaymentCounter = new ConcurrentHashMap<>();

    /**
     * v25 (issue #39 Luis 2026-10-08): cache in-memory del historial reciente
     * de cada sesion. En v24 solo usabamos agent_audit_log — pero
     * {@link AgentAuditWriter#register} es {@code @Async} y en Cloud Run con
     * una sola instancia dev los turns se envian casi inmediatamente, asi
     * que al momento de leer BD del turn N+1 el audit del turn N puede no
     * estar aun flushed. El cache arregla ese race y asegura memoria dentro
     * del mismo proceso. La BD queda como fallback para turns antiguos o
     * restart del container.
     *
     * <p>Eviction barata: cada sesion guarda max {@link #HISTORY_TURNS}
     * turns; LinkedList como ArrayDeque equivalente. No hay TTL — el
     * tamaño del mapa queda bounded por sesiones activas y Cloud Run
     * recicla instancias periodicamente.</p>
     */
    private final Map<String, java.util.Deque<ConversationTurn>> conversationCache
            = new ConcurrentHashMap<>();

    private record ConversationTurn(String userMessage, String assistantMessage) {}

    public TravelConciergeService(
            ILlmClient llmClient,
            AgentAuditWriter auditWriter,
            BudgetGuard budgetGuard,
            TourService tourService,
            ShoppingCartService shoppingCartService,
            SearchTourScheduleFullService searchService,
            ObjectMapper objectMapper,
            com.tourya.api.repository.AgentAuditLogRepository auditLogRepository) {
        this.llmClient = llmClient;
        this.auditWriter = auditWriter;
        this.budgetGuard = budgetGuard;
        this.tourService = tourService;
        this.shoppingCartService = shoppingCartService;
        this.searchService = searchService;
        this.objectMapper = objectMapper;
        this.auditLogRepository = auditLogRepository;
        this.promptTemplate = PromptTemplate.load("concierge.v2");
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

        // v24 (issue #39): cargar historial de la sesion para que el agente
        // recuerde los slots ya confirmados en turns previos. Si falla la carga
        // (sessionId null, BD caida, etc.) seguimos sin historial — no bloquea.
        String historyText = safeLoadConversationHistory(request.getSessionId());

        // 4. Loop de function calling — max MAX_ITERATIONS.
        List<ConciergeAction> actions = new ArrayList<>();
        String assistantMessage = "";
        AgentLlmResponse lastResponse = null;
        long totalDurationMs = 0L;
        String currentUserMessage = request.getUserMessage();
        // v27 (#39 Luis 2026-10-14): acumulamos nombres de tours que REALMENTE
        // devolvio search_tours en esta sesion. Es el whitelist contra el que
        // validamos el assistant_message al final para evitar alucinaciones
        // (ej. LLM proponiendo "Johnny Cay" sin que haya venido en el payload).
        java.util.Set<String> allowedTourNamesLower = new java.util.HashSet<>();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            String renderedPrompt = renderPrompt(request, currentUserMessage, tourDetailJson, cartJson, historyText);

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
                // v29 (#39 Luis 2026-10-15): log de function_call para diagnosticar
                // sesiones donde el LLM NO llama search_tours (prompt v28 removio
                // ejemplos y aparentemente el modelo perdio pistas para armar los
                // params). Con este log vemos exactamente cuando el LLM invoca
                // search_tours y con que categoryIds/dateFrom/textSearch.
                log.info("IA-02 function_call sessionId={} iter={} name={} args={}",
                        request.getSessionId(), iteration, parsed.functionCall.name(),
                        parsed.functionCall.arguments());
                ConciergeAction action = executeFunction(parsed.functionCall, auth);
                actions.add(action);
                // v29: log del resultado inmediato — util para distinguir
                // "search_tours devolvio []" (bug de datos) vs "LLM nunca llamo
                // search_tours" (bug de prompt). Para search_tours loggeamos la
                // cantidad de tours en el array, no el payload completo.
                if ("search_tours".equals(action.name())) {
                    int toursCount = extractToursCount(action.result());
                    log.info("IA-02 search_tours result sessionId={} success={} toursReturned={}",
                            request.getSessionId(), action.success(), toursCount);
                }
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
                // Issue #39 v2: ahora inyectamos tambien action.result() serializado
                // cuando esta presente. Esto cierra el gap que hacia al agente
                // alucinar tours — antes solo veia "success=true", ahora ve la
                // lista real y puede recomendarla sin inventar.
                //
                // Issue #39 v2 refinamiento (Luis 2026-10-03 reporto "Hubo un error"):
                // cap de 6000 chars sobre el payload serializado. Un search_tours
                // con 10 tours completos puede meter 15-20k chars al prompt y
                // tirar al LLM por exceso de contexto. Si excede, degradamos a
                // un resumen con los campos esenciales (id/nombre/precio).
                StringBuilder sysNote = new StringBuilder()
                        .append("\n[SYSTEM: resultado de ").append(action.name())
                        .append(" → success=").append(action.success());
                if (action.error() != null) sysNote.append(" error=").append(action.error());
                if (action.result() != null && !action.result().isNull()) {
                    try {
                        String payload = objectMapper.writeValueAsString(action.result());
                        if (payload.length() > 6000) {
                            payload = summarizeLargePayload(action.name(), action.result());
                        }
                        sysNote.append("\ndata=").append(payload);
                    } catch (Exception serEx) {
                        log.debug("IA-02 no se pudo serializar result de {}: {}", action.name(), serEx.getMessage());
                    }
                    // v27 (#39): recolectar nombres de tours del payload crudo
                    // para el guard anti-alucinacion. Solo tours con schedules
                    // no vacios cuentan como "ofrecibles".
                    if ("search_tours".equals(action.name())) {
                        collectAllowedTourNames(action.result(), allowedTourNamesLower);
                    }
                }
                sysNote.append("]");
                currentUserMessage = request.getUserMessage() + sysNote;
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

        // v27 (#39 Luis 2026-10-14): guard defensivo anti-alucinacion. Si el
        // LLM ignoro el prompt y propuso un tour clasico de San Andres que no
        // vino en search_tours, reescribimos a un mensaje seguro. Defense in
        // depth — el prompt es la primera linea, esto la segunda.
        assistantMessage = rewriteIfHallucinated(assistantMessage, allowedTourNamesLower);
        if (assistantMessage.isBlank() && lastResponse != null && lastResponse.isError()) {
            assistantMessage = "Estoy teniendo problemas para procesar tu mensaje. Intenta de nuevo en un momento.";
        }

        // 5. Actualizar contador de pagos fallidos si el turista lo menciono.
        boolean fraudSuspected = updateFraudCounter(request.getSessionId(), request.getUserMessage());

        // v25 (issue #39): append al cache in-memory ANTES del audit — es
        // el fallback inmediato para el proximo turn mientras el audit async
        // todavia esta en vuelo.
        appendTurnToCache(request.getSessionId(), request.getUserMessage(), assistantMessage);

        // 6. Auditar SIEMPRE (Principio rector #4) + persistir assistant_message
        // para que turns futuros reconstruyan historial (v24 issue #39).
        writeAudit(request, auth, lastResponse != null ? lastResponse : AgentLlmResponse.disabled(),
                totalDurationMs, "autonomous_action", null,
                actions, fraudSuspected, false, assistantMessage);

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
                                String tourDetailJson, String cartJson, String historyText) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sessionId", request.getSessionId() == null ? "" : request.getSessionId());
        vars.put("locale", request.getLocale() == null ? "es" : request.getLocale());
        vars.put("tourId", request.getTourId() == null ? "" : request.getTourId());
        vars.put("cartJson", cartJson == null ? "{}" : cartJson);
        vars.put("tourDetailJson", tourDetailJson == null ? "" : tourDetailJson);
        vars.put("conversationHistory", historyText == null ? "(sin historial — es el primer mensaje de la sesión)" : historyText);
        vars.put("userMessage", userMessage);
        // v30 (issue #39): sin fecha actual, Gemini usaba su knowledge cutoff
        // (ej. dateFrom=2024-10-20 cuando el turista decia "del 20 al 27 de
        // octubre"). Resultado: search_tours siempre devolvia toursReturned=0.
        vars.put("currentDate", LocalDate.now().toString());
        return promptTemplate.render(vars);
    }

    /**
     * v25 (issue #39): reconstruye el historial de la sesion. Prioriza el
     * cache in-memory (seguro contra la ventana async de audit); si viene
     * vacio o incompleto cae a agent_audit_log. Devuelve bloque de texto
     * plain con los ultimos {@link #HISTORY_TURNS} turns en orden cronologico
     * ("[turista]: ... / [agente]: ..."). Null si no hay historial.
     */
    private String safeLoadConversationHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;

        // 1) Cache en memoria — siempre consistente con el proceso actual.
        java.util.Deque<ConversationTurn> cached = conversationCache.get(sessionId);
        if (cached != null && !cached.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConversationTurn turn : cached) {
                if (turn.userMessage() != null && !turn.userMessage().isBlank()) {
                    sb.append("[turista]: ").append(turn.userMessage()).append("\n");
                }
                if (turn.assistantMessage() != null && !turn.assistantMessage().isBlank()) {
                    sb.append("[agente]: ").append(turn.assistantMessage()).append("\n");
                }
            }
            String result = sb.toString().trim();
            if (!result.isBlank()) {
                log.info("IA-02 historial desde cache sessionId={} turns={}", sessionId, cached.size());
                return result;
            }
        }

        // 2) Fallback a BD para sesiones viejas / container reiniciado.
        try {
            List<com.tourya.api.models.AgentAuditLog> rows =
                    auditLogRepository.findRecentBySessionId(AGENT_NAME, sessionId, HISTORY_TURNS);
            if (rows == null || rows.isEmpty()) {
                log.info("IA-02 sin historial previo sessionId={}", sessionId);
                return null;
            }

            // Vienen DESC — invertimos para ir del mas viejo al mas nuevo.
            java.util.Collections.reverse(rows);

            StringBuilder sb = new StringBuilder();
            for (com.tourya.api.models.AgentAuditLog row : rows) {
                String userPart = extractUserMessageFromPrompt(row.getPromptInput());
                String assistantPart = extractAssistantMessageFromResult(row.getResultJson());
                if (userPart != null && !userPart.isBlank()) {
                    sb.append("[turista]: ").append(userPart).append("\n");
                }
                if (assistantPart != null && !assistantPart.isBlank()) {
                    sb.append("[agente]: ").append(assistantPart).append("\n");
                }
            }
            String result = sb.toString().trim();
            log.info("IA-02 historial desde BD sessionId={} rows={}", sessionId, rows.size());
            return result.isBlank() ? null : result;
        } catch (Exception ex) {
            log.warn("IA-02 no pude cargar historial sessionId={}: {}", sessionId, ex.getMessage());
            return null;
        }
    }

    /**
     * v25 (issue #39): agrega el turn actual al cache in-memory inmediatamente
     * tras obtener la respuesta del LLM. Esto evita la ventana de race donde
     * el audit async todavia no persistio al momento de leer BD en el proximo
     * turn del mismo sessionId.
     */
    private void appendTurnToCache(String sessionId, String userMessage, String assistantMessage) {
        if (sessionId == null || sessionId.isBlank()) return;
        if ((userMessage == null || userMessage.isBlank())
                && (assistantMessage == null || assistantMessage.isBlank())) {
            return;
        }
        java.util.Deque<ConversationTurn> q = conversationCache.computeIfAbsent(
                sessionId, k -> new java.util.concurrent.ConcurrentLinkedDeque<>());
        q.add(new ConversationTurn(
                userMessage == null ? "" : userMessage.trim(),
                assistantMessage == null ? "" : assistantMessage.trim()));
        // Trim a HISTORY_TURNS (cada entry = 1 par user/assistant).
        while (q.size() > HISTORY_TURNS) {
            q.pollFirst();
        }
    }

    /** El audit guarda el user message crudo en promptInput — lo usamos tal cual. */
    private String extractUserMessageFromPrompt(String promptInput) {
        if (promptInput == null) return null;
        String trimmed = promptInput.trim();
        if (trimmed.length() > 500) trimmed = trimmed.substring(0, 500) + "...";
        return trimmed;
    }

    /** Extrae el assistant_message del result_json del turn previo. */
    private String extractAssistantMessageFromResult(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode msg = root.path("assistant_message");
            if (msg.isTextual()) {
                String text = msg.asText();
                return text.length() > 500 ? text.substring(0, 500) + "..." : text;
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
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
                    // Issue #39 (2026-10-17): el LLM solia mandar textSearch con
                    // el nombre coloquial de la actividad (ej. "carro playero"),
                    // que depende del campo name del tour para matchear. Varios
                    // tours tienen nombres propios ("Mulita para 4 pasajeros")
                    // que NO contienen el termino coloquial — subCategory es el
                    // enum code que identifica la actividad y da matches
                    // consistentes. Lo exponemos para que el LLM pueda filtrar
                    // por el, en lugar de depender del nombre del tour.
                    if (call.arguments.get("subCategories") instanceof List<?> subs) {
                        List<String> subList = new ArrayList<>();
                        for (Object o : subs) if (o instanceof String s) subList.add(s);
                        filters.setSubCategories(subList);
                    }
                    if (call.arguments.get("dateFrom") instanceof String from) {
                        try { filters.setStartDate(LocalDate.parse(from)); } catch (Exception ignored) {}
                    }
                    if (call.arguments.get("dateTo") instanceof String to) {
                        try { filters.setEndDate(LocalDate.parse(to)); } catch (Exception ignored) {}
                    }
                    Pageable pageable = PageRequest.of(0, 10);
                    Page<SearchTourScheduleFullResponse> page = searchService.searchTourSchedule(filters, pageable, auth);
                    log.debug("IA-02 search_tours OK count={}", page.getTotalElements());
                    // Issue #39 v2: antes el resultado NO se re-inyectaba al LLM
                    // por una decision de diseno equivocada — el agente no sabia
                    // que tours existian y los inventaba. Ahora serializamos la
                    // pagina a JsonNode y la pasamos en el result; el loop la
                    // inyecta al prompt del proximo turno via [SYSTEM: ...].
                    JsonNode searchResult = objectMapper.valueToTree(page.getContent());
                    scrubSensitiveFields(searchResult);
                    return new ConciergeAction(call.name, call.arguments, true, null, searchResult);
                }
                case "get_tour_detail" -> {
                    Object tourIdObj = call.arguments.get("tourId");
                    if (!(tourIdObj instanceof Number n)) {
                        return new ConciergeAction(call.name, call.arguments, false, "tourId invalido o ausente");
                    }
                    TourFullDataResponse detail = tourService.getTourDetailsById(n.intValue(), auth);
                    log.debug("IA-02 get_tour_detail OK tourId={}", n.intValue());
                    // v2: incluir el detail como result para que el LLM pueda
                    // razonar sobre precio, politica, etc. sin alucinar.
                    JsonNode detailJson = detail == null ? null : objectMapper.valueToTree(detail);
                    if (detailJson != null) scrubSensitiveFields(detailJson);
                    return new ConciergeAction(call.name, call.arguments, detail != null,
                            detail == null ? "tour_not_found" : null, detailJson);
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
                    // v2: result con el carrito scrubbed. El loop tambien
                    // refresca cartJson en la variable del prompt base, pero
                    // esto permite al LLM ver el detalle completo en el mismo
                    // turno sin esperar al proximo render.
                    JsonNode cartJsonNode = cart == null ? null : objectMapper.valueToTree(cart);
                    if (cartJsonNode != null) scrubSensitiveFields(cartJsonNode);
                    return new ConciergeAction(call.name, call.arguments, true, null, cartJsonNode);
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

    /**
     * v27 (#39 Luis 2026-10-14): nombres clasicos de tours de San Andres que
     * el LLM tiende a inventar — alucinaciones recurrentes vistas en los
     * ciclos QA v23-v26. Si el {@code assistant_message} menciona uno Y ese
     * nombre no aparece en ninguno de los {@code search_tours} de la sesion,
     * lo reescribimos. Lista minuscula para match case-insensitive.
     */
    private static final String[] HALLUCINATION_TRIGGERS = {
            "johnny cay", "acuario", "rocky cay", "parasail",
            "mulitas", "carro playero", "cayo bolivar", "west view",
            "piscinita", "hoyo soplador", "bioluminiscente"
    };

    /**
     * v27 (#39): extrae los {@code tour.name} con schedules no vacios del
     * payload crudo de search_tours y los agrega al whitelist. Siempre
     * minuscula para comparar sin importar capitalizacion.
     * Package-private para testear directo.
     */
    /**
     * v29 (#39 Luis 2026-10-15): devuelve la cantidad de tours en el array
     * `data` del result de search_tours — o -1 si la estructura no es la
     * esperada. Lo usamos para loggear "search_tours devolvio 0 tours vs 5"
     * y poder diferenciar bug de datos (0 = no hay tours en la fecha) de
     * bug de prompt (LLM nunca llamo search_tours — no se loggea esta linea).
     */
    int extractToursCount(JsonNode searchToursResult) {
        if (searchToursResult == null || searchToursResult.isNull()) return -1;
        if (searchToursResult.isArray()) return searchToursResult.size();
        return -1;
    }

    void collectAllowedTourNames(JsonNode searchToursResult, java.util.Set<String> acc) {
        if (searchToursResult == null || !searchToursResult.isArray()) return;
        for (JsonNode item : searchToursResult) {
            JsonNode schedules = item.get("schedules");
            boolean hasSchedules = schedules != null && schedules.isArray() && schedules.size() > 0;
            if (!hasSchedules) continue;
            JsonNode tour = item.get("tour");
            if (tour == null || tour.isNull()) continue;
            JsonNode nameNode = tour.get("name");
            if (nameNode == null || nameNode.isNull()) continue;
            // name puede ser {es,en,pt} o string plano.
            if (nameNode.isTextual()) {
                acc.add(nameNode.asText().toLowerCase());
            } else if (nameNode.isObject()) {
                nameNode.fieldNames().forEachRemaining(lang -> {
                    JsonNode v = nameNode.get(lang);
                    if (v != null && v.isTextual()) acc.add(v.asText().toLowerCase());
                });
            }
        }
    }

    /**
     * v27 (#39): si el mensaje final menciona un trigger de alucinacion que
     * NO esta en el whitelist de nombres reales de la sesion, reescribimos
     * el mensaje a un fallback seguro. False positives aceptables (es mejor
     * ofrecer menos que inventar). Package-private para testear.
     */
    String rewriteIfHallucinated(String assistantMessage, java.util.Set<String> allowedTourNamesLower) {
        if (assistantMessage == null || assistantMessage.isBlank()) return assistantMessage;
        String lower = assistantMessage.toLowerCase();
        for (String trigger : HALLUCINATION_TRIGGERS) {
            if (!lower.contains(trigger)) continue;
            // Si el trigger aparece como parte de un nombre real que vino en
            // search_tours (ej. "Paseo Johnny Cay" en el whitelist), no es
            // alucinacion.
            boolean inAllowed = allowedTourNamesLower.stream().anyMatch(n -> n.contains(trigger));
            if (inAllowed) continue;
            log.warn("IA-02 anti-hallucination guard: assistant_message menciono '{}' sin que estuviera en search_tours. Reescribiendo.", trigger);
            return "Por el momento no encontre tours con disponibilidad para esas fechas. "
                    + "Querés que amplie el rango de fechas o buscamos otra categoria de actividad "
                    + "(acuaticas, terrestres, cayos, culturales)?";
        }
        return assistantMessage;
    }

    /**
     * Issue #39 v2 refinamiento: cuando el payload del tool call es muy
     * grande (search_tours con 10 tours completos puede pasar 15k chars),
     * reemplazamos el JSON entero con un resumen de campos esenciales.
     * Evita que el prompt al LLM crezca sin control y tire el request a
     * Gemini por exceso de tokens.
     *
     * <p>v25b (issue #39, Luis 2026-10-14): el resumen de search_tours leia
     * campos planos (tourId, tourName, duration, categoryName, priceFrom,
     * scheduleDate) que NO existen al top-level de
     * {@link SearchTourScheduleFullResponse} — el objeto real tiene todo
     * anidado como {@code {tour:{id,name,duration,categoryName,
     * subCategoryName,priceFrom,...}, schedules:[{scheduleDate,...},...]}}.
     * Por eso cada tour quedaba {} y el LLM respondia "no hay disponibilidad"
     * aunque la busqueda si retornara resultados. Ahora extraemos del nodo
     * anidado y devolvemos todas las fechas de schedules para que el LLM
     * tenga el abanico de disponibilidad.</p>
     *
     * <p>Package-private para que el test unitario pueda invocarlo directo
     * sobre payloads sinteticos (igual que {@link #scrubSensitiveFields}).</p>
     */
    String summarizeLargePayload(String actionName, JsonNode result) {
        try {
            if ("search_tours".equals(actionName) && result.isArray()) {
                ArrayNode summary = objectMapper.createArrayNode();
                for (JsonNode item : result) {
                    ObjectNode mini = objectMapper.createObjectNode();
                    JsonNode tour = item.get("tour");
                    if (tour != null && !tour.isNull()) {
                        if (tour.has("id")) mini.set("tourId", tour.get("id"));
                        if (tour.has("name")) mini.set("tourName", tour.get("name"));
                        if (tour.has("duration")) mini.set("duration", tour.get("duration"));
                        if (tour.has("categoryName")) mini.set("categoryName", tour.get("categoryName"));
                        if (tour.has("subCategoryName")) mini.set("subCategoryName", tour.get("subCategoryName"));
                        if (tour.has("priceFrom")) mini.set("priceFrom", tour.get("priceFrom"));
                    }
                    JsonNode schedules = item.get("schedules");
                    if (schedules != null && schedules.isArray() && schedules.size() > 0) {
                        ArrayNode dates = objectMapper.createArrayNode();
                        for (JsonNode s : schedules) {
                            if (s.has("scheduleDate")) dates.add(s.get("scheduleDate"));
                        }
                        mini.set("scheduleDates", dates);
                    }
                    summary.add(mini);
                }
                return objectMapper.writeValueAsString(summary);
            }
            // Para get_tour_detail / get_cart truncamos y agregamos marker.
            String raw = objectMapper.writeValueAsString(result);
            return raw.substring(0, Math.min(raw.length(), 4000))
                    + "...[truncado por tamaño]";
        } catch (Exception ex) {
            return "{\"error\":\"payload truncation failed\"}";
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
        writeAudit(request, auth, response, durationMs, resultType, errorMessage,
                actions, fraudSuspected, escalated, null);
    }

    private void writeAudit(ConciergeChatRequest request, Authentication auth,
                            AgentLlmResponse response, long durationMs,
                            String resultType, String errorMessage,
                            List<ConciergeAction> actions, boolean fraudSuspected,
                            boolean escalated, String assistantMessage) {
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

            // v24 (issue #39): persistir el assistant_message en result_json
            // para que turns siguientes puedan reconstruir el historial via
            // findRecentBySessionId. Si no hay respuesta final (ej. rechazo
            // por budget / secret keyword) guardamos null.
            String resultJson = null;
            if (assistantMessage != null && !assistantMessage.isBlank()) {
                ObjectNode result = objectMapper.createObjectNode();
                result.put("assistant_message", truncate(assistantMessage, 2000));
                resultJson = objectMapper.writeValueAsString(result);
            }

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
                    .resultJson(resultJson)
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
