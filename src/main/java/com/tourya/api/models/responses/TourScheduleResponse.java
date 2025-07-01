package com.tourya.api.models.responses;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TourScheduleResponse {
    private Integer id; // ID de la programación

    // tourId: Solo el ID del tour.
    private Integer tourId;

    private LocalDate scheduleDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer maxCapacity;

    private Integer reservedCapacity; // Incluir la capacidad reservada

    private Boolean isUnlimitedCapacity;

    private String status; // Incluir el status

    // configId: El ID de la configuración de horario a la que pertenece esta programación (si existe)
    private Integer configId;
}
