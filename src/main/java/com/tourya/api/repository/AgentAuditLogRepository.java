package com.tourya.api.repository;

import com.tourya.api.models.AgentAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
