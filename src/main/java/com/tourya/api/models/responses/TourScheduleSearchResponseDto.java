package com.tourya.api.models.responses;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TourScheduleSearchResponseDto {
    private Integer scheduleId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxCapacity; // Capacidad máxima del slot
    private Integer reservedCapacity;
    private Integer availableCapacity; // Calculado: maxCapacity - reservedCapacity
    private Boolean isUnlimitedCapacity;
    private String status;
    private Integer configId;

    private TourDetailsInSearchDto tourDetails;
    private List<TourPriceOptionDto> priceOptions = new ArrayList<>();
    private TourLocationInSearchDto locationDetails;
}
