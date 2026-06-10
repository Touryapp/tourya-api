package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.MaritimeFlagEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Entidad que representa un reporte de actividades marítimas de DIMAR.
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "maritim_activity_report")
public class MaritimActivityReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id", nullable = false)
    private State state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "business_category_id", nullable = false)
    private Integer businessCategoryId;

    @Column(name = "subcategory_code", nullable = false, length = 120)
    private String subcategoryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag", nullable = false, length = 20)
    private MaritimeFlagEnum flag;

    @Column(name = "report_start_date", nullable = false)
    private LocalDate reportStartDate;

    @Column(name = "report_end_date", nullable = false)
    private LocalDate reportEndDate;
}
