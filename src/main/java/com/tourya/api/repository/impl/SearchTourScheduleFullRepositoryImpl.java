package com.tourya.api.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.repository.SearchTourScheduleFullRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SearchTourScheduleFullRepositoryImpl implements SearchTourScheduleFullRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public Page<SearchTourScheduleFullResponse> callStoredProcedure(Map<String, Object> filters, Pageable pageable) {

        String sql = "SELECT result FROM sp_get_tour_schedule_json(CAST(:filters_json AS jsonb))";
        Query query = entityManager.createNativeQuery(sql);

        try {
            // Convertir los filtros a JSON
            // Set pagination params in filters
            filters.put("page", pageable.getOffset());
            filters.put("size", pageable.getPageSize());


            // Extraer campo de ordenamiento si existe
            if (pageable.getSort().isSorted()) {
                pageable.getSort().forEach(order -> {
                    filters.put("sort_by", order.getProperty());
                    filters.put("sort_dir", order.getDirection().name()); // ASC o DESC
                });
            }
            String jsonFilters = objectMapper.writeValueAsString(filters);

            // Asignar el parámetro sin el ":" (solo se usa en la cadena SQL)
            query.setParameter("filters_json", jsonFilters);
            List<String> jsonResults = query.getResultList();

            List<SearchTourScheduleFullResponse> results = jsonResults.stream().map(json -> {
                try {
                    return objectMapper.readValue(json, SearchTourScheduleFullResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException("Error deserializing result JSON", e);
                }
            }).collect(Collectors.toList());

            // Return a Page object (Spring Data compliant)
            return new PageImpl<>(results, pageable, results.size()); // Size can be adjusted if count is known

        } catch (Exception e) {
            throw new RuntimeException("Error executing stored procedure", e);
        }
    }
}
