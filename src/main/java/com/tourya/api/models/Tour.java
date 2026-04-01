package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.PriceTypeEnum;
import com.tourya.api.constans.enums.PriceTypeEnumConverter;
import com.tourya.api.constans.enums.TourDurationEnum;
import com.tourya.api.constans.enums.TourDurationEnumConverter;
import com.tourya.api.constans.enums.TourSubCategoryEnum;
import com.tourya.api.constans.enums.TourSubCategoryEnumConverter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
import java.util.List;

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

    @Convert(converter = TourSubCategoryEnumConverter.class)
    @Column(name = "sub_category", columnDefinition = "tour_subcategory_enum")
    @ColumnTransformer(write = "?::tour_subcategory_enum")
    private TourSubCategoryEnum subCategory;

    @Convert(converter = TourDurationEnumConverter.class)
    @Column(name = "duration_enum", columnDefinition = "tour_duration_enum")
    @ColumnTransformer(write = "?::tour_duration_enum")
    private TourDurationEnum durationEnum;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "time_of_day", columnDefinition = "tour_time_of_day_enum[]")
    @ColumnTransformer(write = "?::tour_time_of_day_enum[]")
    private String[] timeOfDay;

    @Column(name = "is_unlimited_capacity")
    private Boolean isUnlimitedCapacity;

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
    @Column(name = "price_type", columnDefinition = "price_type_enum")
    @ColumnTransformer(write = "?::price_type_enum")
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
