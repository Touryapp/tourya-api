package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReserveCreditResponse {
    private Long shoppingCartItemId;
    private BigDecimal amountReserved;
    private List<Long> creditIdsReserved;
}
