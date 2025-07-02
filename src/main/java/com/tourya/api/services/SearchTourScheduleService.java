package com.tourya.api.service;

import com.tourya.api.models.request.SearchTourScheduleRequest;
import com.tourya.api.models.responses.SearchTourScheduleResponse;

import java.util.List;

public interface SearchTourScheduleService {
    List<SearchTourScheduleResponse> searchTourSchedule(SearchTourScheduleRequest request);
}
