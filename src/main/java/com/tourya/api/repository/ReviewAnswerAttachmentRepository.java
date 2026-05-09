package com.tourya.api.repository;

import com.tourya.api.models.ReviewAnswerAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad ReviewAnswerAttachment.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface ReviewAnswerAttachmentRepository extends JpaRepository<ReviewAnswerAttachment, Long> {

    /**
     * Busca todos los archivos adjuntos de una respuesta de review
     */
    List<ReviewAnswerAttachment> findByAnswerId(Long answerId);

    /**
     * Elimina todos los archivos adjuntos de una respuesta de review
     */
    void deleteByAnswerId(Long answerId);
}

