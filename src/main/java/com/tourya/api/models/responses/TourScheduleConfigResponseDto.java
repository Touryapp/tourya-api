package com.tourya.api.models.responses;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class TourScheduleConfigResponseDto {
    private Integer id;
    private Integer tourId;
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> daysOfWeek;
    private Boolean isUnlimitedCapacity;
    private Long createdBy;
    private Long lastModifiedBy;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private Set<TourScheduleSlotResponseDto> slots = new HashSet<>();
    private List<TourScheduleResponseDto> schedules = new ArrayList<>(); // Lista de horarios generados
}
