package com.tourya.api.controller;

import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.ReviewStatusEnum;
import com.tourya.api.models.request.CreateReviewRequest;
import com.tourya.api.models.request.CreateReviewMultipartDoc;
import com.tourya.api.models.request.UpdateReviewRequest;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.models.responses.ReviewResponse;
import com.tourya.api.models.responses.ReviewReasonCatalogResponse;
import com.tourya.api.models.responses.ReviewReasonListResponse;
import com.tourya.api.models.responses.TourReviewSummaryResponse;
import com.tourya.api.services.ReviewReasonCatalogService;
import com.tourya.api.services.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
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
    private final ReviewReasonCatalogService reviewReasonCatalogService;
    private final ObjectMapper objectMapper;

    /**
     * Obtiene reservas entregadas (DELIVERED) del cliente autenticado que aún no tienen review asociada
     * Requiere autenticación mediante token Bearer en el header Authorization
     */
    @GetMapping("/search/pending-reviews")
    @Operation(summary = "Obtener reservas pendientes de review", description = "Obtiene las reservas del cliente autenticado que ya fueron entregadas (DELIVERED) pero aún no tienen una reseña asociada. Requiere autenticación.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas pendientes de review obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado - se requiere token Bearer")
    })
    public ResponseEntity<PageResponse<ReservationResponse>> getPendingReviews(
            @Parameter(description = "Tamaño de la página", required = true) @RequestParam(required = true) Integer pageSize,
            @Parameter(description = "Número de página (0-indexed)", required = true) @RequestParam(required = true) Integer pageNumber,
            @Nullable Authentication authentication) {
        log.info("Getting reservations delivered without review for authenticated user - pageSize: {}, pageNumber: {}", pageSize, pageNumber);
        
        PageResponse<ReservationResponse> response = reviewService.getPendingReviews(pageSize, pageNumber, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene reseñas con filtros basado en el rol del usuario autenticado
     * - Cliente: solo sus propias reviews
     * - Proveedor: reviews de sus tours
     * - Admin: todas las reviews
     * REQUIERE AUTENTICACIÓN
     */
    @GetMapping("/search/reviews")
    @Operation(summary = "Buscar reseñas", description = "Obtiene reseñas. El filtrado se realiza automáticamente según el rol del usuario autenticado (Cliente/Proveedor/Admin). Requiere autenticación.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reseñas obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado - se requiere token Bearer")
    })
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @Parameter(description = "Tamaño de la página", required = true) @RequestParam(required = true) Integer pageSize,
            @Parameter(description = "Número de página (0-indexed)", required = true) @RequestParam(required = true) Integer pageNumber,
            @Parameter(description = "Filtrar por calificación mínima") @RequestParam(required = false) BigDecimal rating,
            @Parameter(description = "Filtrar por ID de tour (solo para admin)") @RequestParam(required = false) Integer tourId,
            @Parameter(description = "Filtrar por estado de la reseña (PENDING, PUBLISHED, CANCELED)") @RequestParam(required = false) ReviewStatusEnum status,
            @Nullable Authentication authentication) {
        log.info("Getting reviews with filters - pageSize: {}, pageNumber: {}, rating: {}, tourId: {}, status: {}",
                pageSize, pageNumber, rating, tourId, status);
        
        PageResponse<ReviewResponse> response = reviewService.getReviews(pageSize, pageNumber, rating, tourId, status, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Crea una nueva reseña
     */
    @PostMapping(value = "/save/review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Crear reseña",
            description = "Multipart: parte `reviewData` = JSON (`CreateReviewRequest`) y opcionalmente `files` (hasta 5 imágenes). "
                    + "Motivos: solo **un** `reasonId` opcional (1–7), coherente con el catálogo de motivos. "
                    + "Requiere Bearer token."
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = CreateReviewMultipartDoc.class),
                    examples = @ExampleObject(
                            name = "multipartExample",
                            summary = "Ejemplo de reviewData (JSON) dentro del multipart",
                            value = """
                                    {
                                      "reviewData": {
                                        "reservationId": 214,
                                        "rating": 5.0,
                                        "comment": { "es": "Excelente experiencia", "en": "", "pt": "" },
                                        "reasonId": 3,
                                        "date": "2026-05-08"
                                      },
                                      "files": [
                                        "<BINARY_FILE_1>",
                                        "<BINARY_FILE_2>"
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reseña creada exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado - se requiere token Bearer"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o más de 5 imágenes"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReviewResponse> createReview(
            @Parameter(
                    description = "JSON string con el cuerpo de CreateReviewRequest (reservationId, rating, comment, reasonId opcional único, date opcional).",
                    schema = @Schema(
                            type = "string",
                            format = "json",
                            example = "{\"reservationId\":214,\"rating\":5.0,\"comment\":{\"es\":\"Excelente experiencia\",\"en\":\"\",\"pt\":\"\"},\"reasonId\":3,\"date\":\"2026-05-08\"}"
                    )
            )
            @RequestPart("reviewData") String reviewDataJson,
            @Parameter(description = "Imágenes de la reseña (máximo 5)") @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @Nullable Authentication authentication) throws IOException {
        log.info("Creating review with {} images", files != null ? files.size() : 0);
        
        CreateReviewRequest request = objectMapper.readValue(reviewDataJson, CreateReviewRequest.class);
        ReviewResponse response = reviewService.createReview(request, files, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza una reseña existente
     */
    @PatchMapping(value = "/save/review/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Actualizar reseña", description = "Actualiza una reseña existente. Las imágenes van en el answer (máximo 5). Requiere autenticación mediante token Bearer en el header Authorization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reseña actualizada exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado - se requiere token Bearer"),
            @ApiResponse(responseCode = "404", description = "Reseña no encontrada"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para actualizar esta reseña"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o más de 5 imágenes para answer")
    })
    public ResponseEntity<ReviewResponse> updateReview(
            @Parameter(description = "ID de la reseña") @PathVariable Long reviewId,
            @Parameter(description = "Datos de actualización en JSON") @RequestPart("reviewData") String reviewDataJson,
            @Parameter(description = "Imágenes del answer (máximo 5)") @RequestPart(value = "answerFiles", required = false) List<MultipartFile> answerFiles,
            @Nullable Authentication authentication) throws IOException {
        log.info("Updating review: {} with {} answer images", 
                reviewId, answerFiles != null ? answerFiles.size() : 0);
        
        UpdateReviewRequest request = objectMapper.readValue(reviewDataJson, UpdateReviewRequest.class);
        ReviewResponse response = reviewService.updateReview(reviewId, request, answerFiles, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tour/{tourId}/reviews/summary")
    @Operation(summary = "Resumen de reseñas del tour", description = "Promedio (1 decimal) + conteo por estrellas (1..5) de reseñas publicadas.")
    public ResponseEntity<TourReviewSummaryResponse> getTourReviewSummary(@PathVariable Integer tourId) {
        return ResponseEntity.ok(reviewService.getTourReviewSummary(tourId));
    }

    @GetMapping("/tour/{tourId}/reviews")
    @Operation(summary = "Reseñas publicadas por tour (filtrar por estrellas)", description = "Retorna reseñas publicadas del tour. Si se envía stars=3, retorna solo reseñas de 3 estrellas.")
    public ResponseEntity<PageResponse<ReviewResponse>> getTourReviews(
            @PathVariable Integer tourId,
            @RequestParam(required = true) Integer pageSize,
            @RequestParam(required = true) Integer pageNumber,
            @RequestParam(required = false) Integer stars
    ) {
        return ResponseEntity.ok(reviewService.getPublishedReviewsForTourByStars(tourId, stars, pageSize, pageNumber));
    }

    @GetMapping("/review/reasons")
    @Operation(summary = "Catálogo de motivos de reseña", description = "Devuelve los motivos positivos y negativos (IDs 1..7 por tipo).")
    public ResponseEntity<ReviewReasonCatalogResponse> getReviewReasonsCatalog() {
        return ResponseEntity.ok(reviewReasonCatalogService.getCatalog());
    }

    @GetMapping("/review/reasons/by-rating")
    @Operation(summary = "Motivos sugeridos según rating", description = "Si rating es 4 o 5 devuelve motivos positivos; si es 1 a 3 devuelve negativos.")
    public ResponseEntity<ReviewReasonListResponse> getReviewReasonsByRating(
            @RequestParam java.math.BigDecimal rating
    ) {
        return ResponseEntity.ok(reviewReasonCatalogService.getReasonsForRating(rating));
    }
}

