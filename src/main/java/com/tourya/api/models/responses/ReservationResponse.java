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
    private Long itemId;
    private String qrUrl; // URL del QR en S3
    private LocalDateTime reservationDate;
    private DeliveryStatusEnum deliveryStatus;
    private ServiceResponsibleResponse serviceResponsible;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private Integer createdBy;
    private Integer lastModifiedBy;
    
    // Información adicional del tour
    private Integer tourId;
    private String tourName;
    private String tourImageUrl;
    private String tourType;
    private String duration;
    private LocalDateTime checkInDate;
    private LocalDateTime returnDate;
    private String destination;
    private Double price;
    private String travellers;
    private List<String> activities;
    private List<String> extraServices;
    
    // Campos de cancelación y re-agendamiento
    private java.time.LocalDate maxCancellationDate;
    private java.time.LocalDate maxReschedulingDate;
    private com.tourya.api.constans.enums.CancellationReasonEnum cancellationReason;
    private LocalDateTime cancellationDate;
    
    // Información del crédito creado al re-agendar
    private CreditResponse credit;
}