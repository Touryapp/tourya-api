package com.tourya.api.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourFaqRequest {
    private Integer id;
    @NotEmpty(message = "question is mandatory")
    @NotNull(message = "question is mandatory")
    private String question;
    @NotEmpty(message = "answer is mandatory")
    @NotNull(message = "answer is mandatory")
    private String answer;
}
