package com.tourya.api.models.responses;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TourScheduleBulkResponse {
    private Integer tourId;
    private LocalDate scheduleDate;
    private TourScheduleConfigResponse config;
}

