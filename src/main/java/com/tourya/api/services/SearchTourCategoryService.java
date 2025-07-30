package com.tourya.api.services;

import com.tourya.api.models.responses.SearchTourCategoryResponse;

import java.util.List;

public interface SearchTourCategoryService {
    List<SearchTourCategoryResponse> getTourCategories();
}