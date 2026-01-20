package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.MaritimeFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response para un reporte de actividad marítima DIMAR.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaritimActivityReportResponse {
    
    private Long id;
    private String country;
    private String city;
    private String activity;
    private MaritimeFlagEnum flag;
    private LocalDate reportDate;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}

