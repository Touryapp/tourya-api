package com.tourya.api.models.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateSlotPercentageRangeRequest {

    /**
     * Porcentaje Tourya en puntos (ej. 15 = 15%). Solo backoffice.
     */
    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal slotPercentageTourya;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
