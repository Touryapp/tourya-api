package com.tourya.api.models.responses;

import com.tourya.api.models.TranslatedField;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchTourSubCategoryResponse {
    private String code;
    private TranslatedField name;
}
