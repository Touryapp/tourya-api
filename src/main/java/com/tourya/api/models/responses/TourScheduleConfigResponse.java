package com.tourya.api.models.responses;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class TourScheduleConfigResponse {
    private Integer id;
    private Integer providerId;
    private String label;
    private List<String> daysOfWeek;
    private Set<TourScheduleSlotResponse> slots = new HashSet<>();
}
