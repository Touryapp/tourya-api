package com.tourya.api.services.impl;

import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.repository.SearchTourScheduleFullRepository;
import com.tourya.api.services.SearchTourScheduleFullService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchTourScheduleFullServiceImpl implements SearchTourScheduleFullService {

    private final SearchTourScheduleFullRepository repository;

    @Override
    public List<SearchTourScheduleFullResponse> searchTourSchedule(Map<String, Object> filters) {
        return repository.callStoredProcedure(filters);
    }
}