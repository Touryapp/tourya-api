package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TourTagResponse {
    private Integer tagId;
    private String category;
    private String name;
    private String description;
}
