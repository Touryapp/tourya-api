package com.tourya.api.agents.shared;

import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.repository.AgentAuditLogRepository;
import com.tourya.api.services.AppConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * IA-01: unit tests para {@link BudgetGuard}. Mockea el repositorio + app_config.
 *
 * <p>La lógica actual (v1) aproxima el spent como {@code calls * $0.01} por
 * simplicidad — no hay {@code SUM(cost_usd)} en el repository todavía. Los
 * tests validan esa aproximación explícita; si cambia a SUM real (IA-01b),
 * los tests se actualizan.</p>
 */
@ExtendWith(MockitoExtension.class)
class BudgetGuardTest {

    @Mock
    private AgentAuditLogRepository auditRepository;

    @Mock
    private AppConfigService appConfigService;

    @InjectMocks
    private BudgetGuard guard;

    // El stub de app_config se coloca en cada test que lo necesita (Mockito
    // strict rechaza stubbings sin uso — los tests de "unknown agent" no
    // consultan appConfigService porque el mapping no tiene la key).

    @Test
    @DisplayName("canRun devuelve true cuando el consumo esta bajo el cap")
    void canRun_belowCap_true() {
        when(appConfigService.getInt(eq(ConfigKeyEnum.AGENT_BUDGET_TRAVELCONCIERGE_USD_MONTHLY), anyInt()))
                .thenReturn(30);
        when(auditRepository.countByAgentNameAndCreatedAtAfter(eq("TravelConcierge"), any(OffsetDateTime.class)))
                .thenReturn(100L); // 100 * 0.01 = $1 << $30

        assertTrue(guard.canRun("TravelConcierge"));
    }

    @Test
    @DisplayName("canRun devuelve false cuando el consumo alcanza el cap")
    void canRun_atCap_false() {
        when(appConfigService.getInt(eq(ConfigKeyEnum.AGENT_BUDGET_TRAVELCONCIERGE_USD_MONTHLY), anyInt()))
                .thenReturn(30);
        when(auditRepository.countByAgentNameAndCreatedAtAfter(eq("TravelConcierge"), any(OffsetDateTime.class)))
                .thenReturn(3000L); // 3000 * 0.01 = $30 == cap → NO puede correr

        assertFalse(guard.canRun("TravelConcierge"));
    }

    @Test
    @DisplayName("canRun devuelve false cuando el consumo supera el cap")
    void canRun_aboveCap_false() {
        when(appConfigService.getInt(eq(ConfigKeyEnum.AGENT_BUDGET_TRAVELCONCIERGE_USD_MONTHLY), anyInt()))
                .thenReturn(30);
        when(auditRepository.countByAgentNameAndCreatedAtAfter(eq("TravelConcierge"), any(OffsetDateTime.class)))
                .thenReturn(5000L); // $50 > $30

        assertFalse(guard.canRun("TravelConcierge"));
    }

    @Test
    @DisplayName("getSpentThisMonthUsd usa aproximacion count * $0.01 (v1)")
    void getSpent_v1Approximation() {
        when(auditRepository.countByAgentNameAndCreatedAtAfter(anyString(), any(OffsetDateTime.class)))
                .thenReturn(250L);

        BigDecimal spent = guard.getSpentThisMonthUsd("TravelConcierge");
        assertEquals(0, new BigDecimal("2.50").compareTo(spent),
                "250 calls * $0.01 = $2.50");
    }

    @Test
    @DisplayName("getMonthlyCapUsd usa el default si el agente no esta en el mapping")
    void getMonthlyCapUsd_unknownAgent_usesDefault() {
        BigDecimal cap = guard.getMonthlyCapUsd("UnknownAgent");
        assertEquals(0, new BigDecimal("10").compareTo(cap),
                "agente no listado usa fallback $10");
    }

    @Test
    @DisplayName("canRun para agente desconocido usa el default $10 y aplica igual")
    void canRun_unknownAgent_usesDefault() {
        when(auditRepository.countByAgentNameAndCreatedAtAfter(eq("UnknownAgent"), any(OffsetDateTime.class)))
                .thenReturn(500L); // $5 < $10

        assertTrue(guard.canRun("UnknownAgent"));
    }
}
