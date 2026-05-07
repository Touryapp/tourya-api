package com.tourya.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "provider_payout_order_reservation")
@IdClass(ProviderPayoutOrderReservation.Pk.class)
public class ProviderPayoutOrderReservation {

    @Id
    @Column(name = "payout_order_id", nullable = false)
    private Long payoutOrderId;

    @Id
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "account_payable_id")
    private Long accountPayableId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_order_id", insertable = false, updatable = false)
    private ProviderPayoutOrder payoutOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", insertable = false, updatable = false)
    private Reservation reservation;

    public static class Pk implements Serializable {
        private Long payoutOrderId;
        private Long reservationId;

        public Pk() {}

        public Pk(Long payoutOrderId, Long reservationId) {
            this.payoutOrderId = payoutOrderId;
            this.reservationId = reservationId;
        }
    }
}

