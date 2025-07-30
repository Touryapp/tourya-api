package com.tourya.api.repository;

import com.tourya.api.models.responses.SearchTourLocationResponse;

import java.util.List;

public interface SearchTourLocationRepository {

    List<SearchTourLocationResponse> getTourLocations();

}
