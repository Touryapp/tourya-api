package com.tourya.api.models.responses;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class TourReviewSummaryResponse {
    private Integer tourId;
    private BigDecimal averageRating; // 1 decimal
    private Long totalReviews;
    private Map<Integer, Long> countByStars; // 1..5
}

