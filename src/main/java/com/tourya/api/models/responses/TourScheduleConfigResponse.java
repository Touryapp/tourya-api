package com.tourya.api.models.responses;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TourScheduleConfigResponse {
    private Integer id; // ID de la configuración de horario

    // tourId: Puedes incluir solo el ID del tour, o un DTO simplificado del tour si es necesario.
    // Aquí asumo que solo quieres el ID.
    private Integer tourId;

    private String label;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<String> daysOfWeek;

    private Boolean isUnlimitedCapacity;

}
