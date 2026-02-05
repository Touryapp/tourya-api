package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.AgePriceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tour_schedule_config_price")
public class TourScheduleConfigPrice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrementing primary key
    private Integer id;

    // MODIFICACIÓN: Esta propiedad ahora es solo de lectura para la columna 'slot_id'.
    // La relación @ManyToOne 'slot' será la propietaria para insertar/actualizar.
    @Column(name = "slot_id", nullable = false, insertable = false, updatable = false)
    //@Column(name = "slot_id", nullable = false)
    private Integer slotId;

    //@ManyToOne // Relación Many-to-One con la entidad TourScheduleConfigSlot
    //@JoinColumn(name = "slot_id", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private TourScheduleConfigSlot slot;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "age_type", length = 20, nullable = false)
    private AgePriceType ageType;

    @Column(name = "min_age", nullable = false)
    private Integer minAge;

    @Column(name = "max_age", nullable = false)
    private Integer maxAge;

    @Column(name = "price", nullable = false)
    private BigDecimal price; // Using BigDecimal for numeric type

    @Column(name = "provider_price")
    private BigDecimal providerPrice; // Price that the provider receives
}
