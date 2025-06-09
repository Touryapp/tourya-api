package com.tourya.api.models;


import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.AddressTypeEnum;
import com.tourya.api.constans.enums.AddressTypeEnumConverter;
import com.tourya.api.constans.enums.CancellationPolicyTypeEnum;
import com.tourya.api.constans.enums.CancellationPolicyTypeEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "tour_cancellation_policy")
public class TourCancellationPolicy extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // Many-to-One relationship with Tour
    @ManyToOne
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Convert(converter = CancellationPolicyTypeEnumConverter.class)
    @Column(name = "cancellation_policy_type")
    private CancellationPolicyTypeEnum cancellationPolicyType;

    @Column(name = "allows_rain_refund")
    private boolean allowsRainRefund = Boolean.TRUE;

    @Column(name = "allows_rescheduling")
    private boolean allowsRescheduling = Boolean.TRUE;

    private String observations;

}
