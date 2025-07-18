package com.tourya.api.repository;

import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface SearchTourScheduleFullRepository {
    Page<SearchTourScheduleFullResponse> callStoredProcedure(Map<String, Object> filters, Pageable pageable);
}
