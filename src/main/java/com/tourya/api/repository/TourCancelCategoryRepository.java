package com.tourya.api.repository;

import com.tourya.api.models.TourCancelCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TourCancelCategoryRepository extends JpaRepository<TourCancelCategory, Integer> {
    TourCancelCategory findByName(String name);

    @Query("""
            SELECT tourCancelCategory
            FROM TourCancelCategory tourCancelCategory
            """)
    List<TourCancelCategory> getAllTourCancelCategoryList();
}
