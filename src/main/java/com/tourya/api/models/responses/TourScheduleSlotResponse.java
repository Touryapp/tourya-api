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
    private Integer capacity;
    private Integer bookings;
    private Integer availability;
    private Integer minCapacityCalc;
    private Boolean checkAvailability;
    private Set<TourSchedulePriceResponse> prices = new HashSet<>();
}
