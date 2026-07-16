package com.tourya.api.agents.shared;

/**
 * IA-01: resultado de una ejecución de agente. Cuatro variantes discretas:
 * <ul>
 *   <li><b>autonomousAction</b>: el agente ejecutó una acción de negocio
 *       (ej. canceló una reserva dentro de política).</li>
 *   <li><b>suggestion</b>: el agente propone algo que requiere confirmación
 *       humana (ej. reagendamiento con diferencia de precio a cobrar).</li>
 *   <li><b>rejected</b>: el agente decidió no actuar (budget agotado,
 *       fuera de política, falta contexto).</li>
 *   <li><b>error</b>: falló el call al LLM o el parsing del output.</li>
 * </ul>
 *
 * <p>El {@code payload} genérico permite que cada agente devuelva su tipo
 * específico (ReagendamientoResult, CartAction, etc.).</p>
 */
public record AgentRunResult<T>(
        Type type,
        T payload,
        String reason
) {

    public enum Type {
        AUTONOMOUS_ACTION,
        SUGGESTION,
        REJECTED,
        ERROR
    }

    public static <T> AgentRunResult<T> autonomousAction(T payload) {
        return new AgentRunResult<>(Type.AUTONOMOUS_ACTION, payload, null);
    }

    public static <T> AgentRunResult<T> suggestion(T payload) {
        return new AgentRunResult<>(Type.SUGGESTION, payload, null);
    }

    public static <T> AgentRunResult<T> rejected(String reason) {
        return new AgentRunResult<>(Type.REJECTED, null, reason);
    }

    public static <T> AgentRunResult<T> error(String reason) {
        return new AgentRunResult<>(Type.ERROR, null, reason);
    }

    public boolean isSuccess() {
        return type == Type.AUTONOMOUS_ACTION || type == Type.SUGGESTION;
    }

    /** Traduce el tipo al string del campo <code>agent_audit_log.result_type</code>. */
    public String toAuditResultType() {
        return switch (type) {
            case AUTONOMOUS_ACTION -> "autonomous_action";
            case SUGGESTION -> "suggestion";
            case REJECTED -> "rejected";
            case ERROR -> "error";
        };
    }
}
