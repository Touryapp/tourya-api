package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Response para información de reserva de tour.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourReservationResponse {

    private Long reservationId;
    private Long scheduleId;
    private Long tourId;
    private String tourName;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private ZonedDateTime createdDate;
    private List<ReservationDetailResponse> details;
}
