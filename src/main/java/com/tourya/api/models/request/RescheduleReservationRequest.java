package com.tourya.api.models.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Request para re-agendar una reserva con nueva fecha y cantidades.
 * Permite cambiar la fecha, las cantidades por ageType y opcionalmente el slotId / horas.
 * Si no se envía slotId o no existe en la nueva config, el backend buscará el slot equivalente por horario.
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

    /**
     * Hora de inicio/fin del slot esperado (opcional).
     * Si se envía, el backend usará (startTime,endTime) como fallback cuando el slotId no exista en la nueva config.
     * Formato esperado: HH:mm:ss (ej: 09:00:00).
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
}

