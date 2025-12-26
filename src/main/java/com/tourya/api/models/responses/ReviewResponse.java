package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.ReviewStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response para información de reseña.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponse {

    private String id;
    private String tourName;
    private String tourId;
    private String tourImage;
    private String customerName;
    private String customerImage;
    private BigDecimal rating;
    private String comment;
    private LocalDate date;
    private String daysAgo;
    private Integer likes;
    private Integer dislikes;
    private Integer hearts;
    private String bookingId;
    private ReviewStatusEnum status;
    private String rejectionReason;
    private ReviewAnswerResponse answer;
    private List<String> attachmentUrls;
}

