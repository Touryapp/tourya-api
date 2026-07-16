package com.tourya.api.agents.shared;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * IA-01: entrada para el {@link AgentAuditWriter}. DTO separado del entity
 * JPA {@code AgentAuditLog} para que los agentes no dependan de la capa de
 * persistencia — solo del writer service.
 */
@Builder
public record AgentAuditEntry(
        String agentName,
        String model,
        String promptVersion,
        int inputTokens,
        int outputTokens,
        BigDecimal costUsd,
        long durationMs,
        String entityType,
        Long entityId,
        Integer userId,
        String resultType,
        String resultJson,
        String promptInput,
        String errorMessage
) {}
