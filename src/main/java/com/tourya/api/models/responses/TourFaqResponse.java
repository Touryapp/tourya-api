package com.tourya.api.models.responses;

import com.tourya.api.models.TranslatedField;
import lombok.Data;

@Data
public class TourFaqResponse {
    private Integer id;
    private TranslatedField question;
    private TranslatedField answer;
}
