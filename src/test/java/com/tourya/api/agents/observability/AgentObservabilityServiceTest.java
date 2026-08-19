package com.tourya.api.agents.observability;

import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.models.Role;
import com.tourya.api.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * IA-11: unit tests para {@link AgentObservabilityService}.
 *
 * <p>Se mockea el repositorio ({@link AgentObservabilityRepository}) y solo
 * se valida:</p>
 * <ol>
 *   <li>Guard de rol ADMIN / BACKOFFICE_OPERATION — cualquier otro rol
 *       lanza {@link InsufficientPrivilegesException} <b>sin</b> consultar
 *       la BD.</li>
 *   <li>Default de rango (ultimos 30 dias) cuando el caller no pasa
 *       {@code from}/{@code to}.</li>
 *   <li>Pass-through de filtros ({@code agent}, {@code capability},
 *       {@code granularity} whitelisted, {@code limit}).</li>
 *   <li>Normalizacion de granularity: valores no whitelisted → {@code day}.</li>
 *   <li>Normalizacion de agent blank → {@code null} para que el repo aplique
 *       "sin filtro".</li>
 * </ol>
 *
 * <p>Los queries nativos se testean end-to-end en QA con datos reales — este
 * test no toca Postgres.</p>
 */
@ExtendWith(MockitoExtension.class)
class AgentObservabilityServiceTest {

    @Mock
    private AgentObservabilityRepository repository;

    @InjectMocks
    private AgentObservabilityService service;

    private Authentication adminAuth;
    private Authentication backofficeOpAuth;
    private Authentication touristAuth;

    @BeforeEach
    void setUp() {
        adminAuth = authFor("admin@tourya.co", "ADMIN");
        backofficeOpAuth = authFor("bo@tourya.co", "BACKOFFICE_OPERATION");
        touristAuth = authFor("tourist@tourya.co", "USER");
    }

    // ------------------------------------------------------------------
    // Guard de rol
    // ------------------------------------------------------------------

    @Test
    @DisplayName("summary lanza InsufficientPrivileges cuando el usuario no es ADMIN ni BACKOFFICE_OPERATION")
    void summary_nonBackofficeRole_throws() {
        assertThrows(
                InsufficientPrivilegesException.class,
                () -> service.summary(touristAuth, null, null, null)
        );
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("summary lanza InsufficientPrivileges cuando la Authentication es null")
    void summary_nullAuth_throws() {
        assertThrows(
                InsufficientPrivilegesException.class,
                () -> service.summary(null, null, null, null)
        );
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("summary acepta rol BACKOFFICE_OPERATION igual que ADMIN")
    void summary_backofficeOperation_accepted() {
        when(repository.summary(any(OffsetDateTime.class), any(OffsetDateTime.class), eq(null)))
                .thenReturn(sampleSummary());

        List<AgentSummaryDto> out = service.summary(backofficeOpAuth, null, null, null);

        assertNotNull(out);
        assertEquals(2, out.size());
        verify(repository).summary(any(OffsetDateTime.class), any(OffsetDateTime.class), eq(null));
    }

    // ------------------------------------------------------------------
    // summary
    // ------------------------------------------------------------------

    @Test
    @DisplayName("summary usa rango default de 30 dias cuando from/to son null")
    void summary_defaultRange_last30Days() {
        when(repository.summary(any(OffsetDateTime.class), any(OffsetDateTime.class), eq(null)))
                .thenReturn(List.of());

        service.summary(adminAuth, null, null, null);

        ArgumentCaptor<OffsetDateTime> fromCap = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCap = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).summary(fromCap.capture(), toCap.capture(), eq(null));

        long days = java.time.Duration.between(fromCap.getValue(), toCap.getValue()).toDays();
        // 30 dias exactos - 1 nanosec (por el "to inclusive" del service).
        // .toDays() trunca hacia abajo → aceptamos 29 o 30.
        assertTrue(days == 29 || days == 30,
                "El rango default debe ser ~30 dias (fue " + days + ")");
    }

    @Test
    @DisplayName("summary con agent=TravelConcierge pasa el filtro al repo")
    void summary_filtersBySpecificAgent_whenAgentParamProvided() {
        when(repository.summary(any(OffsetDateTime.class), any(OffsetDateTime.class), eq("TravelConcierge")))
                .thenReturn(List.of(new AgentSummaryDto(
                        "TravelConcierge", 3L, 100L, 50L,
                        new BigDecimal("0.05"), 800L,
                        new BigDecimal("100.00"), new BigDecimal("0.00")
                )));

        List<AgentSummaryDto> out = service.summary(
                adminAuth,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 15),
                "TravelConcierge"
        );

        assertEquals(1, out.size());
        assertEquals("TravelConcierge", out.get(0).agent());
        verify(repository).summary(any(OffsetDateTime.class), any(OffsetDateTime.class), eq("TravelConcierge"));
    }

