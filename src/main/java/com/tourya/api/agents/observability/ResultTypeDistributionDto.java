package com.tourya.api.agents.observability;

/**
 * IA-11: distribucion de {@code result_type} por agente en el rango.
 *
 * <p>Los valores validos hoy en el esquema son
 * {@code suggestion | autonomous_action | rejected | error} (ver
 * {@code database/migrations/076_agent_audit_log.sql}). El campo
 * {@code success} agrupa {@code suggestion + autonomous_action} — la
 * distincion no aporta al panel de salud pero se mantiene rescatable en
 * futuros endpoints de drill-down.</p>
 */
public record ResultTypeDistributionDto(
        String agent,
        long success,
        long error,
        long rejected
) {}
