package com.tourya.api.models.responses;

import com.tourya.api.models.TranslatedField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class TourItineraryResponse {
    private Integer id;
    private TranslatedField title;
    private Integer day;
    private String time;
    private TranslatedField description;
}