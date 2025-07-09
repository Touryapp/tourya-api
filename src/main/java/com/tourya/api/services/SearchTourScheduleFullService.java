package com.tourya.api.services;

import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import java.util.List;
import java.util.Map;

public interface SearchTourScheduleFullService {
    List<SearchTourScheduleFullResponse> searchTourSchedule(Map<String, Object> filters);
}