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
    private Set<TourSchedulePriceResponse> prices = new HashSet<>();
}
