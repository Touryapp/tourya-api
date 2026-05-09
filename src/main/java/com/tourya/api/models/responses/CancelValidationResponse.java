package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response para validación de cancelación de reserva (solo validaciones, sin cambios).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CancelValidationResponse {
    private Boolean canCancel;
    private String message;
    private String reason;
}

