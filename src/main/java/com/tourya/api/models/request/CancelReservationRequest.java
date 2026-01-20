package com.tourya.api.models.request;

import com.tourya.api.constans.enums.CancellationReasonEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para cancelar una reserva.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelReservationRequest {
    
    @NotNull(message = "El motivo de cancelación es obligatorio")
    private CancellationReasonEnum cancellationReason;
}

