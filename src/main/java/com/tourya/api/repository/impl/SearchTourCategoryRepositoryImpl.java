package com.tourya.api.repository.impl;
import com.tourya.api.models.responses.SearchTourCategoryResponse;
import com.tourya.api.models.responses.SearchTourSubCategoryResponse;
import com.tourya.api.repository.SearchTourCategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SearchTourCategoryRepositoryImpl implements SearchTourCategoryRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<SearchTourCategoryResponse> getTourCategories() {
        String sql = """
                SELECT c.id, c.name, c.display_order, m.subcategory_code, m.display_name
                FROM public.tour_business_category c
                LEFT JOIN public.tour_business_subcategory_mapping m
                    ON m.business_category_id = c.id
                ORDER BY c.display_order, m.display_name
                """;
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        LinkedHashMap<Integer, SearchTourCategoryResponse> grouped = new LinkedHashMap<>();

        for (Object[] row : results) {
            Integer categoryId = ((Number) row[0]).intValue();
            SearchTourCategoryResponse category = grouped.computeIfAbsent(categoryId, ignored -> {
                SearchTourCategoryResponse response = new SearchTourCategoryResponse();
                response.setId(categoryId);
                response.setName((String) row[1]);
                response.setSubCategories(new ArrayList<>());
                return response;
            });

            if (row[3] != null) {
                category.getSubCategories().add(
                        new SearchTourCategoryResponse.SubCategoryResponse((String) row[3], (String) row[4]));
            }
        }

        return new ArrayList<>(grouped.values());
    }

    @Override
    public List<SearchTourSubCategoryResponse> getTourSubCategories() {
        String sql = """
                SELECT m.subcategory_code, m.display_name
                FROM public.tour_business_subcategory_mapping m
                JOIN public.tour_business_category c
                    ON c.id = m.business_category_id
                ORDER BY c.display_order, m.display_name
                """;
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();

        List<SearchTourSubCategoryResponse> response = new ArrayList<>();
        for (Object[] row : results) {
            response.add(new SearchTourSubCategoryResponse((String) row[0], (String) row[1]));
        }
        return response;
    }
}
