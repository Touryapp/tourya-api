// Service Implementations
package com.tourya.api.services.impl;

import com.tourya.api.models.responses.SearchTourLocationResponse;
import com.tourya.api.repository.SearchTourLocationRepository;
import com.tourya.api.services.SearchTourLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchTourLocationServiceImpl implements SearchTourLocationService {

    private final SearchTourLocationRepository repository;

    @Override
    public List<SearchTourLocationResponse> getTourLocations() {
        return repository.getTourLocations();
    }
}
