package com.tourya.api.models.responses;

import com.tourya.api.models.TranslatedField;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourMainAttractionResponse {
    private Integer id;
    private TranslatedField description;
}