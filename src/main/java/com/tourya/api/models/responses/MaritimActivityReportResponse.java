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
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaritimActivityReportResponse {

    private Long id;
    private Integer countryId;
    private Integer stateId;
    private Integer cityId;
    private Integer businessCategoryId;
    private String subcategoryCode;
    private MaritimeFlagEnum flag;
    private LocalDate reportStartDate;
    private LocalDate reportEndDate;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}
