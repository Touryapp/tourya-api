package com.tourya.api.models.responses;

import lombok.Data;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class TourScheduleSlotResponse {
    private Integer id;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer minCapacity;
    private Integer maxCapacity;
    private Set<TourSchedulePriceResponse> prices = new HashSet<>();
}
