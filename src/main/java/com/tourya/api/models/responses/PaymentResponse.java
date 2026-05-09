package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO para pago.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private String transactionId;
    private String transactionData;
    /** Monto total pagado con créditos en este pago; null si no se usaron créditos. */
    private BigDecimal amountCredit;
    /** Detalle de créditos usados (id del crédito y monto usado). */
    private List<PaymentCreditItemResponse> creditsUsed;
    private List<ReservationResponse> reservations;
    private PayerResponse payer;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private Integer createdBy;
    private Integer lastModifiedBy;

}