package com.tourya.api.models.mapper;

import com.tourya.api.models.ReviewAnswer;
import com.tourya.api.models.request.ReviewAnswerRequest;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre entidades ReviewAnswer y DTOs.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Component
public class ReviewAnswerMapper {

    /**
     * Convierte un ReviewAnswerRequest a entidad ReviewAnswer
     */
    public ReviewAnswer toEntity(ReviewAnswerRequest request, Long reviewId, Integer userId) {
        if (request == null) {
            return null;
        }

        return ReviewAnswer.builder()
                .reviewId(reviewId)
                .comment(request.getComment())
                .providerName(request.getProviderName())
                .providerImage(request.getProviderImage())
                .date(request.getDate() != null ? request.getDate() : java.time.LocalDate.now())
                .likes(request.getLikes() != null ? request.getLikes() : 0)
                .dislikes(request.getDislikes() != null ? request.getDislikes() : 0)
                .hearts(request.getHearts() != null ? request.getHearts() : 0)
                .createdBy(userId)
                .createdDate(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * Actualiza una entidad ReviewAnswer con datos de un ReviewAnswerRequest
     */
    public void updateEntity(ReviewAnswer answer, ReviewAnswerRequest request) {
        if (answer == null || request == null) {
            return;
        }

        if (request.getComment() != null) {
            answer.setComment(request.getComment());
        }
        if (request.getProviderName() != null) {
            answer.setProviderName(request.getProviderName());
        }
        if (request.getProviderImage() != null) {
            answer.setProviderImage(request.getProviderImage());
        }
        if (request.getDate() != null) {
            answer.setDate(request.getDate());
        }
        if (request.getLikes() != null) {
            answer.setLikes(request.getLikes());
        }
        if (request.getDislikes() != null) {
            answer.setDislikes(request.getDislikes());
        }
        if (request.getHearts() != null) {
            answer.setHearts(request.getHearts());
        }
    }
}

