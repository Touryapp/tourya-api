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

    @Override
    public Page<SearchTourScheduleFullResponse> callStoredProcedure(Map<String, Object> filters, Pageable pageable) {

        String sql = "SELECT result FROM sp_get_tour_schedule_json(CAST(:filters_json AS jsonb))";
        Query query = entityManager.createNativeQuery(sql);

        try {
            // 🔹 Agregar paginación
            filters.put("page", pageable.getPageNumber());
            filters.put("size", pageable.getPageSize());

            // 🔹 Agregar ordenamiento si existe
            if (pageable.getSort().isSorted()) {
                pageable.getSort().forEach(order -> {
                    filters.put("sort_by", order.getProperty());
                    filters.put("sort_dir", order.getDirection().name()); // ASC o DESC
                });
            }

            // 🔹 Limpiar y asegurar valores simples para JSON
            Map<String, Object> cleanFilters = filters.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> (e.getValue() instanceof String ||
                                    e.getValue() instanceof Number ||
                                    e.getValue() instanceof Boolean ||
                                    e.getValue() instanceof List)
                                    ? e.getValue()
                                    : e.getValue().toString()
                    ));

            // 🔹 Convertir a JSON válido
            String jsonFilters = objectMapper.writeValueAsString(cleanFilters);

            // 📌 Log para depuración
            System.out.println("🚀 JSON enviado al SP: " + jsonFilters);

            // 🔹 Pasar el parámetro al SP
            query.setParameter("filters_json", jsonFilters);

            // 🔹 Ejecutar el SP y obtener resultados JSON
            List<String> jsonResults = query.getResultList();

            // 🔹 Convertir cada resultado JSON a DTO
            List<SearchTourScheduleFullResponse> results = jsonResults.stream().map(json -> {
                try {
                    return objectMapper.readValue(json, SearchTourScheduleFullResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException("Error deserializing result JSON", e);
                }
            }).collect(Collectors.toList());

            // 🔹 Retornar como Page
            return new PageImpl<>(results, pageable, results.size());

        } catch (Exception e) {
            throw new RuntimeException("Error executing stored procedure", e);
        }
    }
}
