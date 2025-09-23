package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.OrderStatusEnum;
import com.tourya.api.constans.enums.PaymentMethodTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Entidad que representa un pago realizado por un item del carrito de compras.
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
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 255)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatusEnum status;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type", length = 50)
    private PaymentMethodTypeEnum paymentMethodType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_cart_item_id", nullable = false)
    private ShoppingCartItem shoppingCartItem;

    // Datos del shopping cart item (duplicados para consultas rápidas)
    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "product_type", nullable = false, length = 50)
    private String productType;

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(name = "tour_schedule_id")
    private Long tourScheduleId;

    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "item_total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal itemTotalPrice;

    @Column(name = "item_status", nullable = false, length = 50)
    private String itemStatus;

    // Datos de la persona que paga (puede ser diferente al usuario del carrito)
    @Column(name = "payer_name", nullable = false, length = 255)
    private String payerName;

    @Column(name = "payer_email", nullable = false, length = 255)
    private String payerEmail;

    @Column(name = "payer_id", nullable = false)
    private Integer payerId;

    @Column(name = "payer_phone", length = 20)
    private String payerPhone;

    @Column(name = "payer_document_type", length = 50)
    private String payerDocumentType;

    @Column(name = "payer_document_number", length = 50)
    private String payerDocumentNumber;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations;

}
