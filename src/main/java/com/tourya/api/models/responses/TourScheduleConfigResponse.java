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
    private Integer tourId;
    private Integer providerId;
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> daysOfWeek;
    private Boolean isUnlimitedCapacity;
    private Long createdBy;
    private Long lastModifiedBy;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private Set<TourScheduleSlotResponse> slots = new HashSet<>();
    private List<TourScheduleResponse> schedules = new ArrayList<>(); // Lista de horarios generados
}
