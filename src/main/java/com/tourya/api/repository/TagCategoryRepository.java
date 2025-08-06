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
                        "SELECT unnest(enum_range(NULL::tour_tag_category_enum))::TEXT AS category"
                )
                .getResultList();
    }
}
