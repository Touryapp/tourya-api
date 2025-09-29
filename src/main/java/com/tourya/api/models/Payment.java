package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidad que representa un pago realizado.
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
    private Long paymentId;

    @Column(name = "transaction_id", nullable = false, length = 255)
    private String transactionId;

    @Column(name = "transaction_data", columnDefinition = "TEXT")
    private String transactionData; // JSON string

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "payer_id", nullable = false)
    private Integer payerId;

    @Column(name = "payer_name", nullable = false, length = 255)
    private String payerName;

    @Column(name = "payer_email", nullable = false, length = 255)
    private String payerEmail;

    @Column(name = "payer_phone", length = 20)
    private String payerPhone;

    @Column(name = "payer_document_type", length = 50)
    private String payerDocumentType;

    @Column(name = "payer_document_number", length = 50)
    private String payerDocumentNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", insertable = false, updatable = false)
    private Reservation reservation;
}