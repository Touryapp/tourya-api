package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
public class TourScheduleConfigSlotDto {
    private Integer id; // Agregado para permitir actualizaciones (identificar slots existentes)
    @NotNull(message = "La hora de inicio del slot no puede ser nula")
    private LocalTime startTime;
    @NotNull(message = "La hora de fin del slot no puede ser nula")
    private LocalTime endTime;
    @NotNull(message = "La capacidad del slot no puede ser nula")
    private Integer capacity;
    @Valid // Habilita la validación anidada de los precios
    @NotEmpty(message = "Cada slot debe tener al menos un precio asociado")
    private List<TourScheduleConfigPriceDto> prices; // Precios asociados a este slot
}
