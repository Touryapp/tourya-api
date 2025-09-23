package com.tourya.api.models.request;

import com.tourya.api.constans.enums.OrderStatusEnum;
import com.tourya.api.constans.enums.PaymentMethodTypeEnum;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request para crear un nuevo pago.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequest {

    @NotBlank(message = "El ID de transacción es obligatorio")
    @Size(max = 255, message = "El ID de transacción no puede exceder 255 caracteres")
    private String transactionId;

    @NotNull(message = "El estado del pago es obligatorio")
    private OrderStatusEnum status;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotBlank(message = "La moneda es obligatoria")
    @Size(min = 3, max = 3, message = "La moneda debe tener exactamente 3 caracteres")
    private String currency;

    private PaymentMethodTypeEnum paymentMethodType;

    @NotNull(message = "El ID del item del carrito es obligatorio")
    @Positive(message = "El ID del item del carrito debe ser positivo")
    private Long shoppingCartItemId;

    // Datos del shopping cart item
    @NotNull(message = "El ID del producto es obligatorio")
    @Positive(message = "El ID del producto debe ser positivo")
    private Integer productId;

    @NotBlank(message = "El tipo de producto es obligatorio")
    @Size(max = 50, message = "El tipo de producto no puede exceder 50 caracteres")
    private String productType;

    private LocalDate scheduleDate;

    private Long tourScheduleId;

    private Long slotId;

    @NotNull(message = "El precio total del item es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio total del item debe ser mayor a 0")
    private BigDecimal itemTotalPrice;

    @NotBlank(message = "El estado del item es obligatorio")
    @Size(max = 50, message = "El estado del item no puede exceder 50 caracteres")
    private String itemStatus;

    // Datos de la persona que paga
    @NotBlank(message = "El nombre del pagador es obligatorio")
    @Size(max = 255, message = "El nombre del pagador no puede exceder 255 caracteres")
    private String payerName;

    @NotBlank(message = "El email del pagador es obligatorio")
    @Email(message = "El email del pagador debe tener un formato válido")
    @Size(max = 255, message = "El email del pagador no puede exceder 255 caracteres")
    private String payerEmail;

    @NotNull(message = "El ID del pagador es obligatorio")
    @Positive(message = "El ID del pagador debe ser positivo")
    private Integer payerId;

    @Size(max = 20, message = "El teléfono del pagador no puede exceder 20 caracteres")
    private String payerPhone;

    @Size(max = 50, message = "El tipo de documento del pagador no puede exceder 50 caracteres")
    private String payerDocumentType;

    @Size(max = 50, message = "El número de documento del pagador no puede exceder 50 caracteres")
    private String payerDocumentNumber;
}
