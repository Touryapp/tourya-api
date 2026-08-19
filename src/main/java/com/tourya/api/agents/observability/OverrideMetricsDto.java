package com.tourya.api.agents.observability;

import java.math.BigDecimal;

/**
 * IA-11: heuristica de "override rate" — proxy MVP (Opcion A).
 *
 * <p>Hoy no hay tracking explicito de "el humano edito/descarto la
 * sugerencia" en {@code agent_audit_log}. Este DTO cubre el MVP con
 * senales indirectas:</p>
 * <ul>
 *   <li>{@code escalatedRate} — cuantas filas tienen
 *       {@code metadata->>'escalated_to_human' = true}. Sube cuando un
 *       guardrail bloqueo la request (secretos, budget, error).</li>
 *   <li>{@code errorRate} — cuantas filas tienen
 *       {@code result_type = 'error'}.</li>
 *   <li>{@code overrideRateProxy} — {@code escalatedRate + errorRate} como
 *       aproximacion de "casos donde el agente no pudo cerrar solo".</li>
 * </ul>
 *
 * <p>Deuda IA-11b: implementar Opcion B — columna
 * {@code human_outcome VARCHAR(20) NULL} en {@code agent_audit_log} +
 * hook desde el frontend para reportar {@code KEPT|EDITED|DISCARDED}
 * cuando el provider guarda el tour, y as recomputar {@code overrideRate}
 * verdadero.</p>
 */
public record OverrideMetricsDto(
        String agent,
        String capability,
        long totalSuggestions,
        long escalated,
        long errored,
        BigDecimal escalatedRate,
        BigDecimal errorRate,
        BigDecimal overrideRateProxy
) {}
