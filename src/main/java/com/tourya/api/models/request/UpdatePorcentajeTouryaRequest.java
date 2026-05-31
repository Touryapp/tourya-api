package com.tourya.api.models.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePorcentajeTouryaRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal porcentajeTourya;
}
