package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tour_schedule_config")
public class TourScheduleConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // --- Opcional: config asociada a un tour específico ---
    @Column(name = "tour_id", nullable = true)
    private Integer tourId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", insertable = false, updatable = false)
    private Tour tour;

    // --- NUEVO: Opcional, config por proveedor ---
    @Column(name = "provider_id", nullable = true)
    private Integer providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", insertable = false, updatable = false)
    private Provider provider;

    // Una configuración puede tener múltiples slots
    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TourScheduleConfigSlot> slots = new HashSet<>();

    @Column(name = "label")
    private String label;

    // Eliminado: start_date / end_date (sin rango de fechas)
/*
    @Column(name = "start_date", nullable = true)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = true)
    private LocalDate endDate;
*/

    // Si usas PostgreSQL text[] (asegúrate del mapping de array)
    @Column(name = "days_of_week", columnDefinition = "text[]")
    private List<String> daysOfWeek;

    @Column(name = "is_unlimited_capacity")
    private Boolean isUnlimitedCapacity;

    @Column(name = "is_template")
    private Boolean isTemplate;
}
