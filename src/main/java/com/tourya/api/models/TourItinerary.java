package com.tourya.api.models;
import com.tourya.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;



@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class TourItinerary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Convert(converter = com.tourya.api.config.TranslatedFieldConverter.class)
    @Column(name = "title", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private TranslatedField title;

    @Column(name = "day")
    private Integer day;

    private LocalTime time;

    @Convert(converter = com.tourya.api.config.TranslatedFieldConverter.class)
    @Column(name = "description", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private TranslatedField description;
}