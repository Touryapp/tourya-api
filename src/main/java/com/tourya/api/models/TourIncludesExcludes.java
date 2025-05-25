package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnumCoverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "tour_includes_excludes")
public class TourIncludesExcludes extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    private String description;

    @Convert(converter = IncludeExcludeTypeEnumCoverter.class)
    @Column(name = "type")
    private IncludeExcludeTypeEnum type;

}