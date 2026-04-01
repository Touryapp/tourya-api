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
                    SELECT t.id, d.nombre AS dimension, t.nombre, t.slug
                    FROM tags t
                    JOIN tag_dimensions d ON d.id = t.dimension_id
                    ORDER BY d.display_order, t.nombre
                """)
                .getResultList();
    }
}
