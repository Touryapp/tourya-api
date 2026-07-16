package com.tourya.api.agents.shared;

import java.math.BigDecimal;

/**
 * IA-01: respuesta normalizada del LLM tras un call {@link ILlmClient#complete}.
 * Incluye el contenido generado + metadata de tokens + costo pre-calculado
 * (ver {@link ModelPricing}). Los agentes NO recalculan costo — confían en este record.
 *
 * @param content       Texto generado por el modelo (única choice, único content block).
 * @param model         ID del modelo usado (idéntico al parámetro del call — para audit).
 * @param inputTokens   Tokens del prompt + system.
 * @param outputTokens  Tokens del contenido generado.
 * @param costUsd       Costo total del call ({@link ModelPricing#computeCostUsd}).
 * @param finishReason  end_turn | max_tokens | stop_sequence | tool_use | error | disabled
 * @param error         Populated cuando finishReason=error o disabled; null en éxito.
 */
public record AgentLlmResponse(
        String content,
        String model,
        int inputTokens,
        int outputTokens,
        BigDecimal costUsd,
        String finishReason,
        String error
) {
    public boolean isError() {
        return "error".equals(finishReason) || "disabled".equals(finishReason);
    }

    /**
     * Placeholder cuando el proveedor LLM no está configurado (API key ausente).
     * Permite que los agentes corran en dev/CI sin secretos — pero rechazan con
     * result_type = "rejected" o "error" en el audit log.
     */
    public static AgentLlmResponse disabled() {
        return new AgentLlmResponse(
                "",
                "unknown",
                0,
                0,
                BigDecimal.ZERO,
                "disabled",
                "ANTHROPIC_API_KEY not configured"
        );
    }

    public static AgentLlmResponse error(String message) {
        return new AgentLlmResponse(
                "",
                "unknown",
                0,
                0,
                BigDecimal.ZERO,
                "error",
                message
        );
    }
}
