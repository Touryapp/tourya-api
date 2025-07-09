package com.tourya.api.models.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SearchTourScheduleRequest {
    private Integer tourId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Integer page = 1;
    private Integer size = 10;
}