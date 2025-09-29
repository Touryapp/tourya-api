package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

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

    private Long reservationId;
    private Long paymentId;
    private String qrUrl; // URL del QR en S3
    private LocalDateTime reservationDate;
    private DeliveryStatusEnum deliveryStatus;
    private LocalDateTime createdDate;
    private List<ReservationItemResponse> items;
    private LocalDateTime lastModifiedDate;
    private Integer createdBy;
    private Integer lastModifiedBy;
}