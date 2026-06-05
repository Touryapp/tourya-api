package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSlotPercentageResultResponse {
    private Integer tourId;
    /** Valor enviado y persistido (15 = 15%). Confirma que el PUT guardó lo correcto. */
    private BigDecimal savedSlotPercentageTourya;
    private Integer scheduleId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int schedulesProcessed;
    private int slotsUpdated;
    private int pricesRecalculated;
}
