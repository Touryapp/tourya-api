package com.tourya.api.models.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourGalleryRequest {
    private Integer id;
    private String description;
    private Integer orderIndex;
    private String fileKey;
    private boolean replaceFile = false;
}
