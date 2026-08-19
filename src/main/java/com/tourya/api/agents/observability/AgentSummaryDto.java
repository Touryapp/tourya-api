package com.tourya.api.agents.observability;

import java.math.BigDecimal;

/**
 * IA-11: agregado por agente para el dashboard de observabilidad.
 *
 * <p>Una fila = un agente en el rango consultado. Todos los agregados salen
 * de {@code agent_audit_log} vía {@link AgentObservabilityRepository}.</p>
 *
 * <p>Nunca expone prompts, ni el {@code metadata} completo — solo campos
 * derivados. {@code escalatedRate} se computa desde {@code metadata->>'escalated_to_human'}.</p>
 */
public record AgentSummaryDto(
        String agent,
        long totalCalls,
        long tokensIn,
        long tokensOut,
        BigDecimal costUsd,
        Long avgLatencyMs,
        BigDecimal successRate,
        BigDecimal escalatedRate
) {}
