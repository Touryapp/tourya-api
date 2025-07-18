package com.tourya.api.services;

import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface SearchTourScheduleFullService {
    Page<SearchTourScheduleFullResponse> searchTourSchedule(Map<String, Object> filters, Pageable pageable);
}
