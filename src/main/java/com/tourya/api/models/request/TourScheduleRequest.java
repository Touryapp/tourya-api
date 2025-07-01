package com.tourya.api.models.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TourScheduleRequest {

    @NotNull(message = "Schedule date is mandatory")
    @FutureOrPresent(message = "Schedule date cannot be in the past")
    private LocalDate scheduleDate; // Corresponde a 'schedule_date' en la tabla

    @NotNull(message = "Start time is mandatory")
    private LocalTime startTime; // Corresponde a 'start_time' en la tabla

    @NotNull(message = "End time is mandatory")
    private LocalTime endTime; // Corresponde a 'end_time' en la tabla

    @Min(value = 0, message = "Max capacity cannot be negative")
    @Max(value = 9999, message = "Max capacity exceeds reasonable limits") // Ejemplo de límite superior
    private Integer maxCapacity; // Corresponde a 'max_capacity' en la tabla (puede ser null)

    // reservedCapacity no se envía en el request, lo gestiona el backend

    private Boolean isUnlimitedCapacity; // Corresponde a 'is_unlimited_capacity' en la tabla

    // status no se envía en el request, tiene un valor por defecto en la DB y el backend lo gestiona

    // configId: Si este schedule se genera a partir de una configuración, se podría enviar aquí.
    // Si no está directamente vinculado a una config específica al crear, puedes omitirlo o hacerlo opcional.
    private Integer configId; // Corresponde a 'config_id' en la tabla (puede ser null)
}
