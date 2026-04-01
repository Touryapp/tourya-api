package com.tourya.api.services;

import com.tourya.api.models.request.PublicTourScheduleSearchRequest;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchTourScheduleFullService {
    Page<SearchTourScheduleFullResponse> searchTourSchedule(PublicTourScheduleSearchRequest filters, Pageable pageable);
}
