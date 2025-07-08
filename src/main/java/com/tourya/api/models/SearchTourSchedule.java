package com.tourya.api.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "tour_schedule")
@Data
@SqlResultSetMapping(
        name = "SearchTourScheduleMapping",
        classes = @ConstructorResult(
                targetClass = com.tourya.api.models.responses.SearchTourScheduleResponse.class,
                columns = {
                        @ColumnResult(name = "schedule_id", type = Integer.class),
                        @ColumnResult(name = "tour_id", type = Integer.class),
                        @ColumnResult(name = "schedule_date", type = LocalDate.class),
                        @ColumnResult(name = "start_time", type = LocalTime.class),
                        @ColumnResult(name = "end_time", type = LocalTime.class),
                        @ColumnResult(name = "is_unlimited_capacity", type = Boolean.class),
                        @ColumnResult(name = "max_capacity", type = Integer.class),
                        @ColumnResult(name = "reserved_capacity", type = Integer.class),
                        @ColumnResult(name = "status", type = String.class)
                }
        )
)
public class SearchTourSchedule {

    @Id
    @Column(name = "schedule_id")
    private Integer scheduleId;

    @Column(name = "tour_id")
    private Integer tourId;

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "is_unlimited_capacity")
    private Boolean isUnlimitedCapacity;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "reserved_capacity")
    private Integer reservedCapacity;

    @Column(name = "status")
    private String status;
}