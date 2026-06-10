package com.tourya.api.models.request;

import com.tourya.api.constans.enums.MaritimeFlagEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request para crear/actualizar un reporte de actividad marítima DIMAR.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaritimActivityReportRequest {

    @NotNull(message = "El ID del país es obligatorio")
    private Integer countryId;

    @NotNull(message = "El ID del departamento es obligatorio")
    private Integer stateId;

    @NotNull(message = "El ID de la ciudad es obligatorio")
    private Integer cityId;

    @NotNull(message = "El ID de la categoría es obligatorio")
    private Integer businessCategoryId;

    @NotBlank(message = "El código de subcategoría es obligatorio")
    private String subcategoryCode;

    @NotNull(message = "La bandera es obligatoria")
    private MaritimeFlagEnum flag;

    @NotNull(message = "La fecha de inicio del reporte es obligatoria")
    private LocalDate reportStartDate;

    @NotNull(message = "La fecha de fin del reporte es obligatoria")
    private LocalDate reportEndDate;
}
