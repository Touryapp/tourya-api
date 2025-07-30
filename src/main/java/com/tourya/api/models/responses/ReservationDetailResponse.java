package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.AgePriceType;
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
    private AgePriceType ageType;
    private Integer quantity;
    private BigDecimal priceAtReservation;
    private BigDecimal subtotal;
}
