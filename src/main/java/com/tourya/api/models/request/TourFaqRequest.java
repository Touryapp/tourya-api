package com.tourya.api.models.request;

import com.tourya.api.models.TranslatedField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourFaqRequest {
    private Integer id;
    @Valid
    @NotNull(message = "question is mandatory")
    private TranslatedField question;
    @Valid
    @NotNull(message = "answer is mandatory")
    private TranslatedField answer;
}
