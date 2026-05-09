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
@Table(name = "maritim_activity_report")
public class MaritimActivityReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "activity", nullable = false, length = 255)
    private String activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag", nullable = false, length = 20)
    private MaritimeFlagEnum flag;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;
}

