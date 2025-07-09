package com.tourya.api.repository;

import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import java.util.List;
import java.util.Map;

public interface SearchTourScheduleFullRepository {
    List<SearchTourScheduleFullResponse> callStoredProcedure(Map<String, Object> filters);
}
