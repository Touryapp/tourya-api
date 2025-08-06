package com.tourya.api.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TourTagRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> getAllTourTags() {
        return entityManager
                .createNativeQuery("""
                    SELECT id AS tagId, category::TEXT AS category, name, description
                    FROM tour_tag
                    ORDER BY category, name
                """)
                .getResultList();
    }
}
