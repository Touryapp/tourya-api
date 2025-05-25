package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.RequestProviderStatusEnum;
import lombok.Data;

@Data
public class RequestProviderResponse {
    private Integer id;
    private RequestProviderStatusEnum status;
    private ProviderResponse provider;
}
