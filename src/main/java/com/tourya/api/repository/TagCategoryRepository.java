package com.tourya.api.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TagCategoryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<String> getAllCategories() {
        return entityManager
                .createNativeQuery(
                        "SELECT nombre FROM public.tag_dimensions ORDER BY display_order"
                )
                .getResultList();
    }
}
