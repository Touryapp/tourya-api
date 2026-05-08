package com.tourya.api.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class TourTagsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Integer> getTagIdsByTourId(Integer tourId) {
        if (tourId == null) return Collections.emptyList();

        @SuppressWarnings("unchecked")
        List<Number> rows = entityManager.createNativeQuery("""
                SELECT tt.tag_id
                FROM public.tour_tags tt
                WHERE tt.tour_id = :tourId
                ORDER BY tt.tag_id
                """)
                .setParameter("tourId", tourId)
                .getResultList();

        return rows.stream().map(Number::intValue).toList();
    }

    public void replaceTourTags(Integer tourId, List<Integer> tagIds) {
        if (tourId == null) return;

        entityManager.createNativeQuery("""
                DELETE FROM public.tour_tags
                WHERE tour_id = :tourId
                """)
                .setParameter("tourId", tourId)
                .executeUpdate();

        if (tagIds == null || tagIds.isEmpty()) return;

        for (Integer tagId : tagIds) {
            if (tagId == null) continue;
            entityManager.createNativeQuery("""
                    INSERT INTO public.tour_tags (tour_id, tag_id)
                    VALUES (:tourId, :tagId)
                    ON CONFLICT (tour_id, tag_id) DO NOTHING
                    """)
                    .setParameter("tourId", tourId)
                    .setParameter("tagId", tagId)
                    .executeUpdate();
        }
    }
}

