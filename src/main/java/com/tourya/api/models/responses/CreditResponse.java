package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.CreditStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditResponse {
    private Long id;
    private Long reservationId;
    private Integer userId;
    private Integer transferredFromUserId;
    private LocalDateTime transferredAt;
    private BigDecimal amount;
    private BigDecimal reservedAmount;
    private Long shoppingCartItemId;
    private LocalDate creationDate;
    private LocalDate expirationDate;
    private CreditStatusEnum status;

    // TC-022 (#253): flujo de devolucion de creditos.
    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundedAt;
    private String refundProofUrl;

    // TC-022: informacion embebida del turista (solo poblada en el listado admin).
    private String touristName;
    private String touristEmail;
}
