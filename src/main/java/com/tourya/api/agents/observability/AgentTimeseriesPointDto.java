package com.tourya.api.agents.observability;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * IA-11: punto de la serie temporal — un bucket (dia/semana/mes) + agente.
 *
 * <p>El bucket sale de {@code date_trunc(:granularity, created_at)} sobre
 * {@code agent_audit_log}. El campo {@code date} es la fecha del bucket
 * (sin hora), para graficar directamente.</p>
 */
public record AgentTimeseriesPointDto(
        LocalDate date,
        String agent,
        long calls,
        BigDecimal costUsd,
        Long avgLatencyMs
) {}
