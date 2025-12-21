package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.models.TranslatedField;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TourResponse {
    private Integer id;
    private TranslatedField name;
    private TranslatedField description;
    private String duration;
    private Integer maxPeople;
    private Integer highlight;
    private BigDecimal price;
    private Integer minAge;
    private BigDecimal rating;
    private TourStatusEnum status;
    private TourCategoryResponse tourCategory;
    private ProviderResponse provider;
    private TourGalleryResponse profilePicture;

}
