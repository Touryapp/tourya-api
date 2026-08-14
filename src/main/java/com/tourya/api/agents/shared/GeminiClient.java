package com.tourya.api.agents.shared;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * IA-02: implementación del cliente Vertex AI Gemini (Google Cloud).
 *
 * <p>Franklin decidió Vertex AI en vez de Anthropic (2026-08-14) por 3 razones:
 * (1) reusa infra GCP existente (SA {@code tourya-dev-cloud-run} ya con
 * {@code roles/aiplatform.user}); (2) ~40% mas barato para el mismo volumen;
 * (3) el framework {@link ILlmClient} ya es provider-agnostic, cero rework.</p>
 *
 * <p><b>Auth:</b> Application Default Credentials (ADC). En Cloud Run usa la
 * service account automaticamente; en local dev requiere
 * {@code gcloud auth application-default login} (Franklin ya lo hizo).</p>
 *
 * <p><b>Modo no-op degradado:</b> si {@code agents.gemini.enabled=false} o el
 * SDK falla al inicializar (credenciales ausentes, endpoint no disponible)
 * devuelve {@link AgentLlmResponse#disabled()} sin lanzar excepcion. Los
 * agentes registran {@code result_type=rejected} en {@code agent_audit_log}.</p>
 *
 * <p><b>Manejo de errores:</b> nunca throws. Igual que {@link AnthropicClient}
 * las fallas devuelven {@link AgentLlmResponse#error(String)} — decision
 * intencional para que el flujo del agente no se rompa por el LLM.</p>
 */
@Slf4j
@Component
public class GeminiClient implements ILlmClient {

    private static final String DISABLED_REASON = "Vertex AI Gemini disabled (agents.gemini.enabled=false)";

    private final boolean enabled;
    private final String projectId;
    private final String location;

    /** Lazy — evita costo de inicializacion en tests y arranque cuando el flag esta OFF. */
    private volatile VertexAI vertexAi;

    public GeminiClient(
            @Value("${agents.gemini.enabled:true}") boolean enabled,
            @Value("${agents.gemini.project-id:tourya-project-dev}") String projectId,
            @Value("${agents.gemini.location:us-central1}") String location) {
        this.enabled = enabled;
        this.projectId = projectId;
        this.location = location;
    }

    @Override
    public AgentLlmResponse complete(String prompt, String model) {
        if (!enabled) {
            log.debug("IA-02 Gemini disabled by config");
            return AgentLlmResponse.disabled();
        }
        if (prompt == null || prompt.isBlank()) {
            return AgentLlmResponse.error("empty_prompt");
        }
        if (model == null || model.isBlank()) {
            return AgentLlmResponse.error("empty_model");
        }

        try {
            VertexAI client = getOrInitVertexAi();
            if (client == null) {
                return AgentLlmResponse.disabled();
            }
            GenerativeModel generativeModel = new GenerativeModel(model, client);
            GenerateContentResponse response = generativeModel.generateContent(prompt);
            return parseResponse(response, model);
        } catch (Exception ex) {
            log.error("IA-02 Gemini call failed model={}", model, ex);
            return AgentLlmResponse.error("vertex_ai_error: " + safeMessage(ex));
        }
    }

    /**
     * Convierte la respuesta de Vertex AI en {@link AgentLlmResponse} con
     * costo pre-calculado via {@link ModelPricing}. Los conteos vienen en
     * {@code usageMetadata}: {@code promptTokenCount} + {@code candidatesTokenCount}.
     */
    private AgentLlmResponse parseResponse(GenerateContentResponse response, String requestedModel) {
        String content;
        try {
            content = ResponseHandler.getText(response);
        } catch (Exception ex) {
            log.warn("IA-02 Gemini response has no text (finish reason may block)", ex);
            content = "";
        }
        int inputTokens = 0;
        int outputTokens = 0;
        try {
            inputTokens = response.getUsageMetadata().getPromptTokenCount();
            outputTokens = response.getUsageMetadata().getCandidatesTokenCount();
        } catch (Exception ignored) {
            // Sin usage metadata dejamos 0 — el costo saldra 0. No es un error fatal.
        }
        String finishReason = "end_turn";
        try {
            if (response.getCandidatesCount() > 0) {
                var candidate = response.getCandidates(0);
                if (candidate.getFinishReason() != null) {
                    finishReason = candidate.getFinishReason().name().toLowerCase();
                }
            }
        } catch (Exception ignored) {}

        return new AgentLlmResponse(
                content,
                requestedModel,
                inputTokens,
                outputTokens,
                ModelPricing.computeCostUsd(requestedModel, inputTokens, outputTokens),
                finishReason,
                null
        );
    }

    /**
     * Inicializa el cliente Vertex AI la primera vez que se usa (thread-safe
     * via double-checked locking). Si falla — típicamente ADC no configurado —
     * loguea y devuelve {@code null} para caer a modo disabled sin romper.
     */
    private VertexAI getOrInitVertexAi() {
        VertexAI local = this.vertexAi;
        if (local == null) {
            synchronized (this) {
                local = this.vertexAi;
                if (local == null) {
                    try {
                        local = new VertexAI(projectId, location);
                        this.vertexAi = local;
                        log.info("IA-02 Gemini VertexAI initialized project={} location={}", projectId, location);
                    } catch (Exception ex) {
                        log.error("IA-02 Gemini VertexAI init failed — falling back to disabled mode", ex);
                        return null;
                    }
                }
            }
        }
        return local;
    }

    @PreDestroy
    void shutdown() {
        VertexAI local = this.vertexAi;
        if (local != null) {
            try {
                local.close();
            } catch (Exception ex) {
                log.warn("IA-02 Gemini VertexAI close failed", ex);
            }
        }
    }

    private static String safeMessage(Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null) return ex.getClass().getSimpleName();
        // Truncamos a 200 chars para no ensuciar el audit log con stack completo.
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}
