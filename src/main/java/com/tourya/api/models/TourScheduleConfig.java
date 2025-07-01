package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
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

    @Column(name = "tour_id", nullable = false)
    private Integer tourId;

    @ManyToOne // Relación Many-to-One con la entidad Tour
    @JoinColumn(name = "tour_id", insertable = false, updatable = false)
    private Tour tour; // Asegúrate de tener una entidad Tour definida

    // Una configuración puede tener múltiples slots
    // orphanRemoval = true: Si un slot se elimina de esta lista, se borra de la DB
    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TourScheduleConfigSlot> slots = new HashSet<>(); // CAMBIO: De List a Set, inicialización

    @Column(name = "label")
    private String label; // TEXT en SQL se mapea a String en Java

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // DATE en SQL se mapea a LocalDate en Java

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // DATE en SQL se mapea a LocalDate en Java

    // Opción 1 (Directa, a veces funciona con ciertos dialectos de Hibernate para PostgreSQL arrays)
    @Column(name = "days_of_week", columnDefinition = "text[]")
    private List<String> daysOfWeek;

    @Column(name = "is_unlimited_capacity")
    private Boolean isUnlimitedCapacity;
}
