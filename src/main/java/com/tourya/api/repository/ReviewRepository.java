package com.tourya.api.repository;

import com.tourya.api.constans.enums.ReviewStatusEnum;
import com.tourya.api.models.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Review.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByReservationId(Long reservationId);

    Page<Review> findByTourId(Integer tourId, Pageable pageable);

    Page<Review> findByUserId(Integer userId, Pageable pageable);

    Page<Review> findByStatus(ReviewStatusEnum status, Pageable pageable);

    Page<Review> findByStatusOrderByCreatedDateDesc(ReviewStatusEnum status, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.rating = :rating")
    Page<Review> findByRating(@Param("rating") BigDecimal rating, Pageable pageable);

    @Query("""
        SELECT r FROM Review r
        LEFT JOIN r.user u
        WHERE (:tourId IS NULL OR r.tourId = :tourId)
        AND (:userId IS NULL OR r.userId = :userId)
        AND (:minRating IS NULL OR r.rating >= :minRating)
        AND (:maxRating IS NULL OR r.rating < :maxRating)
        AND (:status IS NULL OR r.status = :status)
        AND (:customerNamePattern IS NULL OR LOWER(CONCAT(COALESCE(u.firstname, ''), ' ', COALESCE(u.lastname, ''))) LIKE :customerNamePattern)
        ORDER BY r.reviewDate DESC
        """)
    Page<Review> findWithFilters(
            @Param("tourId") Integer tourId,
            @Param("userId") Integer userId,
            @Param("minRating") BigDecimal minRating,
            @Param("maxRating") BigDecimal maxRating,
            @Param("status") ReviewStatusEnum status,
            @Param("customerNamePattern") String customerNamePattern,
            Pageable pageable
    );

    @Query("""
        SELECT r FROM Review r
        LEFT JOIN r.user u
        WHERE (:tourIds IS NULL OR r.tourId IN :tourIds)
        AND (:userId IS NULL OR r.userId = :userId)
        AND (:minRating IS NULL OR r.rating >= :minRating)
        AND (:maxRating IS NULL OR r.rating < :maxRating)
        AND (:status IS NULL OR r.status = :status)
        AND (:customerNamePattern IS NULL OR LOWER(CONCAT(COALESCE(u.firstname, ''), ' ', COALESCE(u.lastname, ''))) LIKE :customerNamePattern)
        ORDER BY r.reviewDate DESC
        """)
    Page<Review> findWithFiltersAndTourIds(
            @Param("tourIds") List<Integer> tourIds,
            @Param("userId") Integer userId,
            @Param("minRating") BigDecimal minRating,
            @Param("maxRating") BigDecimal maxRating,
            @Param("status") ReviewStatusEnum status,
            @Param("customerNamePattern") String customerNamePattern,
            Pageable pageable
    );

    @Query("""
        SELECT r FROM Review r
        LEFT JOIN r.user u
        WHERE (:tourId IS NULL OR r.tourId = :tourId)
        AND (:minRating IS NULL OR r.rating >= :minRating)
        AND (:maxRating IS NULL OR r.rating < :maxRating)
        AND (:status IS NULL OR r.status = :status)
        AND (:customerNamePattern IS NULL OR LOWER(CONCAT(COALESCE(u.firstname, ''), ' ', COALESCE(u.lastname, ''))) LIKE :customerNamePattern)
        ORDER BY r.reviewDate DESC
        """)
    Page<Review> findWithFiltersForAdmin(
            @Param("tourId") Integer tourId,
            @Param("minRating") BigDecimal minRating,
            @Param("maxRating") BigDecimal maxRating,
            @Param("status") ReviewStatusEnum status,
            @Param("customerNamePattern") String customerNamePattern,
            Pageable pageable
    );

    Page<Review> findAll(Pageable pageable);

    @Query("""
        SELECT r FROM Review r
        WHERE r.status = 'PENDING'
        ORDER BY r.createdDate DESC
        """)
    Page<Review> findPendingReviews(Pageable pageable);

    boolean existsByReservationId(Long reservationId);

    List<Review> findByItemId(Long itemId);

    Optional<Review> findOneByReservationId(Long reservationId);

    @Query("""
            SELECT AVG(r.rating)
            FROM Review r
            WHERE r.tourId = :tourId
              AND r.status = com.tourya.api.constans.enums.ReviewStatusEnum.PUBLISHED
            """)
    BigDecimal avgPublishedRatingByTourId(@Param("tourId") Integer tourId);

    @Query("""
            SELECT r.tourId, AVG(r.rating)
            FROM Review r
            WHERE r.tourId IN :tourIds
              AND r.status = com.tourya.api.constans.enums.ReviewStatusEnum.PUBLISHED
            GROUP BY r.tourId
            """)
    List<Object[]> avgPublishedRatingGroupedByTourIds(@Param("tourIds") Collection<Integer> tourIds);

    @Query("""
            SELECT COUNT(r)
            FROM Review r
            WHERE r.tourId = :tourId
              AND r.status = com.tourya.api.constans.enums.ReviewStatusEnum.PUBLISHED
            """)
    long countPublishedByTourId(@Param("tourId") Integer tourId);

    @Query(value = """
            SELECT CAST(FLOOR(rating) AS int) AS stars, COUNT(*) AS cnt
            FROM public.review
            WHERE tour_id = :tourId
              AND status = 'PUBLISHED'
            GROUP BY CAST(FLOOR(rating) AS int)
            """, nativeQuery = true)
    List<Object[]> countPublishedByStars(@Param("tourId") Integer tourId);

    @Query("""
            SELECT r
            FROM Review r
            WHERE r.tourId = :tourId
              AND r.status = com.tourya.api.constans.enums.ReviewStatusEnum.PUBLISHED
              AND r.rating >= :minRating
              AND r.rating < :maxRating
            ORDER BY r.reviewDate DESC
            """)
    Page<Review> findPublishedByTourIdAndStars(
            @Param("tourId") Integer tourId,
            @Param("minRating") BigDecimal minRating,
            @Param("maxRating") BigDecimal maxRating,
            Pageable pageable
    );
}
