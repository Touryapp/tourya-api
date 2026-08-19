package com.tourya.api.agents.moderation;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * IA-10: entrada estructurada para el prompt del Agente 6.
 *
 * <p>Se construye internamente en {@link ReviewModerationService#moderate(Long)}
 * a partir del {@code Review} + contexto de negocio (tour, provider). El agente
 * usa estos datos para calibrar cada flag (por ejemplo OFF_TOPIC requiere saber
 * de que va el tour; POTENTIAL_FRAUD ayuda saber la antiguedad de la cuenta).</p>
 */
@Builder
public record ModerationRequest(
        Long reviewId,
        String reviewText,
        BigDecimal rating,
        List<String> mediaUrls,
        String tourName,
        String tourSubcategory,
        String providerName,
        String authorEmail
) {}
