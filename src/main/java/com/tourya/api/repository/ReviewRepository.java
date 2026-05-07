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
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Review.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Busca reseñas por ID de reserva
     */
    List<Review> findByReservationId(Long reservationId);

    /**
     * Busca reseñas por ID de tour
     */
    Page<Review> findByTourId(Integer tourId, Pageable pageable);

    /**
     * Busca reseñas por ID de usuario
     */
    Page<Review> findByUserId(Integer userId, Pageable pageable);

    /**
     * Busca reseñas por estado
     */
    Page<Review> findByStatus(ReviewStatusEnum status, Pageable pageable);

    /**
     * Busca reseñas pendientes
     */
    Page<Review> findByStatusOrderByCreatedDateDesc(ReviewStatusEnum status, Pageable pageable);

    /**
     * Busca reseñas por rating
     */
    @Query("SELECT r FROM Review r WHERE r.rating = :rating")
    Page<Review> findByRating(@Param("rating") BigDecimal rating, Pageable pageable);

    /**
     * Busca reseñas con filtros múltiples
     */
    @Query("""
        SELECT r FROM Review r 
        WHERE (:tourId IS NULL OR r.tourId = :tourId)
        AND (:userId IS NULL OR r.userId = :userId)
        AND (:rating IS NULL OR r.rating >= :rating)
        AND (:status IS NULL OR r.status = :status)
        ORDER BY r.reviewDate DESC
        """)
    Page<Review> findWithFilters(
            @Param("tourId") Integer tourId,
            @Param("userId") Integer userId,
            @Param("rating") BigDecimal rating,
            @Param("status") ReviewStatusEnum status,
            Pageable pageable
    );

    /**
     * Busca reseñas con filtros múltiples incluyendo lista de tourIds (para providers)
     */
    @Query("""
        SELECT r FROM Review r 
        WHERE (:tourIds IS NULL OR r.tourId IN :tourIds)
        AND (:userId IS NULL OR r.userId = :userId)
        AND (:rating IS NULL OR r.rating >= :rating)
        AND (:status IS NULL OR r.status = :status)
        ORDER BY r.reviewDate DESC
        """)
    Page<Review> findWithFiltersAndTourIds(
            @Param("tourIds") List<Integer> tourIds,
            @Param("userId") Integer userId,
            @Param("rating") BigDecimal rating,
            @Param("status") ReviewStatusEnum status,
            Pageable pageable
    );

    /**
     * Busca reseñas con filtros múltiples SIN filtrar por userId (para Admin)
     */
    @Query("""
        SELECT r FROM Review r 
        WHERE (:tourId IS NULL OR r.tourId = :tourId)
        AND (:rating IS NULL OR r.rating >= :rating)
        AND (:status IS NULL OR r.status = :status)
        ORDER BY r.reviewDate DESC
        """)
    Page<Review> findWithFiltersForAdmin(
            @Param("tourId") Integer tourId,
            @Param("rating") BigDecimal rating,
            @Param("status") ReviewStatusEnum status,
            Pageable pageable
    );
    
    /**
     * Busca todas las reseñas sin filtros (para debugging)
     */
    Page<Review> findAll(Pageable pageable);

    /**
     * Busca reseñas pendientes de revisión
     */
    @Query("""
        SELECT r FROM Review r 
        WHERE r.status = 'PENDING'
        ORDER BY r.createdDate DESC
        """)
    Page<Review> findPendingReviews(Pageable pageable);

    /**
     * Verifica si existe una reseña para una reserva específica
     */
    boolean existsByReservationId(Long reservationId);

    /**
     * Busca reseñas por ID de item del carrito
     */
    List<Review> findByItemId(Long itemId);

    /**
     * Busca una reseña por ID de reserva
     */
    Optional<Review> findOneByReservationId(Long reservationId);
}

