package com.tourya.api.repository;

import com.tourya.api.models.AgentAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * IA-00: repositorio de audit log de agentes.
 *
 * <p>Los queries expuestos cubren los patrones de acceso previstos por el
 * dashboard IA-11 y por consultas forenses:</p>
 * <ul>
 *   <li>{@link #findByAgentNameOrderByCreatedAtDesc}: dashboards por agente.</li>
 *   <li>{@link #findByEntityTypeAndEntityIdOrderByCreatedAtDesc}: "¿qué le
 *       pasó a la reserva 123?".</li>
 *   <li>{@link #findByUserIdOrderByCreatedAtDesc}: historial de agente por
 *       usuario humano involucrado.</li>
 *   <li>{@link #countByAgentNameAndCreatedAtAfter}: métricas de uso mensual
 *       para el {@code BudgetGuard} (IA-01).</li>
 * </ul>
 */
public interface AgentAuditLogRepository extends JpaRepository<AgentAuditLog, Long> {

    Page<AgentAuditLog> findByAgentNameOrderByCreatedAtDesc(String agentName, Pageable pageable);

    List<AgentAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    Page<AgentAuditLog> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    long countByAgentNameAndCreatedAtAfter(String agentName, OffsetDateTime since);

    /**
     * v24 (issue #39 Luis 2026-10-06): recupera los últimos N turns de la
     * conversación de un sessionId dado (misma {@code metadata->>'session_id'})
     * para reinyectarlos como "historial" al prompt del Concierge. Antes el
     * agente perdía el contexto en cada POST y repetía las mismas preguntas.
     *
     * <p>Native query porque usamos el operador {@code ->>} de JSONB (Spring
     * Data no lo expone en JPQL). Orden descendente por fecha; el caller
     * invierte si necesita orden cronológico.</p>
     */
    @Query(
            value = "SELECT * FROM agent_audit_log "
                    + "WHERE agent_name = :agentName "
                    + "AND metadata->>'session_id' = :sessionId "
                    + "AND result_type IN ('success','partial') "
                    + "ORDER BY created_at DESC LIMIT :limit",
            nativeQuery = true
    )
    List<AgentAuditLog> findRecentBySessionId(
            @Param("agentName") String agentName,
            @Param("sessionId") String sessionId,
            @Param("limit") int limit
    );
}
