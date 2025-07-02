package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class SearchTourScheduleResponse {
    private Integer scheduleId;
    private Integer tourId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isUnlimitedCapacity;
    private Integer maxCapacity;
    private Integer reservedCapacity;
    private String status;
}