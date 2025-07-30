package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponse {
    private Integer reservationId;
    private Integer scheduleId;
    private Integer tourId;
    private String tourName;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private ZonedDateTime createdDate;
    private List<ReservationDetailResponse> details;
}