    @Test
    @DisplayName("summary retorna lista vacia cuando el repo no tiene datos en el rango")
    void summary_returnsEmptyList_whenNoDataInRange() {
        when(repository.summary(any(OffsetDateTime.class), any(OffsetDateTime.class), eq(null)))
                .thenReturn(List.of());

        List<AgentSummaryDto> out = service.summary(adminAuth, null, null, null);

        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    @Test
    @DisplayName("summary trata agent blank ('   ') como sin filtro")
    void summary_blankAgent_treatedAsNull() {
        when(repository.summary(any(OffsetDateTime.class), any(OffsetDateTime.class), eq(null)))
                .thenReturn(List.of());

        service.summary(adminAuth, null, null, "   ");

        verify(repository).summary(any(OffsetDateTime.class), any(OffsetDateTime.class), eq(null));
    }

    // ------------------------------------------------------------------
    // timeseries
    // ------------------------------------------------------------------

    @Test
    @DisplayName("timeseries usa granularity=day por default")
    void timeseries_returnsDailyBuckets_whenGranularityDay() {
        List<AgentTimeseriesPointDto> expected = List.of(
                new AgentTimeseriesPointDto(
                        LocalDate.of(2026, 8, 10),
                        "TravelConcierge", 5L,
                        new BigDecimal("0.02"), 900L
                )
        );
        when(repository.timeseries(
                any(OffsetDateTime.class), any(OffsetDateTime.class),
                eq(null), eq("day"))
        ).thenReturn(expected);

        List<AgentTimeseriesPointDto> out = service.timeseries(adminAuth, null, null, null, null);

        assertSame(expected, out);
        verify(repository).timeseries(any(OffsetDateTime.class), any(OffsetDateTime.class),
                eq(null), eq("day"));
    }

    @Test
    @DisplayName("timeseries normaliza granularity invalida ('hour') a 'day'")
    void timeseries_invalidGranularity_fallsBackToDay() {
        when(repository.timeseries(
                any(OffsetDateTime.class), any(OffsetDateTime.class),
                eq(null), eq("day"))
        ).thenReturn(List.of());

        service.timeseries(adminAuth, null, null, null, "hour");

        verify(repository).timeseries(any(OffsetDateTime.class), any(OffsetDateTime.class),
                eq(null), eq("day"));
    }

    // ------------------------------------------------------------------
    // latency
    // ------------------------------------------------------------------

    @Test
    @DisplayName("latency retorna p50/p95/p99 para el agente pasado")
    void latency_returnsP50P95P99_forAgent() {
        LatencyDistributionDto row = new LatencyDistributionDto(
                "TravelConcierge", null,
                800L, 3500L, 5200L, 250L, 8100L
        );
        when(repository.latency(
                any(OffsetDateTime.class), any(OffsetDateTime.class),
                eq("TravelConcierge"), eq(null))
        ).thenReturn(List.of(row));

        List<LatencyDistributionDto> out = service.latency(
                adminAuth, null, null, "TravelConcierge", null
        );

        assertEquals(1, out.size());
        assertEquals(800L, out.get(0).p50());
        assertEquals(3500L, out.get(0).p95());
        assertEquals(5200L, out.get(0).p99());
    }

    // ------------------------------------------------------------------
    // topConsumers
    // ------------------------------------------------------------------

    @Test
    @DisplayName("topConsumers respeta el limit y pasa el filtro de agente al repo")
    void topConsumers_ordersByCallCountDesc_withLimit() {
        List<TopConsumerDto> repoRows = new ArrayList<>();
        repoRows.add(new TopConsumerDto(101, "alice@tourya.co", 42L, new BigDecimal("0.10")));
        repoRows.add(new TopConsumerDto(102, "bob@tourya.co", 30L, new BigDecimal("0.08")));
        when(repository.topConsumers(
                any(OffsetDateTime.class), any(OffsetDateTime.class),
                eq("OperatorSupport"), eq(5))
        ).thenReturn(repoRows);

        List<TopConsumerDto> out = service.topConsumers(
                adminAuth, null, null, "OperatorSupport", 5
        );

        assertEquals(2, out.size());
        assertEquals(42L, out.get(0).calls());
        // el orden lo respeta el repo (ORDER BY calls DESC); el service solo pasa la lista.
        assertTrue(out.get(0).calls() >= out.get(1).calls());
    }

    @Test
    @DisplayName("topConsumers usa limit=10 cuando el caller pasa null")
    void topConsumers_nullLimit_uses10() {
        when(repository.topConsumers(
                any(OffsetDateTime.class), any(OffsetDateTime.class),
                eq(null), eq(10))
        ).thenReturn(List.of());

        service.topConsumers(adminAuth, null, null, null, null);

        verify(repository).topConsumers(any(OffsetDateTime.class), any(OffsetDateTime.class),
                eq(null), eq(10));
    }

    // ------------------------------------------------------------------
    // resultTypes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("resultTypes retorna la distribucion 1 fila por agente")
    void resultTypes_returnsDistributionPerAgent() {
        List<ResultTypeDistributionDto> repoRows = List.of(
                new ResultTypeDistributionDto("OperatorSupport", 45L, 2L, 1L),
                new ResultTypeDistributionDto("TravelConcierge", 100L, 5L, 8L)
        );
        when(repository.resultTypes(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(repoRows);

        List<ResultTypeDistributionDto> out = service.resultTypes(adminAuth, null, null);

        assertEquals(2, out.size());
        assertEquals(45L, out.get(0).success());
        assertEquals(8L, out.get(1).rejected());
    }

    // ------------------------------------------------------------------
    // overrides
    // ------------------------------------------------------------------

    @Test
    @DisplayName("overrides retorna escalatedRate + errorRate como proxy (Opcion A)")
    void overrides_returnsEscalatedRateAsProxy_optionA() {
        OverrideMetricsDto proxyRow = new OverrideMetricsDto(
                "TravelConcierge", null,
                100L, 12L, 3L,
                new BigDecimal("12.00"),
                new BigDecimal("3.00"),
                new BigDecimal("15.00")
        );
        when(repository.overrides(
                any(OffsetDateTime.class), any(OffsetDateTime.class),
                eq("TravelConcierge"))
        ).thenReturn(List.of(proxyRow));

        List<OverrideMetricsDto> out = service.overrides(
                adminAuth, null, null, "TravelConcierge"
        );

        assertEquals(1, out.size());
        OverrideMetricsDto only = out.get(0);
        assertEquals(0, new BigDecimal("15.00").compareTo(only.overrideRateProxy()),
                "overrideRateProxy = escalatedRate + errorRate en el MVP");
        assertEquals(12L, only.escalated());
        assertEquals(3L, only.errored());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static Authentication authFor(String email, String... roleNames) {
        List<Role> roles = new ArrayList<>();
        for (String r : roleNames) {
            roles.add(Role.builder().name(r).build());
        }
        User user = User.builder()
                .id(999)
                .email(email)
                .roles(roles)
                .build();
        return new UsernamePasswordAuthenticationToken(user, null);
    }

    private static List<AgentSummaryDto> sampleSummary() {
        return List.of(
                new AgentSummaryDto(
                        "OperatorSupport", 20L, 1500L, 300L,
                        new BigDecimal("0.35"), 1800L,
                        new BigDecimal("95.00"), new BigDecimal("5.00")
                ),
                new AgentSummaryDto(
                        "TravelConcierge", 10L, 800L, 200L,
                        new BigDecimal("0.10"), 900L,
                        new BigDecimal("90.00"), new BigDecimal("10.00")
                )
        );
    }
}
