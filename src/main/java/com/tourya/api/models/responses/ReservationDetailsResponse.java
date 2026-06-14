package com.tourya.api.models.responses;

import com.tourya.api.models.TranslatedField;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReservationDetailsResponse {

    // --- Reservation ---
    private Long reservationId;
    private String bookingId;
    private String reservationDate;
    private String reservationDeliveryStatus;
    private String reservationCreatedDate;

    // --- Payment ---
    private Long paymentId;
    private String paymentTransactionId;
    private String payerName;
    private String payerEmail;
    private String payerPhone;
    private String payerDocumentType;
    private String payerDocumentNumber;

    /** Foto de perfil del cliente (titular del carrito / {@code tourist_profile.photo_url}). */
    private String customerProfileImageUrl;

    // --- Shopping Item ---
    private Integer shoppingItemId;
    private Double shoppingTotalPrice;
    private Double shoppingUnitPrice;
    /** Suma de precios netos del proveedor al momento de la reserva. */
    private Double providerPrice;
    private Integer shoppingQuantity;

    private String productType;
    private Integer productId;

    private Long totalTourists; // BIGINT ✔

    // --- Tour ---
    private Integer tourId;
    private TranslatedField tourName;
    private Integer tourCategoryId;
    private String tourSubCategory;
    private Integer tourProviderId;

    // --- Schedule ---
    private Integer tourScheduleId;
    private String scheduleDate;

    private Integer slotId;
    private String slotTimeStart;
    private String slotTimeEnd;

    // minCapacity y maxCapacity eliminados - la capacidad ahora se maneja a nivel de TourSchedule

    // --- Responsable del servicio (quien asiste al tour) ---
    private String serviceResponsibleName;
    private String serviceResponsibleEmail;
    private String serviceResponsiblePhone;

    /** Operario principal del tour por parte del operador turístico. */
    private TourOperatorResponse tourOperator;
    
    // --- Campos de cancelación y re-agendamiento ---
    private java.time.LocalDate maxCancellationDate;
    private java.time.LocalDate maxReschedulingDate;
    private String cancellationReason;
    private java.time.LocalDateTime cancellationDate;
    
    // --- Validación de reagendamiento ---
    private Boolean canReschedule;

    // --- Validación de cancelación ---
    private Boolean canCancel;

    /** Cancelación por lluvia (DIMAR): solo el día del tour si hay reporte rojo vigente. */
    private Boolean canRainCancel;

    /** Confirmar entrega: true si la fecha del tour es hoy. */
    private Boolean canConfirmReservation;
}
