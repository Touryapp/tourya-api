package com.tourya.api.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.request.PublicTourScheduleSearchRequest;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.repository.SearchTourScheduleFullRepository;
import com.tourya.api.services.SearchTourScheduleFullService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchTourScheduleFullServiceImpl implements SearchTourScheduleFullService {

    private final SearchTourScheduleFullRepository searchRepo;
    private final ObjectMapper objectMapper;

    @Override
    public Page<SearchTourScheduleFullResponse> searchTourSchedule(PublicTourScheduleSearchRequest filters, Pageable pageable) {
        Map<String, Object> filterMap = new HashMap<>(objectMapper.convertValue(filters, Map.class));
        return searchRepo.callStoredProcedure(filterMap, pageable);
    }
}
