package com.tourya.api.models.request;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Request para crear una nueva reserva.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CreateReservationRequest {

    @NotNull(message = "El ID del pago es obligatorio")
    @Positive(message = "El ID del pago debe ser positivo")
    private Long paymentId;

    // qrToken se genera automáticamente en el servicio, no es obligatorio en el request
    private String qrToken;

    @NotNull(message = "La fecha de reserva es obligatoria")
    private LocalDateTime reservationDate;

    @NotNull(message = "El estado de entrega es obligatorio")
    private DeliveryStatusEnum deliveryStatus;

    @NotBlank(message = "El nombre del responsable del servicio es obligatorio")
    @Size(max = 255, message = "El nombre del responsable no puede exceder 255 caracteres")
    private String serviceResponsibleName;

    @NotBlank(message = "El email del responsable del servicio es obligatorio")
    @Email(message = "El email del responsable debe tener un formato válido")
    @Size(max = 255, message = "El email del responsable no puede exceder 255 caracteres")
    private String serviceResponsibleEmail;

    @NotNull(message = "El ID del responsable del servicio es obligatorio")
    @Positive(message = "El ID del responsable del servicio debe ser positivo")
    private Integer serviceResponsibleId;

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

    @Min(value = 1, message = "La calificación del proveedor debe ser entre 1 y 5")
    @Max(value = 5, message = "La calificación del proveedor debe ser entre 1 y 5")
    private Integer providerRating;

    @Size(max = 1000, message = "Los comentarios no pueden exceder 1000 caracteres")
    private String comments;

    // serviceData se genera automáticamente en el servicio, no es obligatorio en el request
    private String serviceData;
}
