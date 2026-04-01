package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class SearchTourCategoryResponse {
    private Integer id;
    private String name;
    private List<SubCategoryResponse> subCategories;

    @Data
    @AllArgsConstructor
    public static class SubCategoryResponse {
        private String code;
        private String name;
    }
}
