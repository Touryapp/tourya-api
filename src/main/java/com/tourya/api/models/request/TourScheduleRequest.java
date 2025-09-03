package com.tourya.api.models.request;

import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
    public class TourScheduleRequest {
        private Integer id;
        private Integer tourId;
        private LocalDate scheduleDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer maxCapacity;
        private Integer reservedCapacity;
        private Boolean isUnlimitedCapacity;
        private TourScheduleStatusEnum status;
        private TourScheduleConfigDto config;
    }
