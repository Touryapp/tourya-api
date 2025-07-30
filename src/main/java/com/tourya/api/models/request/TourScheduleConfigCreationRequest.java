package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class TourScheduleConfigCreationRequest {
    private Integer id; // Agregado para permitir actualizaciones (identificar la configuración existente)
    @NotNull(message = "El ID del tour no puede ser nulo")
    private Integer tourId; // ID del tour al que se aplica esta configuración
    // Label puede ser nulo según tu tabla, pero se puede añadir @NotBlank si se desea
    private String label;
    @NotNull(message = "La fecha de inicio de la configuración no puede ser nula")
    private LocalDate startDate;
    @NotNull(message = "La fecha de fin de la configuración no puede ser nula")
    private LocalDate endDate;
    @NotEmpty(message = "La lista de días de la semana no puede estar vacía")
    private List<String> daysOfWeek; // Días de la semana como Strings (ej. "MONDAY", "TUESDAY")
    // isUnlimitedCapacity tiene un default en DB, no requiere @NotNull aquí
    private Boolean isUnlimitedCapacity;
    @Valid // Habilita la validación anidada de los slots
    @NotEmpty(message = "La configuración debe tener al menos un slot de horario")
    private Set<TourScheduleConfigSlotDto> slots = new HashSet<>(); // CAMBIO: De List a Set, inicialización
}
