package com.tourya.api.models.request;

import com.tourya.api.models.TranslatedField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourGalleryRequest {
    private Integer id;
    @Valid
    private TranslatedField description;
    private Integer orderIndex;
    private String fileKey;
    private boolean replaceFile = false;
}
