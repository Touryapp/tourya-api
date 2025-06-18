package com.tourya.api.models.responses;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestProviderGalleryResponse {
    private Integer id;
    private String imageUrl;
    private String description;
    private Integer orderIndex;
    private Integer documentTypeId;
    private String documentTypeName;
}