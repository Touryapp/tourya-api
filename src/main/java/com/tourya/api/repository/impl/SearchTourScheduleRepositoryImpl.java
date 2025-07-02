package com.tourya.api.repository.impl;

import com.tourya.api.models.request.SearchTourScheduleRequest;
import com.tourya.api.models.responses.SearchTourScheduleResponse;
import com.tourya.api.repository.SearchTourScheduleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SearchTourScheduleRepositoryImpl implements SearchTourScheduleRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<SearchTourScheduleResponse> callStoredProcedure(SearchTourScheduleRequest request) {
        String sql = """
            SELECT * FROM sp_get_tour_schedule(:p_tour_id, :p_start_date, :p_end_date, :p_status, :p_limit, :p_offset)
        """;

        Query query = entityManager.createNativeQuery(sql, "SearchTourScheduleMapping");
        query.setParameter("p_tour_id", request.getTourId());
        query.setParameter("p_start_date", request.getStartDate());
        query.setParameter("p_end_date", request.getEndDate());
        query.setParameter("p_status", request.getStatus());
        query.setParameter("p_limit", request.getSize());
        query.setParameter("p_offset", (request.getPage() - 1) * request.getSize());

        return query.getResultList();
    }
}