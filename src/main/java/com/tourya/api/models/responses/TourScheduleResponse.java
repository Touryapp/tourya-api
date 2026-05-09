package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TourScheduleResponse {
    private Integer id;
    private Integer tourId;
    private LocalDate scheduleDate;
    private TourScheduleStatusEnum status;
    private Integer configId; // El ID de la configuración que generó este horario
    private TourScheduleConfigResponse config;
}
