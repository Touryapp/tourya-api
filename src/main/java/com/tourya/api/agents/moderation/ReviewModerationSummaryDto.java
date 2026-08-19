package com.tourya.api.agents.moderation;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * IA-10: resumen ligero de una reseña moderada para el listado del backoffice
 * ({@code GET /admin/reviews/moderation}).
 *
 * <p>{@code reviewText} solo trae los primeros 200 chars para no cargar el
 * listado — si el operador quiere el texto completo abre el detalle via el
 * endpoint {@code GET /reviews} existente.</p>
 */
@Builder
public record ReviewModerationSummaryDto(
        Long reviewId,
        Integer tourId,
        String tourName,
        String moderationStatus,
        List<String> flags,
        LocalDateTime moderatedAt,
        String reviewText,
        String authorEmail
) {}
