package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.models.TranslatedField;
import com.tourya.api.constans.enums.PriceTypeEnum;
import com.tourya.api.constans.enums.TourDurationEnum;
import com.tourya.api.constans.enums.TourSubCategoryEnum;
import com.tourya.api.constans.enums.TourTimeOfDayEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TourResponse {
    private Integer id;
    private TranslatedField name;
    private TranslatedField description;
    private String duration;
    private Integer maxPeople;
    private PriceTypeEnum priceType;
    private Boolean isUnlimitedCapacity;
    private TourSubCategoryEnum subCategory;
    private TourDurationEnum durationEnum;
    private List<TourTimeOfDayEnum> timeOfDay;
    private Integer highlight;
    private BigDecimal price;
    private Integer minAge;
    private BigDecimal rating;
    private TourStatusEnum status;
    private TourCategoryResponse tourCategory;
    private ProviderResponse provider;
    private TourGalleryResponse profilePicture;

}
