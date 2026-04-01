package com.tourya.api.repository;

import com.tourya.api.models.responses.SearchTourCategoryResponse;
import com.tourya.api.models.responses.SearchTourSubCategoryResponse;

import java.util.List;

public interface SearchTourCategoryRepository {
    List<SearchTourCategoryResponse> getTourCategories();
    List<SearchTourSubCategoryResponse> getTourSubCategories();
}
