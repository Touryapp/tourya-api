package com.tourya.api.agents.shared;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * IA-02: mock deterministico de {@link ILlmClient} para tests unitarios.
 *
 * <p>Uso: se registra una secuencia de respuestas (una por iteracion del loop
 * de function calling del agente); {@link #complete(String, String)} las
 * devuelve en orden y captura el prompt real para asserts (ver
 * {@link #capturedPrompts()}).</p>
 *
 * <p>Alternativa avanzada: registrar un {@code Function<String, AgentLlmResponse>}
 * que decide la respuesta segun el prompt (util para escenarios que dependen
 * del texto real).</p>
 */
public class MockLlmClient implements ILlmClient {

    private final List<AgentLlmResponse> queue = new ArrayList<>();
    private final List<String> capturedPrompts = new ArrayList<>();
    private Function<String, AgentLlmResponse> dynamicResolver;

    public MockLlmClient enqueue(AgentLlmResponse response) {
        queue.add(response);
        return this;
    }

    public MockLlmClient enqueueText(String text) {
        return enqueue(new AgentLlmResponse(text, "gemini-2.5-pro", 10, 20,
                BigDecimal.ZERO, "end_turn", null));
    }

    public MockLlmClient enqueueFunctionCall(String name, String argumentsJson) {
        String content = "{\"function_call\":{\"name\":\"" + name + "\",\"arguments\":" + argumentsJson + "}}";
        return enqueue(new AgentLlmResponse(content, "gemini-2.5-pro", 10, 20,
                BigDecimal.ZERO, "tool_use", null));
    }

    public MockLlmClient enqueueAssistantMessage(String message) {
        String content = "{\"assistant_message\":\"" + escape(message) + "\"}";
        return enqueue(new AgentLlmResponse(content, "gemini-2.5-pro", 10, 20,
                BigDecimal.ZERO, "end_turn", null));
    }

    public MockLlmClient enqueueError(String reason) {
        return enqueue(AgentLlmResponse.error(reason));
    }

    public MockLlmClient withDynamicResolver(Function<String, AgentLlmResponse> resolver) {
        this.dynamicResolver = resolver;
        return this;
    }

    public List<String> capturedPrompts() {
        return capturedPrompts;
    }

    public String lastPrompt() {
        return capturedPrompts.isEmpty() ? null : capturedPrompts.get(capturedPrompts.size() - 1);
    }

    @Override
    public AgentLlmResponse complete(String prompt, String model) {
        capturedPrompts.add(prompt);
        if (dynamicResolver != null) {
            return dynamicResolver.apply(prompt);
        }
        if (queue.isEmpty()) {
            return AgentLlmResponse.error("mock_queue_empty");
        }
        return queue.remove(0);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
