package com.tourya.api.controller;

import com.tourya.api.common.PageResponse;
import com.tourya.api.models.request.CreateReviewRequest;
import com.tourya.api.models.request.UpdateReviewRequest;
import com.tourya.api.models.responses.ReviewResponse;
import com.tourya.api.services.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador REST para gestionar reseñas.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "API para gestión de reseñas")
public class ReviewController {

    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;

    /**
     * Obtiene reseñas pendientes de revisión
     */
    @GetMapping("/search/pending-reviews")
    @Operation(summary = "Obtener reseñas pendientes", description = "Obtiene todas las reseñas pendientes de revisión")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reseñas pendientes obtenida exitosamente")
    })
    public ResponseEntity<PageResponse<ReviewResponse>> getPendingReviews(
            @Parameter(description = "Tamaño de la página") @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "Número de página (0-indexed)") @RequestParam(required = false, defaultValue = "0") Integer pageNumber) {
        log.info("Getting pending reviews - pageSize: {}, pageNumber: {}", pageSize, pageNumber);
        
        PageResponse<ReviewResponse> response = reviewService.getPendingReviews(pageSize, pageNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene reseñas con filtros
     */
    @GetMapping("/search/reviews")
    @Operation(summary = "Buscar reseñas", description = "Obtiene reseñas con filtros opcionales")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reseñas obtenida exitosamente")
    })
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @Parameter(description = "Tamaño de la página") @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "Número de página (0-indexed)") @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
            @Parameter(description = "Filtrar por calificación") @RequestParam(required = false) BigDecimal rating,
            @Parameter(description = "Filtrar por ID de tour") @RequestParam(required = false) Integer tourId,
            @Parameter(description = "Filtrar por ID de usuario") @RequestParam(required = false) Integer userId) {
        log.info("Getting reviews with filters - pageSize: {}, pageNumber: {}, rating: {}, tourId: {}, userId: {}",
                pageSize, pageNumber, rating, tourId, userId);
        
        PageResponse<ReviewResponse> response = reviewService.getReviews(pageSize, pageNumber, rating, tourId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Crea una nueva reseña
     */
    @PostMapping("/save/review")
    @Operation(summary = "Crear reseña", description = "Crea una nueva reseña para una reserva")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reseña creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReviewResponse> createReview(
            @Parameter(description = "Datos de la reseña") @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {
        log.info("Creating review for reservation: {}", request.getReservationId());
        
        ReviewResponse response = reviewService.createReview(request, null, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza una reseña existente
     */
    @PatchMapping(value = "/save/review/{reviewId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar reseña", description = "Actualiza una reseña existente. Las imágenes se envían como URLs (strings) en el campo attachmentUrls")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reseña actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reseña no encontrada"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para actualizar esta reseña")
    })
    public ResponseEntity<ReviewResponse> updateReview(
            @Parameter(description = "ID de la reseña") @PathVariable String reviewId,
            @Parameter(description = "Datos de actualización") 
            @Valid @RequestBody UpdateReviewRequest request,
            Authentication authentication) {
        log.info("Updating review: {}", reviewId);
        
        Long id = extractReviewId(reviewId);
        ReviewResponse response = reviewService.updateReview(id, request, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Extrae el ID numérico del formato "REV-XXX"
     */
    private Long extractReviewId(String reviewId) {
        try {
            if (reviewId.startsWith("REV-")) {
                return Long.parseLong(reviewId.substring(4));
            }
            return Long.parseLong(reviewId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid review ID format: " + reviewId);
        }
    }
}

