package com.tourya.api.services;

import com.tourya.api.repository.TagCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagCategoryService {

    private final TagCategoryRepository repository;

    public List<String> getCategories() {
        return repository.getAllCategories();
    }
}
