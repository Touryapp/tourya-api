package com.tourya.api.agents.backoffice;

import lombok.Builder;

/**
 * IA-08: incumplimiento puntual detectado en la pre-validacion de un tour.
 *
 * @param severity  CRITICAL (bloquea approve) | WARN (avisa al ADMIN) | INFO (nota).
 * @param code      Codigo estable para la UI (MISSING_ES, GALLERY_VERTICAL, ...).
 * @param field     Campo/entidad afectada (description, gallery, cancellationPolicy, ...).
 * @param message   Descripcion humana en espanol lista para mostrar al ADMIN.
 */
@Builder
public record TourIssueItem(
        String severity,
        String code,
        String field,
        String message
) {
    public static final String SEVERITY_CRITICAL = "CRITICAL";
    public static final String SEVERITY_WARN = "WARN";
    public static final String SEVERITY_INFO = "INFO";
}
