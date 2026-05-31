package com.tourya.api.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.repository.SearchTourScheduleFullRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SearchTourScheduleFullRepositoryImpl implements SearchTourScheduleFullRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    private final ObjectMapper objectMapper;

    @Override
    public Page<SearchTourScheduleFullResponse> callStoredProcedure(Map<String, Object> filters, Pageable pageable) {
        Map<String, Object> queryFilters = new HashMap<>(filters);
        queryFilters.put("page", pageable.getPageNumber());
        queryFilters.put("size", pageable.getPageSize());

        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                queryFilters.put("sort_by", order.getProperty());
                queryFilters.put("sort_dir", order.getDirection().name());
            });
        }

        List<SearchTourScheduleFullResponse> results = executeSearch(queryFilters);
        long total = countTours(filters);
        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public long countTours(Map<String, Object> filters) {
        try {
            Map<String, Object> countFilters = new HashMap<>(filters);
            countFilters.remove("page");
            countFilters.remove("size");
            countFilters.remove("sort_by");
            countFilters.remove("sort_dir");
            String jsonFilters = objectMapper.writeValueAsString(sanitizeFilters(countFilters));
            Query query = entityManager.createNativeQuery(
                    "SELECT sp_count_tour_schedule_json(CAST(:filters_json AS jsonb))");
            query.setParameter("filters_json", jsonFilters);
            Object result = query.getSingleResult();
            if (result instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(result.toString());
        } catch (Exception e) {
            throw new RuntimeException("Error counting tour schedules", e);
        }
    }

    private List<SearchTourScheduleFullResponse> executeSearch(Map<String, Object> filters) {
        String sql = "SELECT result FROM sp_get_tour_schedule_json(CAST(:filters_json AS jsonb))";
        Query query = entityManager.createNativeQuery(sql);
        try {
            String jsonFilters = objectMapper.writeValueAsString(sanitizeFilters(filters));
            log.debug("Tour schedule search filters: {}", jsonFilters);
            query.setParameter("filters_json", jsonFilters);

            @SuppressWarnings("unchecked")
            List<String> jsonResults = query.getResultList();

            return jsonResults.stream().map(json -> {
                try {
                    return objectMapper.readValue(json, SearchTourScheduleFullResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException("Error deserializing result JSON", e);
                }
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error executing stored procedure", e);
        }
    }

    private Map<String, Object> sanitizeFilters(Map<String, Object> filters) {
        return filters.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (e.getValue() instanceof String
                                || e.getValue() instanceof Number
                                || e.getValue() instanceof Boolean
                                || e.getValue() instanceof List)
                                ? e.getValue()
                                : e.getValue().toString()
                ));
    }
}
