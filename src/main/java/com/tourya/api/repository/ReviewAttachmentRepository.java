package com.tourya.api.repository;

import com.tourya.api.models.ReviewAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad ReviewAttachment.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface ReviewAttachmentRepository extends JpaRepository<ReviewAttachment, Long> {

    /**
     * Busca todos los archivos adjuntos de una reseña
     */
    List<ReviewAttachment> findByReviewId(Long reviewId);

    /**
     * Elimina todos los archivos adjuntos de una reseña
     */
    void deleteByReviewId(Long reviewId);
}

