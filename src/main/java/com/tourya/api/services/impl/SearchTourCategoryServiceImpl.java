package com.tourya.api.services.impl;
import com.tourya.api.models.responses.SearchTourCategoryResponse;
import com.tourya.api.models.responses.SearchTourSubCategoryResponse;
import com.tourya.api.repository.SearchTourCategoryRepository;
import com.tourya.api.services.SearchTourCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class SearchTourCategoryServiceImpl implements SearchTourCategoryService {

    private final SearchTourCategoryRepository repository;

    @Override
    public List<SearchTourCategoryResponse> getTourCategories() {
        return repository.getTourCategories();
    }

    @Override
    public List<SearchTourSubCategoryResponse> getTourSubCategories() {
        return repository.getTourSubCategories();
    }
}