package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.CreditStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditResponse {
    private Long id;
    private Long reservationId;
    private BigDecimal amount;
    private BigDecimal reservedAmount;
    private Long shoppingCartItemId;
    private LocalDate creationDate;
    private LocalDate expirationDate;
    private CreditStatusEnum status;
}


