package com.tourya.api.agents.observability;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * IA-11: implementacion JDBC del repositorio de observabilidad.
 *
 * <p>Todos los queries son <b>read-only</b> y agrupados sobre
 * {@code agent_audit_log}. Los indices existentes (creados en la migracion
 * {@code 076_agent_audit_log.sql}) cubren los patrones de acceso:</p>
 * <ul>
 *   <li>{@code idx_agent_audit_log_agent_date} sobre {@code (agent_name,
 *       created_at DESC)} — soporta {@code summary}, {@code timeseries},
 *       {@code latency}, {@code resultTypes}, {@code overrides}.</li>
 *   <li>{@code idx_agent_audit_log_user_date} sobre {@code (user_id,
 *       created_at DESC) WHERE user_id IS NOT NULL} — soporta
 *       {@code topConsumers}.</li>
 *   <li>{@code idx_agent_audit_log_metadata} GIN sobre {@code metadata}
 *       — soporta filtros {@code metadata @> '{...}'} y {@code ->>}.</li>
 * </ul>
 *
 * <p>No se agrega ninguna migracion nueva — los indices ya existen.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AgentObservabilityRepositoryImpl implements AgentObservabilityRepository {

    /** Whitelist para {@code date_trunc(...)} — nunca aceptar valor libre. */
    private static final Set<String> ALLOWED_GRANULARITIES = Set.of("day", "week", "month");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<AgentSummaryDto> summary(OffsetDateTime from, OffsetDateTime to, String agent) {
        String sql = """
                SELECT
                    agent_name,
                    COUNT(*)                                                                         AS total_calls,
                    COALESCE(SUM(input_tokens), 0)                                                   AS tokens_in,
                    COALESCE(SUM(output_tokens), 0)                                                  AS tokens_out,
                    COALESCE(SUM(cost_usd), 0)                                                       AS cost_usd,
                    COALESCE(ROUND(AVG(duration_ms)::numeric, 0), 0)                                 AS avg_latency_ms,
                    ROUND(100.0 * COUNT(*) FILTER (
                        WHERE result_type IN ('suggestion', 'autonomous_action')
                    ) / NULLIF(COUNT(*), 0), 2)                                                      AS success_rate,
                    ROUND(100.0 * COUNT(*) FILTER (
                        WHERE (metadata->>'escalated_to_human')::boolean IS TRUE
                    ) / NULLIF(COUNT(*), 0), 2)                                                      AS escalated_rate
                FROM agent_audit_log
                WHERE created_at BETWEEN ? AND ?
                  AND (? IS NULL OR agent_name = ?)
                GROUP BY agent_name
                ORDER BY total_calls DESC
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new AgentSummaryDto(
                        rs.getString("agent_name"),
                        rs.getLong("total_calls"),
                        rs.getLong("tokens_in"),
                        rs.getLong("tokens_out"),
                        nullSafeBigDecimal(rs.getBigDecimal("cost_usd")),
                        rs.getObject("avg_latency_ms") == null ? null : rs.getLong("avg_latency_ms"),
                        nullSafeBigDecimal(rs.getBigDecimal("success_rate")),
                        nullSafeBigDecimal(rs.getBigDecimal("escalated_rate"))
                ),
                Timestamp.from(from.toInstant()),
                Timestamp.from(to.toInstant()),
                agent, agent
        );
    }

    @Override
    public List<AgentTimeseriesPointDto> timeseries(OffsetDateTime from,
                                                    OffsetDateTime to,
                                                    String agent,
                                                    String granularity) {
        String safeGranularity = safeGranularity(granularity);
        // date_trunc no acepta bind param para la unidad — se whitelist arriba.
        String sql = ("""
                SELECT
                    date_trunc('%s', created_at)::date               AS bucket,
                    agent_name,
                    COUNT(*)                                          AS calls,
                    COALESCE(SUM(cost_usd), 0)                        AS cost_usd,
                    COALESCE(ROUND(AVG(duration_ms)::numeric, 0), 0)  AS avg_latency_ms
                FROM agent_audit_log
                WHERE created_at BETWEEN ? AND ?
                  AND (? IS NULL OR agent_name = ?)
                GROUP BY bucket, agent_name
                ORDER BY bucket ASC, agent_name ASC
                """).formatted(safeGranularity);
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new AgentTimeseriesPointDto(
                        toLocalDate(rs.getDate("bucket")),
                        rs.getString("agent_name"),
                        rs.getLong("calls"),
                        nullSafeBigDecimal(rs.getBigDecimal("cost_usd")),
                        rs.getObject("avg_latency_ms") == null ? null : rs.getLong("avg_latency_ms")
                ),
                Timestamp.from(from.toInstant()),
                Timestamp.from(to.toInstant()),
                agent, agent
        );
    }

    @Override
    public List<LatencyDistributionDto> latency(OffsetDateTime from,
                                                OffsetDateTime to,
                                                String agent,
                                                String capability) {
        boolean withCapability = capability != null && !capability.isBlank();
        String selectCapability = withCapability
                ? "metadata->>'capability' AS capability,"
                : "NULL::text                AS capability,";
        String groupBy = withCapability
                ? "GROUP BY agent_name, metadata->>'capability'"
                : "GROUP BY agent_name";
        String extraFilter = withCapability
                ? "AND metadata->>'capability' = ?"
                : "";
        String sql = ("""
                SELECT
                    agent_name,
                    %s
                    ROUND(percentile_cont(0.5)  WITHIN GROUP (ORDER BY duration_ms))::bigint AS p50,
                    ROUND(percentile_cont(0.95) WITHIN GROUP (ORDER BY duration_ms))::bigint AS p95,
                    ROUND(percentile_cont(0.99) WITHIN GROUP (ORDER BY duration_ms))::bigint AS p99,
                    MIN(duration_ms)                                                          AS min_ms,
                    MAX(duration_ms)                                                          AS max_ms
                FROM agent_audit_log
                WHERE created_at BETWEEN ? AND ?
                  AND (? IS NULL OR agent_name = ?)
                  %s
                %s
                ORDER BY agent_name ASC
                """).formatted(selectCapability, extraFilter, groupBy);

        Object[] params = withCapability
                ? new Object[]{
                        Timestamp.from(from.toInstant()),
                        Timestamp.from(to.toInstant()),
                        agent, agent, capability
                }
                : new Object[]{
                        Timestamp.from(from.toInstant()),
                        Timestamp.from(to.toInstant()),
                        agent, agent
                };

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new LatencyDistributionDto(
                        rs.getString("agent_name"),
                        rs.getString("capability"),
                        nullSafeLong(rs, "p50"),
                        nullSafeLong(rs, "p95"),
                        nullSafeLong(rs, "p99"),
                        nullSafeLong(rs, "min_ms"),
                        nullSafeLong(rs, "max_ms")
                ),
                params
        );
    }

    @Override
    public List<TopConsumerDto> topConsumers(OffsetDateTime from,
                                             OffsetDateTime to,
                                             String agent,
                                             int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 100));
        // LEFT JOIN a _user (tabla real, ver com.tourya.api.models.User) para el email.
        // user_id puede ser NULL (calls anonimos del Concierge) → GROUP BY lo respeta.
        String sql = """
                SELECT
                    l.user_id,
                    u.email                       AS user_email,
                    COUNT(*)                       AS calls,
                    COALESCE(SUM(l.cost_usd), 0)  AS cost_usd
                FROM agent_audit_log l
                LEFT JOIN _user u ON u.id = l.user_id
                WHERE l.created_at BETWEEN ? AND ?
                  AND (? IS NULL OR l.agent_name = ?)
                GROUP BY l.user_id, u.email
                ORDER BY calls DESC, cost_usd DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    int uid = rs.getInt("user_id");
                    boolean uidWasNull = rs.wasNull();
                    return new TopConsumerDto(
                            uidWasNull ? null : uid,
                            rs.getString("user_email"),
                            rs.getLong("calls"),
                            nullSafeBigDecimal(rs.getBigDecimal("cost_usd"))
                    );
                },
                Timestamp.from(from.toInstant()),
                Timestamp.from(to.toInstant()),
                agent, agent,
                effectiveLimit
        );
    }

    @Override
    public List<ResultTypeDistributionDto> resultTypes(OffsetDateTime from, OffsetDateTime to) {
        String sql = """
                SELECT
                    agent_name,
                    COUNT(*) FILTER (WHERE result_type IN ('suggestion', 'autonomous_action')) AS success,
                    COUNT(*) FILTER (WHERE result_type = 'error')                              AS error,
                    COUNT(*) FILTER (WHERE result_type = 'rejected')                           AS rejected
                FROM agent_audit_log
                WHERE created_at BETWEEN ? AND ?
                GROUP BY agent_name
                ORDER BY agent_name ASC
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ResultTypeDistributionDto(
                        rs.getString("agent_name"),
                        rs.getLong("success"),
                        rs.getLong("error"),
                        rs.getLong("rejected")
                ),
                Timestamp.from(from.toInstant()),
                Timestamp.from(to.toInstant())
        );
    }

    @Override
    public List<OverrideMetricsDto> overrides(OffsetDateTime from,
                                              OffsetDateTime to,
                                              String agent) {
        // Agrupa por agent_name + capability (metadata->>'capability' — solo
        // OperatorSupport lo persiste hoy; para TravelConcierge quedara NULL).
        // MVP (Opcion A): overrideRateProxy = escalatedRate + errorRate.
        String sql = """
                SELECT
                    agent_name,
                    metadata->>'capability'                                                     AS capability,
                    COUNT(*)                                                                     AS total,
                    COUNT(*) FILTER (
                        WHERE (metadata->>'escalated_to_human')::boolean IS TRUE
                    )                                                                             AS escalated,
                    COUNT(*) FILTER (WHERE result_type = 'error')                                 AS errored,
                    ROUND(100.0 * COUNT(*) FILTER (
                        WHERE (metadata->>'escalated_to_human')::boolean IS TRUE
                    ) / NULLIF(COUNT(*), 0), 2)                                                   AS escalated_rate,
                    ROUND(100.0 * COUNT(*) FILTER (WHERE result_type = 'error')
                        / NULLIF(COUNT(*), 0), 2)                                                 AS error_rate,
                    ROUND(100.0 * (
                        COUNT(*) FILTER (WHERE (metadata->>'escalated_to_human')::boolean IS TRUE)
                        + COUNT(*) FILTER (WHERE result_type = 'error')
                    ) / NULLIF(COUNT(*), 0), 2)                                                   AS override_rate_proxy
                FROM agent_audit_log
                WHERE created_at BETWEEN ? AND ?
                  AND (? IS NULL OR agent_name = ?)
                GROUP BY agent_name, metadata->>'capability'
                ORDER BY agent_name ASC, capability ASC NULLS FIRST
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new OverrideMetricsDto(
                        rs.getString("agent_name"),
                        rs.getString("capability"),
                        rs.getLong("total"),
                        rs.getLong("escalated"),
                        rs.getLong("errored"),
                        nullSafeBigDecimal(rs.getBigDecimal("escalated_rate")),
                        nullSafeBigDecimal(rs.getBigDecimal("error_rate")),
                        nullSafeBigDecimal(rs.getBigDecimal("override_rate_proxy"))
                ),
                Timestamp.from(from.toInstant()),
                Timestamp.from(to.toInstant()),
                agent, agent
        );
    }

    private static String safeGranularity(String granularity) {
        if (granularity == null) {
            return "day";
        }
        String normalized = granularity.trim().toLowerCase();
        return ALLOWED_GRANULARITIES.contains(normalized) ? normalized : "day";
    }

    private static java.time.LocalDate toLocalDate(Date sqlDate) {
        return sqlDate == null ? null : sqlDate.toLocalDate();
    }

    private static BigDecimal nullSafeBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static Long nullSafeLong(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }
}
