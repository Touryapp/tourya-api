package com.tourya.api.repository;

import com.tourya.api.models.Tour;
import com.tourya.api.models.TourCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TourRepository extends JpaRepository<Tour, Integer> {

    @Query("""
            SELECT tour
            FROM Tour tour
            Where tour.provider.id = :id
            """)
    Page<Tour> findAllByProviderId(@Param("id") Integer id,  Pageable pageable);

    @Query("""
            SELECT tour
            FROM Tour tour
            Where tour.id = :id And tour.provider.id = :providerId
            """)
    Tour findTourByIdAndProviderId(@Param("id") Integer id, @Param("providerId") Integer providerId);

}
