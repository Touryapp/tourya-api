package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.CancellationReasonEnum;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidad que representa una reserva.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reservation")
@DynamicInsert
public class Reservation extends BaseEntity {

    /** Valor inicial en BD (migration 032); coincide con columna NOT NULL DEFAULT 'PENDING'. */
    public static final String PAYOUT_STATUS_PENDING = "PENDING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "qr_url", length = 500)
    private String qrUrl;

    @Column(name = "reservation_date", nullable = false)
    private LocalDateTime reservationDate;

    @Column(name = "payout_available_date")
    private LocalDate payoutAvailableDate;

    /** Pago al proveedor por esta reserva. PENDING hasta que una provider_payout_order que la incluye se marca PAID. */
    @Column(name = "payout_status", nullable = false, length = 20)
    private String payoutStatus = PAYOUT_STATUS_PENDING;

    /** Fecha/hora en que se marcó payout_status PAID (al subir comprobante de la orden). */
    @Column(name = "payout_paid_at")
    private OffsetDateTime payoutPaidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatusEnum deliveryStatus;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "service_responsible_name", length = 255)
    private String serviceResponsibleName;

    @Column(name = "service_responsible_email", length = 255)
    private String serviceResponsibleEmail;

    @Column(name = "service_responsible_phone", length = 20)
    private String serviceResponsiblePhone;

    @Column(name = "max_cancellation_date")
    private LocalDate maxCancellationDate;

    @Column(name = "max_rescheduling_date")
    private LocalDate maxReschedulingDate;

    @Column(name = "can_cancel", nullable = false)
    private Boolean canCancel = false;

    @Column(name = "can_reschedule", nullable = false)
    private Boolean canReschedule = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason")
    private CancellationReasonEnum cancellationReason;

    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;

    /**
     * BE-24 (RN-055): timestamp cuando el provider marcó "no puedo atender" la reserva.
     * NULL = no declinada. Al setearse se cancela automáticamente + crédito + anula AccountPayable.
     */
    @Column(name = "provider_declined_at")
    private OffsetDateTime providerDeclinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", insertable = false, updatable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private ShoppingCartItem shoppingCartItem;

    @PrePersist
    void ensurePayoutStatusForInsert() {
        if (payoutStatus == null || payoutStatus.isBlank()) {
            payoutStatus = PAYOUT_STATUS_PENDING;
        }
    }
}