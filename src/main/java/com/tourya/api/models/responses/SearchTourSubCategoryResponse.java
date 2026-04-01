package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchTourSubCategoryResponse {
    private String code;
    private String name;
}
