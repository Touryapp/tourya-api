package com.tourya.api.services;

import com.tourya.api.models.responses.SearchTourCategoryResponse;
import com.tourya.api.models.responses.SearchTourSubCategoryResponse;

import java.util.List;

public interface SearchTourCategoryService {
    List<SearchTourCategoryResponse> getTourCategories();
    List<SearchTourSubCategoryResponse> getTourSubCategories();
}