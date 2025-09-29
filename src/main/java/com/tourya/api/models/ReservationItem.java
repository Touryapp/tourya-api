package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reservation_item")
public class ReservationItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationItemId;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", insertable = false, updatable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private ShoppingCartItem shoppingCartItem;

    @Column(name = "service_responsible_name", length = 255)
    private String serviceResponsibleName;

    @Column(name = "service_responsible_email", length = 255)
    private String serviceResponsibleEmail;

    @Column(name = "service_responsible_phone", length = 20)
    private String serviceResponsiblePhone;
}
