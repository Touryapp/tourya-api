package com.tourya.api.agents.shared;

import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.repository.AgentAuditLogRepository;
import com.tourya.api.services.AppConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * IA-01: guard de presupuesto por agente. Si el gasto mensual acumulado
 * supera el cap configurado en {@code app_config}, el guard rechaza la
 * ejecución — implementa el Principio rector #6 (doc 16).
 *
 * <p>Los caps se leen de {@code app_config} con key
 * {@code AGENT_BUDGET_{AGENTNAME}_USD_MONTHLY}. Defaults conservadores
 * (ver constantes). ADMIN los ajusta via {@code PUT /config/{key}}.</p>
 *
 * <p>El "mes" corre en calendario Bogotá (primer día del mes actual, 00:00).
 * Aceptable para reporting operativo — no requiere alineación fiscal exacta.</p>
 *
 * <p>Sin cache: cada {@link #canRun} consulta la BD (una suma agregada). Es
 * un query barato (índice {@code idx_agent_audit_log_agent_date} cubre el
 * predicate) y evita cache stale que podría dejar correr al agente pasado
 * el tope.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetGuard {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    /** Defaults conservadores (USD/mes). ADMIN sube estos vía app_config. */
    private static final Map<String, Integer> DEFAULT_MONTHLY_CAP = Map.of(
            "TravelConcierge", 30,
            "Support24x7", 30,
            "DesertCart", 10,
            "OperatorSupport", 20,
            "BackofficeSupport", 20,
            "ReviewModerator", 10
    );

    private static final Map<String, ConfigKeyEnum> CONFIG_KEY_BY_AGENT = Map.of(
            "TravelConcierge", ConfigKeyEnum.AGENT_BUDGET_TRAVELCONCIERGE_USD_MONTHLY,
            "Support24x7", ConfigKeyEnum.AGENT_BUDGET_SUPPORT24X7_USD_MONTHLY,
            "DesertCart", ConfigKeyEnum.AGENT_BUDGET_DESERTCART_USD_MONTHLY,
            "OperatorSupport", ConfigKeyEnum.AGENT_BUDGET_OPERATORSUPPORT_USD_MONTHLY,
            "BackofficeSupport", ConfigKeyEnum.AGENT_BUDGET_BACKOFFICESUPPORT_USD_MONTHLY,
            "ReviewModerator", ConfigKeyEnum.AGENT_BUDGET_REVIEWMODERATOR_USD_MONTHLY
    );

    private final AgentAuditLogRepository auditRepository;
    private final AppConfigService appConfigService;

    /**
     * @return {@code true} si el agente puede ejecutar (aún hay presupuesto);
     *         {@code false} si el consumo del mes ya alcanzó/superó el cap.
     */
    public boolean canRun(String agentName) {
        BigDecimal cap = getMonthlyCapUsd(agentName);
        BigDecimal spent = getSpentThisMonthUsd(agentName);
        boolean allowed = spent.compareTo(cap) < 0;
        if (!allowed) {
            log.warn("IA-01 budget exhausted agent={} spent={} cap={}",
                    agentName, spent, cap);
        }
        return allowed;
    }

    /** Gasto USD acumulado del agente desde el primer día del mes actual (Bogotá). */
    public BigDecimal getSpentThisMonthUsd(String agentName) {
        OffsetDateTime monthStart = OffsetDateTime.now(BOGOTA)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        // El repository no tiene @Query custom para SUM(cost_usd) — para v1
        // hacemos count. Los agentes actuales no cargan volumen; cuando lo
        // hagan, se agrega un @Query SUM al repository. TODO IA-01b.
        // Por ahora aproximamos: si count > cap/costoPromedioLLM, exhausted.
        // Aproximación conservadora: asumimos costo promedio $0.01 por call
        // (Haiku 4.5 con prompts cortos), un cap de $30 = 3000 calls.
        long callsThisMonth = auditRepository.countByAgentNameAndCreatedAtAfter(agentName, monthStart);
        return BigDecimal.valueOf(callsThisMonth).multiply(new BigDecimal("0.01"));
    }

    /** Cap mensual USD del agente (app_config con fallback al default). */
    public BigDecimal getMonthlyCapUsd(String agentName) {
        ConfigKeyEnum key = CONFIG_KEY_BY_AGENT.get(agentName);
        int defaultCap = DEFAULT_MONTHLY_CAP.getOrDefault(agentName, 10);
        if (key == null) return BigDecimal.valueOf(defaultCap);
        int capInt = appConfigService.getInt(key, defaultCap);
        return BigDecimal.valueOf(capInt);
    }
}
