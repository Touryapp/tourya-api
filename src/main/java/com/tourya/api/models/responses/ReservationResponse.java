package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
    /** Identificador visible (ej. TB-250). */
    private String bookingId;
    private Long paymentId;
    private Long itemId;
    private String qrUrl; // URL del QR en S3
    private LocalDateTime reservationDate;
    private DeliveryStatusEnum deliveryStatus;
    private ServiceResponsibleResponse serviceResponsible;
    /** Operario principal del tour (operador turístico / agencia). */
    private TourOperatorResponse tourOperator;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private Integer createdBy;
    private Integer lastModifiedBy;
    
    /** Datos del pagador (tabla {@code payment}); null si la reserva aún no tiene pago asociado. */
    private String payerName;
    private String payerEmail;
    private String payerPhone;
    private String payerDocumentType;
    private String payerDocumentNumber;

    // Información adicional del tour
    private Integer tourId;
    private String tourName;
    private String tourImageUrl;
    private String tourType;
    private String duration;
    private LocalDateTime checkInDate;
    private LocalDateTime returnDate;
    /** Hora inicio del slot reservado (ej. 09:00). */
    private LocalTime slotStartTime;
    /** Hora fin del slot reservado (ej. 17:00). */
    private LocalTime slotEndTime;
    private String destination;
    /** Puntos de encuentro configurados en el tour (incluye {@code address}). */
    private List<TourAddressResponse> locations = new ArrayList<>();
    private Double price;
    /** Total que recibe el proveedor (suma de subtotales por ageType). */
    private BigDecimal providerTotalAmount;
    /** Desglose por tipo de turista (ADULT, CHILD, INFANT). */
    private List<ReservationPriceBreakdownResponse> priceBreakdown = new ArrayList<>();
    private String travellers;
    private List<String> activities;
    private List<String> extraServices;
    /** Actividades incluidas en el tour (i18n). Usar este campo, no {@code activities}. */
    private List<TourIncludesExcludesResponse> includes = new ArrayList<>();
    /** Actividades excluidas del tour (i18n). */
    private List<TourIncludesExcludesResponse> excludes = new ArrayList<>();
    
    // Campos de cancelación y re-agendamiento
    private java.time.LocalDate maxCancellationDate;
    private java.time.LocalDate maxReschedulingDate;
    private com.tourya.api.constans.enums.CancellationReasonEnum cancellationReason;
    private LocalDateTime cancellationDate;
    
    // Información del crédito creado al re-agendar
    private CreditResponse credit;
}