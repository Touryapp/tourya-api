package com.tourya.api.constans.enums;

/**
 * Enum que define los motivos de cancelación de una reserva.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
public enum CancellationReasonEnum {
    /**
     * No podrá disfrutar el tour
     */
    CANNOT_ATTEND,

    /**
     * Enfermedades
     */
    ILLNESS,

    /**
     * Imposibilidad de viajar
     */
    INABILITY_TO_TRAVEL,

    /**
     * Cancelación por lluvia (DIMAR). Solo vía {@code PUT /reservations/{id}/cancel/rain}.
     */
    RAIN,

    /**
     * Obligaciones legales del turista (BE-05, aprobado Luis 2026-07-07, RN-030).
     */
    LEGAL_OBLIGATIONS,

    /**
     * Cambio de planes del turista (BE-05, aprobado Luis 2026-07-07, RN-030).
     */
    CHANGE_OF_PLANS
}


