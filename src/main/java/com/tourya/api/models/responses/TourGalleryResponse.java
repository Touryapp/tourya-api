package com.tourya.api.models.responses;

import com.tourya.api.models.TranslatedField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TourGalleryResponse {

    private Integer id;
    private String imageUrl;
    private TranslatedField description;
    private Integer orderIndex;
}
