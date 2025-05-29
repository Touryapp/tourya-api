package com.tourya.api.models.responses;

import lombok.Data;

@Data
public class TourFaqResponse {
    private Integer id;
    private String question;
    private String answer;
}
