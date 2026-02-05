package com.tourya.api.models.request;

import com.tourya.api.constans.enums.MaritimeFlagEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request para crear/actualizar un reporte de actividad marítima DIMAR.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaritimActivityReportRequest {
    
    @NotNull(message = "El país es obligatorio")
    private String country;
    
    @NotNull(message = "La ciudad es obligatoria")
    private String city;
    
    @NotNull(message = "La actividad es obligatoria")
    private String activity;
    
    @NotNull(message = "La bandera es obligatoria")
    private MaritimeFlagEnum flag;
    
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate reportDate;
}

