package com.tourya.api.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.repository.SearchTourScheduleFullRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SearchTourScheduleFullRepositoryImpl implements SearchTourScheduleFullRepository {

    @PersistenceContext
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Override
    public List<SearchTourScheduleFullResponse> callStoredProcedure(Map<String, Object> filters) {
        String sql = "SELECT result FROM sp_get_tour_schedule_json(CAST(:filters_json AS jsonb))";
        Query query = entityManager.createNativeQuery(sql);

        try {
            // Convertir los filtros a JSON
            String jsonFilters = objectMapper.writeValueAsString(filters);

            // Asignar el parámetro sin el ":" (solo se usa en la cadena SQL)
            query.setParameter("filters_json", jsonFilters);

            // Ejecutar la consulta y obtener los resultados
            List<String> jsonResults = query.getResultList();

            // Mapear los resultados JSON a objetos de respuesta
            return jsonResults.stream().map(json -> {
                try {
                    return objectMapper.readValue(json, SearchTourScheduleFullResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException("Error deserializing JSON to SearchTourScheduleFullResponse", e);
                }
            }).toList();

        } catch (Exception e) {
            throw new RuntimeException("Error serializing filters to JSON", e);
        }
    }

}