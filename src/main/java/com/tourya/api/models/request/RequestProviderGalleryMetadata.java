package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RequestProviderGalleryMetadata {

    @Valid
    @NotNull
    private List<RequestProviderGalleryRequest> addedGalleries;

    @NotNull
    private List<RequestProviderGalleryRequest> deletedGalleries;
}
