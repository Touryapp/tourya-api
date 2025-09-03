package com.tourya.api.services.impl;

import com.tourya.api.models.responses.TourScheduleConfigResponse;
import com.tourya.api.repository.TourConfigTemplateRepository;
import com.tourya.api.services.TourConfigTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class TourConfigTemplateServiceImpl implements TourConfigTemplateService {

    private final TourConfigTemplateRepository repository;

    @Override
    public List<TourScheduleConfigResponse> getConfigTemplatesByProvider(Integer providerId) {
        return repository.findByProviderId(providerId);
    }
}
