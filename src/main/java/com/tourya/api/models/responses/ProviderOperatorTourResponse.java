package com.tourya.api.models.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderOperatorTourResponse {
    private Integer tourId;
    private String tourName;
    private Boolean isPrincipal;
}
