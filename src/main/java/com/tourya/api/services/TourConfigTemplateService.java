package com.tourya.api.services;

import com.tourya.api.models.responses.TourScheduleConfigResponse;

import java.util.List;

public interface TourConfigTemplateService {
    List<TourScheduleConfigResponse> getConfigTemplatesByProvider(Integer providerId);
}


