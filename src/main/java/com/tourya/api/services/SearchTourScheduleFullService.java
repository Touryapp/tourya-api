package com.tourya.api.services;

import com.tourya.api.models.request.PublicTourScheduleSearchRequest;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;

public interface SearchTourScheduleFullService {
    Page<SearchTourScheduleFullResponse> searchTourSchedule(
            PublicTourScheduleSearchRequest filters, Pageable pageable, @Nullable Authentication connectedUser);
}
