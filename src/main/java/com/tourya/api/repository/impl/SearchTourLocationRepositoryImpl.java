package com.tourya.api.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.responses.SearchTourLocationResponse;
import com.tourya.api.repository.SearchTourLocationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SearchTourLocationRepositoryImpl implements SearchTourLocationRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    private final ObjectMapper objectMapper;

    @Override
    public List<SearchTourLocationResponse> getTourLocations() {
        Query query = entityManager.createNativeQuery("SELECT * FROM sp_get_locations_with_tours()");
        List<Object[]> results = query.getResultList();

        return results.stream().map(row -> {
            SearchTourLocationResponse response = new SearchTourLocationResponse();
            response.setCountryId((Integer) row[0]);
            response.setCountryName((String) row[1]);
            response.setStateId((Integer) row[2]);
            response.setStateName((String) row[3]);
            response.setCityId((Integer) row[4]);
            response.setCityName((String) row[5]);
            return response;
        }).collect(Collectors.toList());
    }
}