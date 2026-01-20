package com.tourya.api.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request para re-agendar una reserva.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RescheduleReservationRequest {
    
    @NotNull(message = "La nueva fecha es obligatoria")
    private LocalDate newDate;
}

