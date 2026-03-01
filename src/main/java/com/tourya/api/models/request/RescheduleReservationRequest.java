package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request para re-agendar una reserva con nueva fecha y cantidades.
 * Permite cambiar la fecha, las cantidades por ageType y opcionalmente el slotId.
 * Si no se envía slotId, el backend buscará automáticamente el slot equivalente por horario.
 * 
 * @author Tourya API Team
 * @version 3.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RescheduleReservationRequest {
    
    @NotNull(message = "La nueva fecha es obligatoria")
    private LocalDate newDate;
    
    @NotNull(message = "La configuración de cantidad por ageType es requerida")
    @Valid
    private List<ConfigQuantityRequest> configQuantity;
    
    /**
     * ID del slot en el nuevo schedule (opcional).
     * Si no se envía, el backend buscará automáticamente el slot equivalente por horario.
     * Útil cuando diferentes fechas tienen configuraciones diferentes con slots diferentes.
     */
    private Long slotId;
}

