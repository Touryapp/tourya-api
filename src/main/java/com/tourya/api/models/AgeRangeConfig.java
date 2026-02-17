package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.AgePriceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entidad que representa la configuración centralizada de rangos de edad.
 * Esta tabla almacena los rangos de edad predefinidos (ADULT, CHILD, INFANT)
 * que se utilizan en los precios de tours.
 */
@Entity
@Table(name = "age_range_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AgeRangeConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Age type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "age_type", unique = true, nullable = false, length = 20)
    private AgePriceType ageType;

    @NotNull(message = "Minimum age is required")
    @Min(value = 0, message = "Minimum age must be 0 or greater")
    @Column(name = "min_age", nullable = false)
    private Integer minAge;

    @NotNull(message = "Maximum age is required")
    @Min(value = 0, message = "Maximum age must be 0 or greater")
    @Column(name = "max_age", nullable = false)
    private Integer maxAge;

    @Column(name = "description", length = 255)
    private String description;

    @NotNull(message = "Active status is required")
    @Column(name = "is_active", nullable = false)
    @lombok.Builder.Default
    private Boolean isActive = true;
}
