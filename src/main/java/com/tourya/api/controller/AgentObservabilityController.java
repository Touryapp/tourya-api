package com.tourya.api.controller;

import com.tourya.api.agents.observability.AgentObservabilityService;
import com.tourya.api.agents.observability.AgentSummaryDto;
import com.tourya.api.agents.observability.AgentTimeseriesPointDto;
import com.tourya.api.agents.observability.LatencyDistributionDto;
import com.tourya.api.agents.observability.OverrideMetricsDto;
import com.tourya.api.agents.observability.ResultTypeDistributionDto;
import com.tourya.api.agents.observability.TopConsumerDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * IA-11: endpoints admin del dashboard de observabilidad de agentes IA.
 *
 * <p>Todos requieren JWT + rol {@code ADMIN} o {@code BACKOFFICE_OPERATION}
 * — el guard vive en el service ({@link AgentObservabilityService}, mismo
 * patron que {@code CreditService.findAllForAdmin} / TC-022 #253). Rol
 * insuficiente → {@link com.tourya.api.exceptions.InsufficientPrivilegesException}
 * que el {@code GlobalExceptionHandler} traduce a 401.</p>
 *
 * <p>Los queries son agregaciones caras; se agrega {@code Cache-Control:
 * private, max-age=60} a las respuestas para que refresh masivo del
 * frontend no golpee la BD en cada tick.</p>
 */
@Slf4j
@RestController
@RequestMapping("/admin/agents")
@RequiredArgsConstructor
@Tag(
        name = "Admin Agents Observability",
        description = "Dashboard de observabilidad de agentes IA (IA-11). "
                + "Solo ADMIN o BACKOFFICE_OPERATION."
)
public class AgentObservabilityController {

    private static final CacheControl CACHE_60S = CacheControl
            .maxAge(Duration.ofSeconds(60))
            .cachePrivate();

    private final AgentObservabilityService service;

    @GetMapping("/summary")
    @Operation(
            operationId = "adminAgentsSummary",
            summary = "Resumen por agente (IA-11)",
            description = "Una fila por agente en el rango: totalCalls, tokens in/out, cost_usd, "
                    + "avg_latency_ms, success_rate, escalated_rate. Rango default: ultimos 30 dias."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumen retornado"),
            @ApiResponse(responseCode = "401", description = "Falta rol ADMIN o BACKOFFICE_OPERATION")
    })
    public ResponseEntity<List<AgentSummaryDto>> summary(
            Authentication authentication,
            @Parameter(description = "Desde (YYYY-MM-DD, UTC, inclusive). Default: hoy - 30d.")
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Hasta (YYYY-MM-DD, UTC, inclusive). Default: hoy.")
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Filtrar por nombre de agente: TravelConcierge | OperatorSupport | ...")
            @RequestParam(value = "agent", required = false) String agent
    ) {
        return ResponseEntity.ok()
                .cacheControl(CACHE_60S)
                .body(service.summary(authentication, from, to, agent));
    }

    @GetMapping("/timeseries")
    @Operation(
            operationId = "adminAgentsTimeseries",
            summary = "Serie temporal por bucket + agente (IA-11)",
            description = "Puntos (fecha, agente, calls, cost_usd, avg_latency_ms) agrupados por "
                    + "date_trunc(granularity, created_at). Granularities validos: day|week|month "
                    + "(default day)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Serie retornada"),
            @ApiResponse(responseCode = "401", description = "Falta rol ADMIN o BACKOFFICE_OPERATION")
    })
    public ResponseEntity<List<AgentTimeseriesPointDto>> timeseries(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "agent", required = false) String agent,
            @Parameter(description = "day | week | month (default day). Valores no whitelisted → day.")
            @RequestParam(value = "granularity", required = false) String granularity
    ) {
        return ResponseEntity.ok()
                .cacheControl(CACHE_60S)
                .body(service.timeseries(authentication, from, to, agent, granularity));
    }

    @GetMapping("/latency")
    @Operation(
            operationId = "adminAgentsLatency",
            summary = "Percentiles de latencia por agente (IA-11)",
            description = "p50/p95/p99/min/max de duration_ms via percentile_cont. "
                    + "Con capability se agrupa ademas por metadata->>'capability' "
                    + "(hoy solo OperatorSupport la persiste)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Distribucion retornada"),
            @ApiResponse(responseCode = "401", description = "Falta rol ADMIN o BACKOFFICE_OPERATION")
    })
    public ResponseEntity<List<LatencyDistributionDto>> latency(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "agent", required = false) String agent,
            @Parameter(description = "Capability del agente (solo OperatorSupport: suggest_tour_content | price_alert | draft_review_reply).")
            @RequestParam(value = "capability", required = false) String capability
    ) {
        return ResponseEntity.ok()
                .cacheControl(CACHE_60S)
                .body(service.latency(authentication, from, to, agent, capability));
    }

    @GetMapping("/top-consumers")
    @Operation(
            operationId = "adminAgentsTopConsumers",
            summary = "Top-N usuarios por call count (IA-11)",
            description = "Ordenado por calls DESC (empate por costo DESC). Limit default 10, max 100. "
                    + "user_id/user_email pueden venir null si el call fue anonimo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Top consumers retornado"),
            @ApiResponse(responseCode = "401", description = "Falta rol ADMIN o BACKOFFICE_OPERATION")
    })
    public ResponseEntity<List<TopConsumerDto>> topConsumers(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "agent", required = false) String agent,
            @Parameter(description = "Limit (default 10, max 100).")
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok()
                .cacheControl(CACHE_60S)
                .body(service.topConsumers(authentication, from, to, agent, limit));
    }

    @GetMapping("/result-types")
    @Operation(
            operationId = "adminAgentsResultTypes",
            summary = "Distribucion success/error/rejected por agente (IA-11)",
            description = "success agrupa suggestion + autonomous_action. Sin filtro por agente — "
                    + "el consumer decide si graficar todos o filtrar en el frontend."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Distribucion retornada"),
            @ApiResponse(responseCode = "401", description = "Falta rol ADMIN o BACKOFFICE_OPERATION")
    })
    public ResponseEntity<List<ResultTypeDistributionDto>> resultTypes(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok()
                .cacheControl(CACHE_60S)
                .body(service.resultTypes(authentication, from, to));
    }

    @GetMapping("/overrides")
    @Operation(
            operationId = "adminAgentsOverrides",
            summary = "Override rate proxy por agente/capability (IA-11, Opcion A MVP)",
            description = "MVP sin tracking real de humanOutcome: overrideRateProxy = "
                    + "escalatedRate + errorRate. Deuda IA-11b: agregar columna human_outcome "
                    + "(KEPT|EDITED|DISCARDED) reportada por el frontend para computar el override "
                    + "real."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metricas retornadas"),
            @ApiResponse(responseCode = "401", description = "Falta rol ADMIN o BACKOFFICE_OPERATION")
    })
    public ResponseEntity<List<OverrideMetricsDto>> overrides(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "agent", required = false) String agent
    ) {
        return ResponseEntity.ok()
                .cacheControl(CACHE_60S)
                .body(service.overrides(authentication, from, to, agent));
    }
}
