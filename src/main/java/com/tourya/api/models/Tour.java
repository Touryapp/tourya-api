package com.tourya.api.models;


import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.constans.enums.TourStatusEnumConverter;
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

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tour")
public class Tour extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    private String name;
    private String description;

    // Many-to-One relationship with TourCategory
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private TourCategory tourCategory;

    private String duration;

    @Column(name = "max_people")
    private Integer maxPeople;

    @Column(name = "highlight")
    private Integer highlight;

    // Nuevas columnas
    @Column(name = "price", precision = 10, scale = 2) // Ajusta la precisión y escala para el precio
    private BigDecimal price;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "rating", precision = 3, scale = 2) // Por ejemplo, para 4.50 (precision total de 3 digitos, 2 despues del punto)
    private BigDecimal rating;

    @Convert(converter = TourStatusEnumConverter.class)
    @Column(name = "status")
    private TourStatusEnum status;

    // Many-to-One relationship with Provider
    @ManyToOne
    @JoinColumn(name = "provider_id")
    private Provider provider;


}
