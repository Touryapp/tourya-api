package com.tourya.api.services.impl;

import com.tourya.api.models.Provider;
import com.tourya.api.models.User;
import com.tourya.api.models.responses.TourScheduleConfigResponse;
import com.tourya.api.repository.TourConfigTemplateRepository;
import com.tourya.api.services.ProviderService;
import com.tourya.api.services.TourConfigTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class TourConfigTemplateServiceImpl implements TourConfigTemplateService {

    private final TourConfigTemplateRepository repository;
    private final ProviderService providerService;

    @Override
    public List<TourScheduleConfigResponse> getConfigTemplatesByProvider(Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Provider provider = providerService.findByUserAndStatusActive(user);
        return repository.findByProviderId(provider);
    }
}
