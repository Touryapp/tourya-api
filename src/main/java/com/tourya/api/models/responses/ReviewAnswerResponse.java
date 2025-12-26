package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response para la respuesta del proveedor a una reseña.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewAnswerResponse {

    private String answerId;
    private String comment;
    private String providerName;
    private String providerImage;
    private LocalDate date;
    private String daysAgo;
    private Integer likes;
    private Integer dislikes;
    private Integer hearts;
}

