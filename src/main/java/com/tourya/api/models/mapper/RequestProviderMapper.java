package com.tourya.api.models.mapper;

import com.tourya.api.models.RequestProvider;
import com.tourya.api.models.responses.RequestProviderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestProviderMapper {
    private final ProviderMapper providerMapper;

    public RequestProviderResponse toRequestProviderResponse(RequestProvider requestProvider) {
        RequestProviderResponse requestProviderResponse = new RequestProviderResponse();
        requestProviderResponse.setId(requestProvider.getId());
        requestProviderResponse.setStatus(requestProvider.getStatus());
        if(requestProvider.getProvider() != null) {
            requestProviderResponse.setProvider(providerMapper.toProviderResponse(requestProvider.getProvider()));
        }
        return requestProviderResponse;
    }
}
