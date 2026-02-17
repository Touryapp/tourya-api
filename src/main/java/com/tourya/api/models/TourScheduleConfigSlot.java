package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.time.LocalTime;
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
@Table(name = "tour_schedule_config_slot")
public class TourScheduleConfigSlot extends BaseEntity {
    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Specifies auto-increment for the ID
    private Integer id;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // Maps to SQL TIME

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // Maps to SQL TIME

    @Column(name = "config_id", nullable = false, insertable = false, updatable = false) // MODIFICACIÓN
    private Integer configId;

    // @ManyToOne // Relación Many-to-One con la entidad TourScheduleConfig
    // @JoinColumn(name = "config_id", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false) // MODIFICACIÓN: nullable=false para FK
    private TourScheduleConfig config; // La relación es la dueña de la columna 'config_id'

    // Un slot puede tener múltiples precios
    // orphanRemoval = true: Si un precio se elimina de esta lista, se borra de la
    // DB
    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TourScheduleConfigPrice> prices = new HashSet<>(); // CAMBIO: De List a Set, inicialización
}
