package com.tourya.api.repository;

import com.tourya.api.models.request.SearchTourScheduleRequest;
import com.tourya.api.models.responses.SearchTourScheduleResponse;

import java.util.List;

public interface SearchTourScheduleRepository {
    List<SearchTourScheduleResponse> callStoredProcedure(SearchTourScheduleRequest request);
}