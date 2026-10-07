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
    /**
     * Sprint 2 mobile — deuda 3: expone el mismo desglose por ageType que ya trae
     * {@link ReservationDetailsResponse#getTravelerBreakdown()} (vista provider,
     * calculada por {@code sp_get_provider_reservations}, TC-016). Permite al mobile
     * del turista renderizar en ReservationDetailPage el mismo bloque que ve el
     * provider. Se puebla desde {@code shopping_cart_item_detail} (ageType, quantity,
     * unitPrice, providerUnitPrice) en {@code ReservationService.enrichReservationResponse}.
     * Redundante con {@code priceBreakdown} pero se mantiene el mismo tipo del provider
     * para poder compartir componente de UI entre ambas vistas.
     */
    private List<TravelerBreakdownDto> travelerBreakdown = new ArrayList<>();
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

    /**
     * v24 (Luis 2026-10-06 issue #37): fecha del tour (shopping_cart_item.schedule_date).
     * Antes la UI mobile usaba checkInDate (LocalDateTime) y renderizaba con la zona
     * del dispositivo, lo que mostraba el dia equivocado. Este campo viaja como
     * string ISO ("YYYY-MM-DD") sin TZ para que el mobile lo pinte tal cual.
     */
    private java.time.LocalDate scheduleDate;

    /**
     * v24 (issue #35 Luis 2026-10-06): bandera "Permite cancelación por lluvia"
     * tomada de la politica de cancelacion del tour (TourCancellationPolicy.allowsRainCancellation).
     * El detalle mobile la muestra junto a maxCancellationDate / maxReschedulingDate.
     */
    private Boolean allowsRainCancellation;

    /**
     * v23 (Luis 2026-10-04 issue #35 RES-347): expuesto al mobile para que la
     * pantalla de detalle muestre u oculte el botón "Reagendar"/"Cancelar".
     * Antes solo venía en el listado (ReservationDetailsResponse via SP);
     * ahora {@code GET /reservations/{id}} también lo trae. Calculado por
     * {@link com.tourya.api.services.ReservationService#enrichReservationResponse}
     * replicando las mismas validaciones que
     * {@link com.tourya.api.services.ReservationService#validateRescheduleReservation}
     * salvo el check de ownership (el controller ya filtra por usuario).
     */
    private Boolean canReschedule;
    private Boolean canCancel;

    /**
     * v24 (issue #33 Luis 2026-10-06): priceType del tour (INDIVIDUAL/GRUPO).
     * El mobile lo usa para elegir el formato de "travellers" en los cards
     * del listado (hoy el campo string {@code travellers} ya viene formateado
     * desde el backend; igual exponemos el enum crudo para que la UI tome
     * decisiones de layout aparte del label).
     */
    private com.tourya.api.constans.enums.PriceTypeEnum priceType;
    
    // Información del crédito creado al re-agendar
    private CreditResponse credit;
}