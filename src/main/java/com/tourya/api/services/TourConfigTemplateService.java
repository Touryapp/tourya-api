package com.tourya.api.services;

import com.tourya.api.models.responses.TourScheduleConfigResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface TourConfigTemplateService {
    List<TourScheduleConfigResponse> getConfigTemplatesByProvider(Authentication connectedUser);
}


