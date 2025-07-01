package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import com.tourya.api.constans.enums.TourScheduleStatusEnumConverter;
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

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tour_schedule")
public class TourSchedule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "tour_id", nullable = false)
    private Integer tourId;

    @ManyToOne // Relación Many-to-One con la entidad Tour
    @JoinColumn(name = "tour_id", insertable = false, updatable = false)
    private Tour tour; // Asegúrate de tener una entidad Tour definida

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate; // DATE en SQL se mapea a LocalDate en Java

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // TIME en SQL se mapea a LocalTime en Java

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // TIME en SQL se mapea a LocalTime en Java

    @Column(name = "max_capacity")
    private Integer maxCapacity; // int4 en SQL se mapea a Integer en Java (permite NULL)

    @Column(name = "reserved_capacity")
    private Integer reservedCapacity; // int4 en SQL se mapea a Integer en Java, con DEFAULT 0

    @Column(name = "is_unlimited_capacity")
    private Boolean isUnlimitedCapacity; // bool en SQL se mapea a Boolean en Java, con DEFAULT false

    @Convert(converter = TourScheduleStatusEnumConverter.class)
    @Column(name = "status") // varchar(20) en SQL
    private TourScheduleStatusEnum status; // String en Java, con DEFAULT 'available'

    // MODIFICACIÓN: Esta propiedad ahora es solo de lectura para la columna 'config_id'.
    // La relación @ManyToOne 'config' será la propietaria para insertar/actualizar.
    @Column(name = "config_id", insertable = false, updatable = false)
    private Integer configId; // ID de la configuración que generó este horario

    @ManyToOne // Relación Many-to-One con la entidad TourScheduleConfig (si aplica)
    //@JoinColumn(name = "config_id", insertable = false, updatable = false)
    @JoinColumn(name = "config_id")
    private TourScheduleConfig config; // Asegúrate de tener una entidad TourScheduleConfig definida (la que creamos antes)

}
