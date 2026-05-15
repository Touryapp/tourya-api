package com.tourya.api.models.mapper;

import com.tourya.api.models.*;
import com.tourya.api.models.request.CreateReviewRequest;
import com.tourya.api.models.request.UpdateReviewRequest;
import com.tourya.api.models.responses.ReviewAnswerResponse;
import com.tourya.api.models.responses.ReviewResponse;
import com.tourya.api.constans.enums.ReviewReasonTypeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidades Review y DTOs.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Component
public class ReviewMapper {

    /**
     * Convierte un CreateReviewRequest a entidad Review
     */
    public Review toEntity(CreateReviewRequest request, Long itemId, Integer tourId, Integer userId) {
        if (request == null) {
            return null;
        }

        Review review = Review.builder()
                .reservationId(request.getReservationId())
                .itemId(itemId)
                .tourId(tourId)
                .userId(userId)
                .rating(request.getRating())
                .comment(request.getComment())
                .reviewDate(request.getDate() != null ? request.getDate() : LocalDate.now())
                .status(com.tourya.api.constans.enums.ReviewStatusEnum.PUBLISHED)
                .likes(0)
                .dislikes(0)
                .hearts(0)
                .build();

        if (request.getReasonId() != null) {
            review.setReasonId(request.getReasonId());
            review.setReasonType(inferReasonTypeFromRating(request.getRating()));
        }

        return review;
    }

    private ReviewReasonTypeEnum inferReasonTypeFromRating(java.math.BigDecimal rating) {
        if (rating == null) return null;
        return rating.compareTo(new java.math.BigDecimal("4.0")) >= 0
                ? ReviewReasonTypeEnum.POSITIVE
                : ReviewReasonTypeEnum.NEGATIVE;
    }

    /**
     * Convierte una entidad Review a ReviewResponse
     */
    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }

        // Obtener información del tour
        String tourName = null;
        String tourImage = null;
        if (review.getTour() != null) {
            Tour tour = review.getTour();
            if (tour.getName() != null && tour.getName().getEs() != null) {
                tourName = tour.getName().getEs();
            }
            // La imagen del tour se obtendrá desde el servicio
        }

        // Obtener información del usuario/customer
        String customerName = null;
        String customerImage = null;
        if (review.getUser() != null) {
            User user = review.getUser();
            customerName = user.fullName();
            // La imagen del usuario se puede obtener de otra fuente si existe
        }

        // Calcular días transcurridos
        String daysAgo = calculateDaysAgo(review.getReviewDate());

        // Mapear answer
        ReviewAnswerResponse answerResponse = null;
        if (review.getAnswer() != null) {
            answerResponse = mapAnswerToResponse(review.getAnswer());
        }

        // bookingId con prefijo TB- según el mock
        String bookingId = review.getReservationId() != null ? "TB-" + review.getReservationId() : null;
        
        return ReviewResponse.builder()
                .id(review.getId())
                .tourName(tourName)
                .tourId(review.getTourId())
                .tourImage(tourImage)
                .customerName(customerName)
                .customerImage(customerImage)
                .rating(review.getRating())
                .comment(review.getComment())
                .date(review.getReviewDate())
                .daysAgo(daysAgo)
                .likes(review.getLikes())
                .dislikes(review.getDislikes())
                .hearts(review.getHearts())
                .bookingId(bookingId)
                .status(review.getStatus())
                .rejectionReason(review.getRejectionReason())
                .reasonType(review.getReasonType())
                .reasonId(review.getReasonId())
                .answer(answerResponse)
                .attachmentUrls(mapAttachmentsToUrls(review.getAttachments()))
                .build();
    }

    /**
     * Actualiza una entidad Review con datos de un UpdateReviewRequest
     */
    public void updateEntity(Review review, UpdateReviewRequest request) {
        if (review == null || request == null) {
            return;
        }

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }
        if (request.getLikes() != null) {
            review.setLikes(request.getLikes());
        }
        if (request.getDislikes() != null) {
            review.setDislikes(request.getDislikes());
        }
        if (request.getHearts() != null) {
            review.setHearts(request.getHearts());
        }
        if (request.getStatus() != null) {
            review.setStatus(request.getStatus());
        }
        if (request.getRejectionReason() != null) {
            review.setRejectionReason(request.getRejectionReason());
        }
    }

    /**
     * Calcula los días transcurridos desde una fecha hasta hoy
     */
    private String calculateDaysAgo(LocalDate date) {
        if (date == null) {
            return "unknown";
        }

        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        
        if (days == 0) {
            return "today";
        } else if (days == 1) {
            return "1 day ago";
        } else {
            return days + " days ago";
        }
    }

    /**
     * Mapea ReviewAnswer entity a ReviewAnswerResponse
     */
    private ReviewAnswerResponse mapAnswerToResponse(ReviewAnswer answer) {
        if (answer == null) {
            return null;
        }

        // Calcular daysAgo
        String daysAgo = null;
        if (answer.getDate() != null) {
            daysAgo = calculateDaysAgo(answer.getDate());
        }

        return ReviewAnswerResponse.builder()
                .answerId(answer.getAnswerId())
                .comment(answer.getComment())
                .providerName(answer.getProviderName())
                .providerImage(answer.getProviderImage())
                .date(answer.getDate())
                .daysAgo(daysAgo)
                .likes(answer.getLikes())
                .dislikes(answer.getDislikes())
                .hearts(answer.getHearts())
                .attachmentUrls(mapAnswerAttachmentsToUrls(answer.getAttachments()))
                .build();
    }

    /**
     * Mapea la lista de attachments a URLs
     */
    private List<String> mapAttachmentsToUrls(List<ReviewAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return new ArrayList<>();
        }
        return attachments.stream()
                .map(ReviewAttachment::getFileUrl)
                .collect(Collectors.toList());
    }

    /**
     * Mapea la lista de attachments de answer a URLs
     */
    private List<String> mapAnswerAttachmentsToUrls(List<com.tourya.api.models.ReviewAnswerAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return new ArrayList<>();
        }
        return attachments.stream()
                .map(com.tourya.api.models.ReviewAnswerAttachment::getFileUrl)
                .collect(Collectors.toList());
    }
}

