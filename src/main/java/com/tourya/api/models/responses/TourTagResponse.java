package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TourTagResponse {
    private Integer id;
    private String dimension;
    private String name;
    private String slug;
}
