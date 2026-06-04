package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSlotPercentageResultResponse {
    private Integer tourId;
    private int schedulesProcessed;
    private int slotsUpdated;
    private int pricesRecalculated;
}
