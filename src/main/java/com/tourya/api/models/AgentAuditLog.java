package com.tourya.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * IA-00: registro de auditoría de una llamada de un agente IA al LLM.
 *
 * <p>Cada agente (TravelConcierge, Support24x7, etc.) DEBE escribir un
 * registro aquí por cada interacción con el LLM — es el pattern estándar
 * para agentes que afectan transacciones reales
 * (ver <a href="../../../docs/16-agentes-ia.md">doc 16</a> Principio rector #4).</p>
 *
 * <p>Sin FK cross-domain a las entidades afectadas: si se borra una
 * reservation, el log de la interacción del agente sigue existiendo para
 * forense. La relación con la entidad se guarda en {@link #entityType} +
 * {@link #entityId} informativos.</p>
 *
 * <p>El {@code AgentAuditWriter} service que escribe acá se crea en IA-01.</p>
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "agent_audit_log")
public class AgentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** TravelConcierge | Support24x7 | DesertCart | OperatorSupport | ... */
    @Column(name = "agent_name", nullable = false, length = 50)
    private String agentName;

    /** claude-sonnet-5 | claude-haiku-4-5 | claude-opus-4-8 | ... */
    @Column(nullable = false, length = 50)
    private String model;

    /** v1 | v2 | ... — versión de la plantilla usada (para A/B testing). */
    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens;

    /** Costo total del call en USD (input+output). */
    @Column(name = "cost_usd", nullable = false, precision = 10, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    /** reservation | tour | review | user | credit | ... */
    @Column(name = "entity_type", length = 30)
    private String entityType;

    /** BIGINT cubre long (reservation, payment) e int (tour, user). */
    @Column(name = "entity_id")
    private Long entityId;

    /** Usuario humano involucrado (turista/provider/operator). */
    @Column(name = "user_id")
    private Integer userId;

    /** suggestion | autonomous_action | rejected | error */
    @Column(name = "result_type", nullable = false, length = 30)
    private String resultType;

    /**
     * Output estructurado del LLM. Se guarda como String JSON para no forzar
     * dependencia a jackson.databind en la capa entidad; el consumer parsea
     * cuando necesita.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", columnDefinition = "jsonb")
    private String resultJson;

    /** Prompt real enviado al LLM (útil para debug + replay). */
    @Column(name = "prompt_input", columnDefinition = "text")
    private String promptInput;

    /** Populated si {@code resultType = error}. */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /**
     * IA-02: metadata libre por agente (session_id, fraud_suspected,
     * actions_executed[], escalated_to_human, etc.). Cada agente define su
     * propio shape; forensia con {@code metadata->>'session_id'}. Nullable.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
