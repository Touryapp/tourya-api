package com.tourya.api.agents.observability;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * IA-11: repositorio dedicado al dashboard de observabilidad de agentes IA.
 *
 * <p>Separado del {@link com.tourya.api.repository.AgentAuditLogRepository}
 * (que es el writer/lookup usado por {@code AgentAuditWriter}) para no
 * mezclar concerns — este repo <b>solo</b> ejecuta agregaciones read-only
 * sobre {@code agent_audit_log} y no debe ser inyectado en los services de
 * agentes que escriben ah.</p>
 *
 * <p>Impl: {@link AgentObservabilityRepositoryImpl} con {@link
 * org.springframework.jdbc.core.JdbcTemplate} — mismo patron que
 * {@link com.tourya.api.repository.impl.ReservationNativeRepositoryImpl}.
 * Se prefiere JDBC directo sobre @Query native para evitar mapear a
 * {@code Object[]} y para poder usar libremente Postgres
 * {@code percentile_cont}, {@code date_trunc}, {@code FILTER (WHERE ...)}
 * y operadores JSONB (@code ->>, @>).</p>
 */
public interface AgentObservabilityRepository {

    /** 1 fila por agente en el rango; {@code agent} opcional para filtrar. */
    List<AgentSummaryDto> summary(OffsetDateTime from, OffsetDateTime to, String agent);

    /**
     * Serie temporal (calls, costo, latencia promedio) por bucket + agente.
     *
     * @param granularity uno de {@code day | week | month}. El caller lo
     *                    valida antes — este metodo asume el valor
     *                    saneado y lo interpola directo en el SQL para
     *                    {@code date_trunc}. Nunca debe llegar input
     *                    libre del cliente.
     */
    List<AgentTimeseriesPointDto> timeseries(OffsetDateTime from,
                                             OffsetDateTime to,
                                             String agent,
                                             String granularity);

    /**
     * Distribucion de latencia (p50/p95/p99/min/max) por agente. Si
     * {@code capability} viene no-null se hace GROUP BY agente + capability
     * y se filtra solo a filas con {@code metadata->>'capability'} igual;
     * si viene null se agrupa solo por agente.
     */
    List<LatencyDistributionDto> latency(OffsetDateTime from,
                                         OffsetDateTime to,
                                         String agent,
                                         String capability);

    /** Top-N usuarios por call count DESC (empate por costo DESC). */
    List<TopConsumerDto> topConsumers(OffsetDateTime from,
                                      OffsetDateTime to,
                                      String agent,
                                      int limit);

    /** Distribucion {@code success/error/rejected} por agente en el rango. */
    List<ResultTypeDistributionDto> resultTypes(OffsetDateTime from, OffsetDateTime to);

    /** Override rate proxy MVP (Opcion A) por agente y capability. */
    List<OverrideMetricsDto> overrides(OffsetDateTime from,
                                       OffsetDateTime to,
                                       String agent);
}
