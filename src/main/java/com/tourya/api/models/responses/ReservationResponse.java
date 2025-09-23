package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Response para información de reserva.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long paymentId;
    private LocalDateTime reservationDate;
    private DeliveryStatusEnum deliveryStatus;
    private String serviceResponsibleName;
    private String serviceResponsibleEmail;
    private Integer serviceResponsibleId;
    private String payerName;
    private String payerEmail;
    private Integer payerId;
    private Integer providerRating;
    private String comments;
    private String serviceData;
    private String qrImageBase64; // Imagen QR en Base64
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private Integer createdBy;
    private Integer lastModifiedBy;
}