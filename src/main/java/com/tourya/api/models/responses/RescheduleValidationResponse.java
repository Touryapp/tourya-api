package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response para validación de re-agendamiento de reserva.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RescheduleValidationResponse {
    
    private Boolean canReschedule;
    private String message;
    private String reason; // Razón por la cual no se puede reagendar (si aplica)
}

