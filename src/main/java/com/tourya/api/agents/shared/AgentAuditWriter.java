package com.tourya.api.agents.shared;

import com.tourya.api.models.AgentAuditLog;
import com.tourya.api.repository.AgentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * IA-01: writer del audit log de agentes. Es la ÚNICA vía por la que un
 * agente escribe en {@code agent_audit_log} — obliga al patrón "auditar
 * siempre" del Principio rector #4 (doc 16).
 *
 * <p>Métodos {@link Async} — no bloquean el flujo del agente. Fallos de
 * persistencia se loguean pero no propagan: el agente ya hizo su trabajo
 * de negocio, un fallo del audit no debe revertirlo.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentAuditWriter {

    private final AgentAuditLogRepository repository;

    @Async
    public void register(AgentAuditEntry entry) {
        try {
            AgentAuditLog log = AgentAuditLog.builder()
                    .agentName(entry.agentName())
                    .model(entry.model())
                    .promptVersion(entry.promptVersion())
                    .inputTokens(entry.inputTokens())
                    .outputTokens(entry.outputTokens())
                    .costUsd(entry.costUsd())
                    .durationMs(entry.durationMs())
                    .entityType(entry.entityType())
                    .entityId(entry.entityId())
                    .userId(entry.userId())
                    .resultType(entry.resultType())
                    .resultJson(entry.resultJson())
                    .promptInput(entry.promptInput())
                    .errorMessage(entry.errorMessage())
                    .metadata(entry.metadata())
                    .createdAt(OffsetDateTime.now())
                    .build();
            repository.save(log);
        } catch (Exception ex) {
            // Nunca propagamos: el agente ya actuó (o rechazó), un fallo del
            // audit es un incidente ops pero no debe romper el flow.
            log.error("IA-01 audit write failed for agent={} model={}",
                    entry.agentName(), entry.model(), ex);
        }
    }
}
