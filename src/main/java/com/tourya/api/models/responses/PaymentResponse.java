package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.OrderStatusEnum;
import com.tourya.api.constans.enums.PaymentMethodTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response para información de pago.
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

    private Long id;
    private String transactionId;
    private OrderStatusEnum status;
    private BigDecimal amount;
    private String currency;
    private PaymentMethodTypeEnum paymentMethodType;
    private Long shoppingCartItemId;
    
    // Datos del shopping cart item
    private Integer productId;
    private String productType;
    private LocalDate scheduleDate;
    private Long tourScheduleId;
    private Long slotId;
    private BigDecimal itemTotalPrice;
    private String itemStatus;
    
    // Datos del pagador
    private String payerName;
    private String payerEmail;
    private Integer payerId;
    private String payerPhone;
    private String payerDocumentType;
    private String payerDocumentNumber;
    
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private Integer createdBy;
    private Integer lastModifiedBy;
}
