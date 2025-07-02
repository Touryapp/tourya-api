package com.tourya.api.services.impl;

import com.tourya.api.models.request.SearchTourScheduleRequest;
import com.tourya.api.models.responses.SearchTourScheduleResponse;
import com.tourya.api.repository.SearchTourScheduleRepository;
import com.tourya.api.service.SearchTourScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchTourScheduleServiceImpl implements SearchTourScheduleService {

    private final SearchTourScheduleRepository repository;

    @Override
    public List<SearchTourScheduleResponse> searchTourSchedule(SearchTourScheduleRequest request) {
        return repository.callStoredProcedure(request);
    }
}