package com.tourya.api.services.impl;

import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.repository.SearchTourScheduleFullRepository;
import com.tourya.api.services.SearchTourScheduleFullService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchTourScheduleFullServiceImpl implements SearchTourScheduleFullService {

    private final SearchTourScheduleFullRepository searchRepo;

    @Override
    public Page<SearchTourScheduleFullResponse> searchTourSchedule(Map<String, Object> filters, Pageable pageable) {
        return searchRepo.callStoredProcedure(filters, pageable);
    }
}
