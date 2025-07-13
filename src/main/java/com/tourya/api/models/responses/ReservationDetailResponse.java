package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationDetailResponse {
    private Integer detailId;
    private String ageType;
    private Integer quantity;
    private BigDecimal priceAtReservation;
    private BigDecimal subtotal;
}
