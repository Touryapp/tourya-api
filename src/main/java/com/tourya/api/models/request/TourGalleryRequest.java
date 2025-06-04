package com.tourya.api.models.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourGalleryRequest {

   // @NotEmpty(message = "Image URL is mandatory")
    private String imageUrl;

    private String description;
//
//    @NotNull(message = "Order index is mandatory")
//    @Min(value = 0, message = "Order index must be 0 or greater")
    private Integer orderIndex;
}
