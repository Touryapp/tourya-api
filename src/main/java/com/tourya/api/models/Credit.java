package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.CreditStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa un crédito generado por cancelación o re-agendamiento de una reserva.
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
@Table(name = "credit")
public class Credit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CreditStatusEnum status;

    /**
     * Monto reservado para un item del carrito. Al consumir se deduce de amount.
     */
    @Column(name = "reserved_amount", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal reservedAmount;

    /**
     * Item del carrito al que está asociada la reserva. NULL cuando no está reservado.
     */
    @Column(name = "shopping_cart_item_id")
    private Long shoppingCartItemId;

    @Column(name = "transferred_from_user_id")
    private Integer transferredFromUserId;

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;

    /**
     * BE-18: timestamp del envío del recordatorio 30 días antes. NULL = pendiente.
     */
    @Column(name = "reminder_30d_sent_at")
    private LocalDateTime reminder30dSentAt;

    /**
     * BE-18: timestamp del envío del recordatorio 7 días antes.
     */
    @Column(name = "reminder_7d_sent_at")
    private LocalDateTime reminder7dSentAt;

    /**
     * BE-19: timestamp del envío del correo de expiración.
     */
    @Column(name = "expired_notified_at")
    private LocalDateTime expiredNotifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", insertable = false, updatable = false)
    private Reservation reservation;
}

