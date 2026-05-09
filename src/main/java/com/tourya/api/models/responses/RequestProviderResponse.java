package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.RequestProviderStatusEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RequestProviderResponse {
    private Integer id;
    private RequestProviderStatusEnum status;
    private String declinedReason;
    private String incompleteReason;
    private ProviderResponse provider;
    private List<RequestProviderGalleryResponse> requestProviderGalleryList = new ArrayList<>();
}
