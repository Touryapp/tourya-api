package com.tourya.api.models.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class TourScheduleConfigSlotRequest {
    @NotNull(message = "Config ID cannot be null")
    private Integer configId;

    @NotNull(message = "Start time cannot be null")
    private LocalTime startTime;

    @NotNull(message = "End time cannot be null")
    private LocalTime endTime;

    @Min(value = 0, message = "Minimum capacity must be at least 0")
    private Integer minCapacity;

    @Min(value = 0, message = "Maximum capacity must be at least 0")
    private Integer maxCapacity;

}
