package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.models.TourCancellationPolicy;
import com.tourya.api.models.TranslatedField;
import com.tourya.api.constans.enums.PriceTypeEnum;
import com.tourya.api.constans.enums.TourDurationEnum;
import com.tourya.api.constans.enums.TourSubCategoryEnum;
import com.tourya.api.constans.enums.TourTimeOfDayEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TourFullDataResponse {
    private Integer id;
    private TranslatedField name;
    private TranslatedField description;
    private Integer tourCategoryId;
    private String duration;
    private Integer maxPeople;
    private PriceTypeEnum priceType;
    private Boolean isUnlimitedCapacity;
    private TourSubCategoryEnum subCategory;
    private TourDurationEnum durationEnum;
    private List<TourTimeOfDayEnum> timeOfDay = new ArrayList<>();
    private Integer highlight;
    private Integer minAge;
    private BigDecimal rating;
    private TourStatusEnum status;
    private ProviderResponse provider;
    private List<TourAddressResponse> locations = new ArrayList<>();
    private List<TourMainAttractionResponse> mainAttractions = new ArrayList<>();
    private List<TourIncludesExcludesResponse>  includes = new ArrayList<>();
    private List<TourIncludesExcludesResponse>  excludes = new ArrayList<>();
    private List<TourFaqResponse>  faq = new ArrayList<>();
    private List<TourItineraryResponse>  itineraries = new ArrayList<>();
    private List<TourCancellationPolicyResponse>  cancellationPolicies = new ArrayList<>();
    private List<TourGalleryResponse>  galleries = new ArrayList<>();

}
