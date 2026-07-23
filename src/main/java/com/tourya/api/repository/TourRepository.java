package com.tourya.api.repository;

import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.constans.enums.TourSubCategoryEnum;
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

    /**
     * BE-24 (RN-055): tours alternativos para el email de cancelación por decline del provider.
     * Devuelve hasta {@code limit} tours ACCEPTED con la misma subcategoria (excluyendo el
     * tour cancelado) ordenados por rating descendente.
     *
     * <p>No filtra por disponibilidad futura para no complicar el JPQL — el email solo
     * muestra "revisa el catálogo" y el turista al hacer click ve los schedules disponibles.</p>
     */
    @Query(value = """
            SELECT t FROM Tour t
            WHERE t.status = 'accepted'
              AND t.subCategory = :subCategory
              AND t.id <> :excludeTourId
            ORDER BY t.rating DESC NULLS LAST
            """)
    List<Tour> findAlternativesBySubCategoryQuery(
            @Param("subCategory") TourSubCategoryEnum subCategory,
            @Param("excludeTourId") Integer excludeTourId,
            Pageable pageable);

    /**
     * BE-24: wrapper conveniente con {@code Pageable} armado internamente.
     */
    default List<Tour> findAlternativesBySubCategory(TourSubCategoryEnum subCategory,
                                                     Integer excludeTourId, int limit) {
        return findAlternativesBySubCategoryQuery(subCategory, excludeTourId,
                org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)));
    }
}
