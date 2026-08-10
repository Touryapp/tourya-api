package com.tourya.api.services;

import com.tourya.api.models.responses.TourScheduleConfigResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

// TC-019 (#231): overload con filtro opcional por subcategoria.
// interface expandida abajo

public interface TourConfigTemplateService {
    List<TourScheduleConfigResponse> getConfigTemplatesByProvider(Authentication connectedUser);

    // TC-019 (#231): filtro opcional por subcategoria. Si viene null/blank, devuelve todos.
    List<TourScheduleConfigResponse> getConfigTemplatesByProvider(Authentication connectedUser, String subCategory);
}


