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
     * <p>Native query con CAST explícito porque {@code tour.sub_category} es un tipo enum
     * nativo de Postgres ({@code tour_subcategory_enum}) y JPQL no aplica el
     * {@link org.hibernate.annotations.ColumnTransformer} de {@link Tour#subCategory} al WHERE.
     * Fix issue #193 (2026-07-23): el JPQL previo fallaba con
     * {@code operator does not exist: tour_subcategory_enum = character varying}.</p>
     */
    @Query(value = """
            SELECT * FROM tour t
            WHERE t.status = 'accepted'
              AND t.sub_category = CAST(:subCategory AS tour_subcategory_enum)
              AND t.id <> :excludeTourId
            ORDER BY t.rating DESC NULLS LAST
            """, nativeQuery = true)
    List<Tour> findAlternativesBySubCategoryQuery(
            @Param("subCategory") String subCategory,
            @Param("excludeTourId") Integer excludeTourId,
            Pageable pageable);

    /**
     * BE-24: wrapper conveniente que convierte el enum a su value ({@code getValue()})
     * antes de pasarlo al native query.
     */
    default List<Tour> findAlternativesBySubCategory(TourSubCategoryEnum subCategory,
                                                     Integer excludeTourId, int limit) {
        String value = subCategory != null ? subCategory.getValue() : null;
        return findAlternativesBySubCategoryQuery(value, excludeTourId,
                org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)));
    }
}
