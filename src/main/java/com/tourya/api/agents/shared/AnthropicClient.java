package com.tourya.api.agents.shared;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * IA-01: implementación del cliente Anthropic Messages API v1.
 *
 * <p>Endpoint: {@code POST {base-url}/v1/messages}. Auth: header {@code x-api-key}.
 * Payload mínimo: <code>{model, max_tokens, messages: [{role: user, content: [{type: text, text: prompt}]}]}</code>.</p>
 *
 * <p><b>Modo no-op degradado:</b> si {@code agents.anthropic.api-key} no está
 * seteado (patrón MO-40 FCM), devuelve {@link AgentLlmResponse#disabled} sin
 * llamar. Los agentes futuros manejarán este caso escribiendo al audit log
 * con resultType="rejected".</p>
 *
 * <p><b>Manejo de errores:</b> nunca throws. HTTP errors, timeouts y parse
 * fallidos devuelven {@link AgentLlmResponse#error} — decisión intencional
 * para que el flujo del agente no se rompa por un problema del LLM y el
 * incidente quede en el audit log.</p>
 */
@Slf4j
@Component
public class AnthropicClient implements ILlmClient {

    private static final String API_VERSION = "2023-06-01";
    private static final int DEFAULT_MAX_TOKENS = 1024;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public AnthropicClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${agents.anthropic.api-key:}") String apiKey,
            @Value("${agents.anthropic.base-url:https://api.anthropic.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public AgentLlmResponse complete(String prompt, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("IA-01 Anthropic disabled: agents.anthropic.api-key not configured");
            return AgentLlmResponse.disabled();
        }
        if (prompt == null || prompt.isBlank()) {
            return AgentLlmResponse.error("empty_prompt");
        }
        if (model == null || model.isBlank()) {
            return AgentLlmResponse.error("empty_model");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", API_VERSION);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", DEFAULT_MAX_TOKENS,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(Map.of("type", "text", "text", prompt))
                    ))
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/v1/messages",
                    org.springframework.http.HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            return parseResponse(response.getBody(), model);
        } catch (HttpStatusCodeException ex) {
            log.error("IA-01 Anthropic HTTP {} error: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            return AgentLlmResponse.error("http_" + ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("IA-01 Anthropic call failed", ex);
            return AgentLlmResponse.error("network_or_parse_error");
        }
    }

    /**
     * Formato de response de Anthropic Messages API:
     * <pre>
     * {
     *   "content": [{"type": "text", "text": "..."}],
     *   "model": "claude-...",
     *   "stop_reason": "end_turn",
     *   "usage": {"input_tokens": 42, "output_tokens": 128}
     * }
     * </pre>
     */
    private AgentLlmResponse parseResponse(String rawBody, String requestedModel) throws Exception {
        JsonNode root = objectMapper.readTree(rawBody);
        JsonNode contentArray = root.path("content");
        String content = "";
        if (contentArray.isArray() && !contentArray.isEmpty()) {
            content = contentArray.get(0).path("text").asText("");
        }
        String returnedModel = root.path("model").asText(requestedModel);
        int inputTokens = root.path("usage").path("input_tokens").asInt(0);
        int outputTokens = root.path("usage").path("output_tokens").asInt(0);
        String stopReason = root.path("stop_reason").asText("end_turn");

        return new AgentLlmResponse(
                content,
                returnedModel,
                inputTokens,
                outputTokens,
                ModelPricing.computeCostUsd(returnedModel, inputTokens, outputTokens),
                stopReason,
                null
        );
    }
}
