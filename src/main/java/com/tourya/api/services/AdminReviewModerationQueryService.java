package com.tourya.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.agents.moderation.ReviewModerationSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * IA-10: query service dedicado al listado del backoffice
 * ({@code GET /admin/reviews/moderation}).
 *
 * <p>Se aisla de {@code ReviewService} para no cargarlo con logica admin — el
 * ReviewService ya tiene ~1000 lineas y el listado admin no reusa nada del
 * mapper enriquecido (solo campos ligeros). Mismo enfoque que
 * {@code OperatorSupportRepository} vs {@code TourRepository}.</p>
 *
 * <p>Autorizacion NO se hace aca — la valida el
 * {@link com.tourya.api.controller.AdminReviewModerationController} antes de
 * invocar cualquier metodo. Este service confia en que el caller ya paso el
 * guard.</p>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminReviewModerationQueryService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int PREVIEW_MAX_CHARS = 200;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Lee reseñas con {@code moderation_status IN (PENDING, REJECTED)} — el
     * default es {@code PENDING} porque APPROVED no aporta a la cola de
     * revision.
     */
    public List<ReviewModerationSummaryDto> listQueue(String statusRaw, LocalDate from,
                                                      LocalDate to, Integer limitRaw) {
        String status = normalizeStatus(statusRaw);
        int limit = clamp(limitRaw == null ? DEFAULT_LIMIT : limitRaw, 1, MAX_LIMIT);

        LocalDate effectiveTo = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_RANGE_DAYS);
        if (effectiveFrom.isAfter(effectiveTo)) {
            LocalDate swap = effectiveFrom;
            effectiveFrom = effectiveTo;
            effectiveTo = swap;
        }
        // moderated_at es TIMESTAMP (no TIMESTAMPTZ) — usamos LocalDateTime.
        LocalDateTime fromTs = effectiveFrom.atStartOfDay();
        LocalDateTime toTs = effectiveTo.plusDays(1).atStartOfDay().minusNanos(1);

        String sql = """
                SELECT r.id, r.tour_id, r.moderation_status, r.moderation_flags, r.moderated_at,
                       r.comment, u.email,
                       t.name AS tour_name_json
                FROM review r
                LEFT JOIN _user u ON u.id = r.user_id
                LEFT JOIN tour t ON t.id = r.tour_id
                WHERE r.moderation_status = ?
                  AND r.moderated_at BETWEEN ? AND ?
                ORDER BY r.moderated_at DESC
                LIMIT ?
                """;

        List<ReviewModerationSummaryDto> out = new ArrayList<>();
        jdbcTemplate.query(sql,
                ps -> {
                    ps.setString(1, status);
                    ps.setTimestamp(2, Timestamp.valueOf(fromTs));
                    ps.setTimestamp(3, Timestamp.valueOf(toTs));
                    ps.setInt(4, limit);
                },
                rs -> {
                    Long reviewId = rs.getLong("id");
                    int tourIdRaw = rs.getInt("tour_id");
                    Integer tourId = rs.wasNull() ? null : tourIdRaw;
                    String modStatus = rs.getString("moderation_status");
                    String flagsJson = rs.getString("moderation_flags");
                    Timestamp moderatedAtTs = rs.getTimestamp("moderated_at");
                    String commentJson = rs.getString("comment");
                    String authorEmail = rs.getString("email");
                    String tourNameJson = rs.getString("tour_name_json");

                    out.add(ReviewModerationSummaryDto.builder()
                            .reviewId(reviewId)
                            .tourId(tourId)
                            .tourName(extractEsField(tourNameJson))
                            .moderationStatus(modStatus)
                            .flags(parseFlagsJson(flagsJson))
                            .moderatedAt(moderatedAtTs == null ? null : moderatedAtTs.toLocalDateTime())
                            .reviewText(truncate(extractEsField(commentJson), PREVIEW_MAX_CHARS))
                            .authorEmail(authorEmail)
                            .build());
                });
        return out;
    }

    private static String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) return "PENDING";
        String up = raw.trim().toUpperCase(Locale.ROOT);
        if ("REJECTED".equals(up)) return "REJECTED";
        // Cualquier otro valor (incluido APPROVED, que NO aporta a la cola) cae a PENDING.
        return "PENDING";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<String> parseFlagsJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (!arr.isArray()) return List.of();
            List<String> out = new ArrayList<>(arr.size());
            arr.forEach(el -> {
                if (el != null && el.isTextual()) out.add(el.asText());
            });
            return out;
        } catch (Exception ex) {
            log.debug("IA-10 malformed moderation_flags JSON: {}", ex.getMessage());
            return List.of();
        }
    }

    /**
     * Los campos i18n del proyecto (comment / tour.name) son JSONB {es,en,pt};
     * el listado admin siempre muestra ES.
     */
    private String extractEsField(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode es = node.path("es");
            return es.isMissingNode() || es.isNull() ? null : es.asText();
        } catch (Exception ex) {
            // Si no es JSON (columnas simples texto legacy), devuelve tal cual.
            return json;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
