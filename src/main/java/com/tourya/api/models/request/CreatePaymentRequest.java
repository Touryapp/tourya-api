package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Request DTO para crear un pago.
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

    private String transactionData; // JSON string

    @Valid
    @NotEmpty(message = "Debe incluir al menos un item")
    private List<PaymentItemRequest> items;

    @Valid
    @NotNull(message = "Los datos del pagador son obligatorios")
    private PayerRequest payer;

    @Getter
    @Setter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentItemRequest {
        
        @NotNull(message = "El ID del item del carrito es obligatorio")
        @Positive(message = "El ID del item del carrito debe ser positivo")
        private Long shoppingCartItemId;

        @Valid
        @NotNull(message = "Los datos del responsable del servicio son obligatorios")
        private ServiceResponsibleRequest serviceResponsible;
    }

    @Getter
    @Setter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceResponsibleRequest {
        
        @NotBlank(message = "El nombre del responsable es obligatorio")
        @Size(max = 255, message = "El nombre del responsable no puede exceder 255 caracteres")
        private String name;

        @NotBlank(message = "El email del responsable es obligatorio")
        @Email(message = "El email del responsable debe tener un formato válido")
        @Size(max = 255, message = "El email del responsable no puede exceder 255 caracteres")
        private String email;

        @Size(max = 20, message = "El teléfono del responsable no puede exceder 20 caracteres")
        private String phone;
    }

    @Getter
    @Setter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PayerRequest {
        
        @NotBlank(message = "El nombre del pagador es obligatorio")
        @Size(max = 255, message = "El nombre del pagador no puede exceder 255 caracteres")
        private String name;

        @NotBlank(message = "El email del pagador es obligatorio")
        @Email(message = "El email del pagador debe tener un formato válido")
        @Size(max = 255, message = "El email del pagador no puede exceder 255 caracteres")
        private String email;

        @NotNull(message = "El ID del pagador es obligatorio")
        @Positive(message = "El ID del pagador debe ser positivo")
        private Integer id;

        @Size(max = 20, message = "El teléfono del pagador no puede exceder 20 caracteres")
        private String phone;

        @Size(max = 50, message = "El tipo de documento del pagador no puede exceder 50 caracteres")
        private String documentType;

        @Size(max = 50, message = "El número de documento del pagador no puede exceder 50 caracteres")
        private String documentNumber;
    }
}