package com.tourya.api.models.request;

import com.tourya.api.constans.enums.ReviewStatusEnum;
import com.tourya.api.models.TranslatedField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request para actualizar una reseña.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateReviewRequest {

    @DecimalMin(value = "1.00", inclusive = true, message = "La calificación debe ser entre 1 y 5")
    @DecimalMax(value = "5.00", inclusive = true, message = "La calificación debe ser entre 1 y 5")
    private BigDecimal rating;

    @Valid
    private TranslatedField comment;

    @Min(value = 0, message = "Los likes no pueden ser negativos")
    private Integer likes;

    @Min(value = 0, message = "Los dislikes no pueden ser negativos")
    private Integer dislikes;

    @Min(value = 0, message = "Los hearts no pueden ser negativos")
    private Integer hearts;

    private ReviewStatusEnum status;

    @Size(max = 500, message = "La razón de rechazo no puede exceder 500 caracteres")
    private String rejectionReason;

    @Valid
    private ReviewAnswerRequest answer;
}

