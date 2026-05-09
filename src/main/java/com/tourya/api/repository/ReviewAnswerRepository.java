package com.tourya.api.repository;

import com.tourya.api.models.ReviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad ReviewAnswer.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface ReviewAnswerRepository extends JpaRepository<ReviewAnswer, Long> {

    /**
     * Busca una respuesta por ID de reseña
     */
    Optional<ReviewAnswer> findByReviewId(Long reviewId);

    /**
     * Elimina una respuesta por ID de reseña
     */
    void deleteByReviewId(Long reviewId);
}

