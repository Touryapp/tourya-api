package com.tourya.api.repository;

import com.tourya.api.models.TourCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TourCategoryRepository extends JpaRepository<TourCategory, Integer> {
    @Query("""
            SELECT tourCategory
            FROM TourCategory tourCategory
            """)
    Page<TourCategory> findAll(Pageable pageable);

    @Query("""
            SELECT tourCategory
            FROM TourCategory tourCategory
            """)
    List<TourCategory> getAllTourCategoryList();
}
