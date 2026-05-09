package com.tourya.api.models.request;

import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class TourScheduleConfigDto { // Cambio de nombre de clase
    private Integer id;
    private Integer tourId;
    private Integer providerId;
    private String label;
    // Fechas y días de la semana ahora son opcionales
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> daysOfWeek;
    private Boolean isTemplate;
    @Valid
    private Set<TourScheduleConfigSlotDto> slots = new HashSet<>();
}
