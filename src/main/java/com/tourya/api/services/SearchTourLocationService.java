package com.tourya.api.services;

import com.tourya.api.models.responses.SearchTourLocationResponse;
import java.util.List;

public interface SearchTourLocationService {
    List<SearchTourLocationResponse> getTourLocations();
}
