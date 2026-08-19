package com.tourya.api.agents.backoffice;

import lombok.Builder;

import java.util.List;

/**
 * IA-08: pre-validacion de un tour antes de {@code acceptTourById}. La
 * mayoria de las verificaciones son deterministicas (RN-011: espanol
 * obligatorio, RN-013: galeria horizontal, politica de cancelacion definida).
 * El LLM solo aporta el {@code reasoning} en lenguaje natural para el ADMIN.
 *
 * @param tourId            Id del {@link com.tourya.api.models.Tour} evaluado.
 * @param canApprove        {@code true} si no hay issues CRITICAL — sugerencia,
 *                          nunca el veredicto final (RN-010/RN-046: ADMIN aprueba).
 * @param issues            Lista de incumplimientos (vacia = tour limpio).
 * @param reasoning         Resumen del agente para el ADMIN (opcional).
 * @param escalatedToHuman  {@code true} cuando el LLM fallo — el agente
 *                          igual devuelve la parte deterministica.
 */
@Builder
public record TourPrevalidationResponse(
        Integer tourId,
        boolean canApprove,
        List<TourIssueItem> issues,
        String reasoning,
        boolean escalatedToHuman
) {}
