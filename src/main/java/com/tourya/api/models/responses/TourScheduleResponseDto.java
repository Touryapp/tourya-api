package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TourScheduleResponseDto {
    private Integer id;
    private Integer tourId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxCapacity;
    private Integer reservedCapacity;
    private Boolean isUnlimitedCapacity;
    private TourScheduleStatusEnum status;
    private Integer configId; // El ID de la configuración que generó este horario
}
