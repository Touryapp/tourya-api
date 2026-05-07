package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.ReviewReasonTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ReviewReasonListResponse {
    private ReviewReasonTypeEnum type;
    @Builder.Default
    private List<ReviewReasonCatalogResponse.Item> reasons = new ArrayList<>();
}

