package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.PriceTypeEnum;
import com.tourya.api.constans.enums.PriceTypeEnumConverter;
import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.constans.enums.TourStatusEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

    @Convert(converter = com.tourya.api.config.TranslatedFieldConverter.class)
    @Column(name = "name", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private TranslatedField name;

    @Convert(converter = com.tourya.api.config.TranslatedFieldConverter.class)
    @Column(name = "description", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private TranslatedField description;

    // Many-to-One relationship with TourCategory
    // @ManyToOne
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private TourCategory tourCategory;

    private String duration;

    @Column(name = "max_people")
    private Integer maxPeople;

    @Column(name = "highlight")
    private Integer highlight;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "rating", precision = 3, scale = 2) // Por ejemplo, para 4.50 (precision total de 3 digitos, 2
                                                       // despues del punto)
    private BigDecimal rating;

    @Convert(converter = TourStatusEnumConverter.class)
    @Column(name = "status")
    private TourStatusEnum status;

    @Convert(converter = PriceTypeEnumConverter.class)
    @Column(name = "price_type")
    private PriceTypeEnum priceType;

    // Many-to-One relationship with Provider
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    // Many-to-One relationship with Service
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private TouryaService service;
}
