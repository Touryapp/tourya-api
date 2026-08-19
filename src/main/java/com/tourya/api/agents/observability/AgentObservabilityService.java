package com.tourya.api.agents.observability;

import com.tourya.api._utils.Utils;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * IA-11: orquestador del dashboard de observabilidad de agentes IA.
 *
 * <p>Solo agrega numeros — <b>nunca</b> devuelve prompts crudos ni el
 * {@code metadata} completo de {@code agent_audit_log}. Los DTOs solo
 * exponen agregaciones (calls, tokens, costo, latencia, ratios).</p>
 *
 * <p>Autorizacion: solo ADMIN o BACKOFFICE_OPERATION — mismo patron que
 * {@link com.tourya.api.services.CreditService#findAllForAdmin}. Cualquier
 * otro rol dispara {@link InsufficientPrivilegesException} (401 via el
 * {@code GlobalExceptionHandler}).</p>
 *
 * <p>Rango default: ultimos 30 dias cuando el caller no especifica
 * {@code from}/{@code to}. Todas las fechas se interpretan en UTC —
 * consistente con {@code created_at TIMESTAMPTZ} en la tabla.</p>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AgentObservabilityService {

    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int DEFAULT_TOP_CONSUMERS_LIMIT = 10;
    private static final Set<String> ALLOWED_GRANULARITIES = Set.of("day", "week", "month");

    private final AgentObservabilityRepository repository;

    public List<AgentSummaryDto> summary(Authentication authentication,
                                         LocalDate from,
                                         LocalDate to,
                                         String agent) {
        requireBackofficeRole(authentication);
        OffsetDateTime[] range = resolveRange(from, to);
        String normalizedAgent = blankToNull(agent);
        return repository.summary(range[0], range[1], normalizedAgent);
    }

    public List<AgentTimeseriesPointDto> timeseries(Authentication authentication,
                                                    LocalDate from,
                                                    LocalDate to,
                                                    String agent,
                                                    String granularity) {
        requireBackofficeRole(authentication);
        OffsetDateTime[] range = resolveRange(from, to);
        String normalizedAgent = blankToNull(agent);
        String safeGranularity = normalizeGranularity(granularity);
        return repository.timeseries(range[0], range[1], normalizedAgent, safeGranularity);
    }

    public List<LatencyDistributionDto> latency(Authentication authentication,
                                                LocalDate from,
                                                LocalDate to,
                                                String agent,
                                                String capability) {
        requireBackofficeRole(authentication);
        OffsetDateTime[] range = resolveRange(from, to);
        return repository.latency(
                range[0], range[1],
                blankToNull(agent),
                blankToNull(capability)
        );
    }

    public List<TopConsumerDto> topConsumers(Authentication authentication,
                                             LocalDate from,
                                             LocalDate to,
                                             String agent,
                                             Integer limit) {
        requireBackofficeRole(authentication);
        OffsetDateTime[] range = resolveRange(from, to);
        int effectiveLimit = (limit == null || limit <= 0)
                ? DEFAULT_TOP_CONSUMERS_LIMIT
                : Math.min(limit, 100);
        return repository.topConsumers(range[0], range[1], blankToNull(agent), effectiveLimit);
    }

    public List<ResultTypeDistributionDto> resultTypes(Authentication authentication,
                                                       LocalDate from,
                                                       LocalDate to) {
        requireBackofficeRole(authentication);
        OffsetDateTime[] range = resolveRange(from, to);
        return repository.resultTypes(range[0], range[1]);
    }

    public List<OverrideMetricsDto> overrides(Authentication authentication,
                                              LocalDate from,
                                              LocalDate to,
                                              String agent) {
        requireBackofficeRole(authentication);
        OffsetDateTime[] range = resolveRange(from, to);
        return repository.overrides(range[0], range[1], blankToNull(agent));
    }

    // ------------------------------------------------------------------
    // Helpers internos
    // ------------------------------------------------------------------

    private void requireBackofficeRole(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
        if (!Utils.isTouryaBackoffice(user.getRoles())) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }

    private OffsetDateTime[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = (to != null) ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate effectiveFrom = (from != null) ? from : effectiveTo.minusDays(DEFAULT_RANGE_DAYS);
        if (effectiveFrom.isAfter(effectiveTo)) {
            LocalDate swap = effectiveFrom;
            effectiveFrom = effectiveTo;
            effectiveTo = swap;
        }
        OffsetDateTime fromTs = effectiveFrom.atStartOfDay().atOffset(ZoneOffset.UTC);
        // "to" inclusivo — sumamos un dia menos un instante para incluir el
        // dia completo. Se hace en la capa de servicio para que el repo se
        // limite a WHERE created_at BETWEEN ? AND ?.
        OffsetDateTime toTs = effectiveTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);
        return new OffsetDateTime[]{fromTs, toTs};
    }

    private String normalizeGranularity(String granularity) {
        if (granularity == null || granularity.isBlank()) {
            return "day";
        }
        String normalized = granularity.trim().toLowerCase();
        return ALLOWED_GRANULARITIES.contains(normalized) ? normalized : "day";
    }

    private String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
