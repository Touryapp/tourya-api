package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entidad que representa una reserva generada después de un pago exitoso.
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
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "qr_token", nullable = false, length = 500)
    private String qrToken;

    @Column(name = "reservation_date", nullable = false)
    private LocalDateTime reservationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatusEnum deliveryStatus;

    @Column(name = "service_responsible_name", nullable = false, length = 255)
    private String serviceResponsibleName;

    @Column(name = "service_responsible_email", nullable = false, length = 255)
    private String serviceResponsibleEmail;

    @Column(name = "service_responsible_id", nullable = false)
    private Integer serviceResponsibleId;

    @Column(name = "payer_name", nullable = false, length = 255)
    private String payerName;

    @Column(name = "payer_email", nullable = false, length = 255)
    private String payerEmail;

    @Column(name = "payer_id", nullable = false)
    private Integer payerId;

    @Column(name = "provider_rating")
    private Integer providerRating;

    @Column(name = "comments", length = 1000)
    private String comments;

    @Column(name = "service_data", length = 2000)
    private String serviceData; // JSON string con todos los datos del servicio
}
