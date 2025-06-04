package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class TourItineraryResponse {
    private Integer id;
    private String title;
    private Integer day;
    private String time;
    private String description;
}