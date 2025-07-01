package com.tourya.api.models.responses;

import lombok.Data;

import java.time.LocalTime;

@Data
public class TourScheduleConfigSlotResponse {
    private Integer id;
    private Integer configId;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer minCapacity;
    private Integer maxCapacity;
}
