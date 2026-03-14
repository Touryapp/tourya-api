package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Detalle de un crédito usado en un pago (para respuesta API).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCreditItemResponse {
    private Long creditId;
    private BigDecimal amountUsed;
}
