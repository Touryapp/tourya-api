package com.tourya.api.repository.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.responses.SearchTourCategoryResponse;
import com.tourya.api.repository.SearchTourCategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;


@Repository
@RequiredArgsConstructor
public class SearchTourCategoryRepositoryImpl implements SearchTourCategoryRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    private final ObjectMapper objectMapper;
    @Override
    public List<SearchTourCategoryResponse> getTourCategories() {
        String sql = "SELECT * FROM sp_get_categories_with_tours()";
        Query query = entityManager.createNativeQuery(sql);

        List<Object[]> results = query.getResultList();

        return results.stream().map(row -> {
            SearchTourCategoryResponse response = new SearchTourCategoryResponse();
            response.setId((Integer) row[0]);
            response.setName((String) row[1]);
            return response;
        }).collect(Collectors.toList());
    }
}
