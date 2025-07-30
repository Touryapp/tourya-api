package com.tourya.api.repository;

import com.tourya.api.models.responses.SearchTourCategoryResponse;

import java.util.List;

public interface SearchTourCategoryRepository {
    List<SearchTourCategoryResponse> getTourCategories();
}
