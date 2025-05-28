package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.TourStatusEnum;
import lombok.Data;

@Data
public class TourResponse {
    private Integer id;
    private String name;
    private String description;
    private String duration;
    private Integer maxPeople;
    private Integer highlight;
    private TourStatusEnum status;
    private TourCategoryResponse tourCategory;
    private ProviderResponse provider;

}
