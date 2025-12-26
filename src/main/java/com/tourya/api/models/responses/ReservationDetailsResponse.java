package com.tourya.api.models.responses;

import com.tourya.api.models.TranslatedField;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReservationDetailsResponse {

    // --- Reservation ---
    private Long reservationId;
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

    // --- Shopping Item ---
    private Integer shoppingItemId;
    private Double shoppingTotalPrice;
    private Double shoppingUnitPrice;
    private Integer shoppingQuantity;

    private String productType;
    private Integer productId;

    private Long totalTourists; // BIGINT ✔

    // --- Tour ---
    private Integer tourId;
    private TranslatedField tourName;
    private Integer tourCategoryId;
    private Integer tourProviderId;

    // --- Schedule ---
    private Integer tourScheduleId;
    private String scheduleDate;

    private Integer slotId;
    private String slotTimeStart;
    private String slotTimeEnd;

    private Integer minCapacity;
    private Integer maxCapacity;

    // --- Responsible ---
    private String serviceResponsibleName;
    private String serviceResponsibleEmail;
    private String serviceResponsiblePhone;
}
