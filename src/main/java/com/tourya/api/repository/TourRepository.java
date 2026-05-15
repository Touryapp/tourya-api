package com.tourya.api.repository;

import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
            Where tour.provider.id = :id
            """)
    List<Tour> findAllByProviderId(@Param("id") Integer id);

    @Query("""
            SELECT tour
            FROM Tour tour
            Where tour.id = :id And tour.provider.id = :providerId
            """)
    Tour findTourByIdAndProviderId(@Param("id") Integer id, @Param("providerId") Integer providerId);

    @Query("""
            SELECT tour
            FROM Tour tour
            WHERE ((:status IS NULL ) OR (tour.status = :status))
            """)
    Page<Tour> findAllTour(@Param("status") TourStatusEnum status, Pageable pageable);

    Optional<Tour> findTourByIdAndStatus(Integer tourId, TourStatusEnum tourStatusEnum);

    /**
     * Rating persistido en {@code tour.rating} (denormalizado); usado como respaldo cuando aún no hay AVG en reseñas publicadas.
     */
    @Query("SELECT t.id, t.rating FROM Tour t WHERE t.id IN :ids")
    List<Object[]> findIdAndRatingByTourIds(@Param("ids") Collection<Integer> ids);
}
