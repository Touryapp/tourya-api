package com.tourya.api.models.responses;

import com.tourya.api.models.TranslatedField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourTagResponse {
    private Integer id;
    private String dimension;
    private TranslatedField name;
    private String slug;
}
