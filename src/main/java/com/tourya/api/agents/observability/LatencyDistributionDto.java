package com.tourya.api.agents.observability;

/**
 * IA-11: distribucion de latencia por agente (o agente + capability).
 *
 * <p>Los percentiles vienen de {@code percentile_cont(0.5/0.95/0.99)
 * WITHIN GROUP (ORDER BY duration_ms)}. {@code capability} es opcional —
 * cuando el filtro por capability aplica, se agrupa por
 * {@code metadata->>'capability'} (solo OperatorSupport lo persiste hoy);
 * cuando no aplica queda {@code null}.</p>
 */
public record LatencyDistributionDto(
        String agent,
        String capability,
        Long p50,
        Long p95,
        Long p99,
        Long min,
        Long max
) {}
