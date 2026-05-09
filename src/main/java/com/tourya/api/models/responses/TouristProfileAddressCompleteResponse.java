package com.tourya.api.models.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TouristProfileAddressCompleteResponse {
    private boolean addressComplete;
}

