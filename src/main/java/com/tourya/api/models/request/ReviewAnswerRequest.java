package com.tourya.api.models.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tourya.api.config.TranslatedFieldDeserializer;
import com.tourya.api.models.TranslatedField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request para la respuesta del proveedor a una reseña.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReviewAnswerRequest {

    @Valid
    @JsonDeserialize(using = TranslatedFieldDeserializer.class)
    private TranslatedField comment;

    private String providerName;

    private String providerImage;

    private LocalDate date;

    @Min(value = 0, message = "Los likes no pueden ser negativos")
    private Integer likes;

    @Min(value = 0, message = "Los dislikes no pueden ser negativos")
    private Integer dislikes;

    @Min(value = 0, message = "Los hearts no pueden ser negativos")
    private Integer hearts;
}

