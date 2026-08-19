package com.tourya.api.agents.backoffice;

import lombok.Builder;

import java.util.List;

/**
 * IA-08: item de checklist KYB — un renglon por documento obligatorio (RN-045).
 *
 * @param documentType  Nombre del tipo de documento (RUT, RNT, POLIZAS_VIGENTES, etc.).
 * @param present       {@code true} si el operador subio al menos una imagen para el tipo.
 * @param issues        Observaciones puntuales encontradas (vacia = ninguna).
 * @param status        OK | WARN | CRITICAL — el ADMIN decide en base a esto.
 */
@Builder
public record KybChecklistItem(
        String documentType,
        boolean present,
        List<String> issues,
        String status
) {
    public static final String STATUS_OK = "OK";
    public static final String STATUS_WARN = "WARN";
    public static final String STATUS_CRITICAL = "CRITICAL";
}
