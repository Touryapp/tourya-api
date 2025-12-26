package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.ReviewStatusEnum;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.ReviewMapper;
import com.tourya.api.models.mapper.ReviewAnswerMapper;
import com.tourya.api.models.request.CreateReviewRequest;
import com.tourya.api.models.request.UpdateReviewRequest;
import com.tourya.api.models.responses.ReviewResponse;
import com.tourya.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar reseñas.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewAttachmentRepository reviewAttachmentRepository;
    private final ReviewAnswerRepository reviewAnswerRepository;
    private final ReservationRepository reservationRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final TourGalleryRepository tourGalleryRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewAnswerMapper reviewAnswerMapper;
    private final S3Service s3Service;
    private final com.tourya.api.config.security.JwtService jwtService;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    /**
     * Crea una nueva reseña
     */
    public ReviewResponse createReview(CreateReviewRequest request, List<MultipartFile> files, Authentication authentication) {
        log.info("Creating review for reservation: {}", request.getReservationId());

        // Obtener el usuario autenticado
        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            throw new IllegalStateException("Authentication is required to create a review");
        }

        // Obtener la reserva
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + request.getReservationId()));

        // Obtener el item del carrito
        ShoppingCartItem cartItem = shoppingCartItemRepository.findById(reservation.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Shopping cart item not found with id: " + reservation.getItemId()));

        // Obtener el tour desde el schedule
        Integer tourId = null;
        if (cartItem.getTourSchedule() != null && cartItem.getTourSchedule().getTourId() != null) {
            tourId = cartItem.getTourSchedule().getTourId();
        } else {
            // Si no hay schedule, usar productId
            tourId = cartItem.getProductId();
        }

        // Verificar que no exista ya una reseña para esta reserva
        if (reviewRepository.existsByReservationId(reservation.getReservationId())) {
            throw new IllegalStateException("A review already exists for this reservation");
        }

        // Crear la reseña
        Review review = reviewMapper.toEntity(request, cartItem.getId(), tourId, user.getId());
        review.setCreatedBy(user.getId());
        review.setCreatedDate(LocalDateTime.now());

        // Guardar la reseña
        review = reviewRepository.save(review);
        
        // Los archivos adjuntos no se permiten en la creación inicial de la reseña

        // Cargar relaciones para la respuesta
        review = loadReviewRelations(review);

        ReviewResponse response = reviewMapper.toResponse(review);
        return enrichReviewResponse(response);
    }

    /**
     * Obtiene reseñas con filtros
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviews(
            Integer pageSize,
            Integer pageNumber,
            BigDecimal rating,
            Integer tourId,
            Integer userId) {
        log.info("Getting reviews with filters - pageSize: {}, pageNumber: {}, rating: {}, tourId: {}, userId: {}",
                pageSize, pageNumber, rating, tourId, userId);

        Pageable pageable = PageRequest.of(pageNumber != null ? pageNumber : 0, pageSize != null ? pageSize : 10);
        
        // No filtrar por status, mostrar todas las reviews
        Page<Review> reviewsPage = reviewRepository.findWithFilters(tourId, userId, rating, pageable);

        List<ReviewResponse> responses = reviewsPage.getContent().stream()
                .map(this::loadReviewRelations)
                .map(reviewMapper::toResponse)
                .map(this::enrichReviewResponse)
                .collect(Collectors.toList());

        return PageResponse.<ReviewResponse>builder()
                .content(responses)
                .number(reviewsPage.getNumber())
                .size(reviewsPage.getSize())
                .totalElements(reviewsPage.getTotalElements())
                .totalPages(reviewsPage.getTotalPages())
                .first(reviewsPage.isFirst())
                .last(reviewsPage.isLast())
                .build();
    }

    /**
     * Obtiene reseñas pendientes de revisión
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getPendingReviews(Integer pageSize, Integer pageNumber) {
        log.info("Getting pending reviews - pageSize: {}, pageNumber: {}", pageSize, pageNumber);

        Pageable pageable = PageRequest.of(pageNumber != null ? pageNumber : 0, pageSize != null ? pageSize : 10);
        Page<Review> reviewsPage = reviewRepository.findPendingReviews(pageable);

        List<ReviewResponse> responses = reviewsPage.getContent().stream()
                .map(this::loadReviewRelations)
                .map(reviewMapper::toResponse)
                .map(this::enrichReviewResponse)
                .collect(Collectors.toList());

        return PageResponse.<ReviewResponse>builder()
                .content(responses)
                .number(reviewsPage.getNumber())
                .size(reviewsPage.getSize())
                .totalElements(reviewsPage.getTotalElements())
                .totalPages(reviewsPage.getTotalPages())
                .first(reviewsPage.isFirst())
                .last(reviewsPage.isLast())
                .build();
    }

    /**
     * Actualiza una reseña
     */
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request, Authentication authentication) {
        log.info("Updating review: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Obtener usuario autenticado
        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            throw new IllegalStateException("Authenticated user not found.");
        }

        // Verificar permisos (solo el creador o admin puede actualizar)
        if (!review.getCreatedBy().equals(user.getId()) && !Utils.isAdmin(user.getRoles())) {
            throw new IllegalStateException("You don't have permission to update this review");
        }

        // Actualizar campos
        reviewMapper.updateEntity(review, request);
        review.setLastModifiedBy(user.getId());
        review.setLastModifiedDate(LocalDateTime.now());

        // Asegurar que la colección de attachments esté inicializada antes de guardar
        if (review.getAttachments() == null) {
            review.setAttachments(new ArrayList<>());
        }
        
        review = reviewRepository.save(review);

        // Manejar answer si viene en el request (replica del proveedor)
        if (request.getAnswer() != null) {
            Optional<ReviewAnswer> existingAnswer = reviewAnswerRepository.findByReviewId(reviewId);
            ReviewAnswer answer;
            
            if (existingAnswer.isPresent()) {
                // Actualizar answer existente
                answer = existingAnswer.get();
                reviewAnswerMapper.updateEntity(answer, request.getAnswer());
                answer.setLastModifiedBy(user.getId());
                answer.setLastModifiedDate(LocalDateTime.now());
            } else {
                // Crear nuevo answer
                answer = reviewAnswerMapper.toEntity(request.getAnswer(), reviewId, user.getId());
                answer.setCreatedBy(user.getId());
                answer.setCreatedDate(LocalDateTime.now());
            }
            
            // Guardar el answer
            answer = reviewAnswerRepository.save(answer);
        }

        // Cargar relaciones para la respuesta
        review = loadReviewRelations(review);

        ReviewResponse response = reviewMapper.toResponse(review);
        return enrichReviewResponse(response);
    }

    /**
     * Obtiene el usuario desde el objeto Authentication.
     * Maneja tanto cuando el principal es un User como cuando es un String (email).
     */
    private User getUserFromAuthentication(@org.springframework.lang.Nullable Authentication authentication) {
        Authentication auth = authentication;
        if (auth == null) {
            // Intentar obtener desde SecurityContextHolder
            auth = SecurityContextHolder.getContext().getAuthentication();
        }
        
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        
        Object principal = auth.getPrincipal();
        
        // Si el principal es un User, retornarlo directamente
        if (principal instanceof User) {
            return (User) principal;
        }
        
        // Si el principal es un String (email), buscar el usuario por email
        if (principal instanceof String) {
            String emailOrAnonymous = (String) principal;
            
            // Si es "anonymousUser", intentar procesar el token JWT del header
            if ("anonymousUser".equals(emailOrAnonymous)) {
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        String authHeader = request.getHeader("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            String jwt = authHeader.substring(7);
                            String userEmail = jwtService.extractUsername(jwt);
                            if (userEmail != null) {
                                org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                                if (jwtService.isTokenValid(jwt, userDetails) && userDetails instanceof User) {
                                    return (User) userDetails;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error processing JWT token from header: {}", e.getMessage());
                }
                throw new IllegalStateException("Valid authentication is required. Please provide a valid Bearer token in the Authorization header.");
            }
            
            // Buscar el usuario por email
            return userRepository.findByEmail(emailOrAnonymous)
                    .orElseThrow(() -> new IllegalStateException("User not found with email: " + emailOrAnonymous));
        }
        
        return null;
    }

    /**
     * Carga las relaciones necesarias de una reseña
     * @param review La reseña a cargar
     * @param reloadAttachments Si true, recarga los attachments desde la BD. Si false, solo carga si están vacíos.
     */
    private Review loadReviewRelations(Review review) {
        return loadReviewRelations(review, true);
    }

    /**
     * Carga las relaciones necesarias de una reseña
     * @param review La reseña a cargar
     * @param reloadAttachments Si true, recarga los attachments desde la BD. Si false, solo carga si están vacíos.
     */
    private Review loadReviewRelations(Review review, boolean reloadAttachments) {
        // Cargar tour
        if (review.getTourId() != null) {
            Tour tour = tourRepository.findById(review.getTourId()).orElse(null);
            if (tour != null) {
                review.setTour(tour);
            }
        }

        // Cargar user
        if (review.getUserId() != null) {
            User user = userRepository.findById(review.getUserId()).orElse(null);
            if (user != null) {
                review.setUser(user);
            }
        }

        // Cargar reservation para obtener paymentId (necesario para bookingId)
        if (review.getReservationId() != null) {
            Reservation reservation = reservationRepository.findById(review.getReservationId()).orElse(null);
            if (reservation != null) {
                review.setReservation(reservation);
            }
        }

        // Cargar attachments - NO usar setAttachments() para evitar problemas con orphanRemoval
        if (reloadAttachments || review.getAttachments() == null || review.getAttachments().isEmpty()) {
            List<ReviewAttachment> attachmentsFromDb = reviewAttachmentRepository.findByReviewId(review.getId());
            if (review.getAttachments() == null) {
                review.setAttachments(new ArrayList<>());
            }
            // Limpiar y agregar los nuevos (usando clear() y addAll() en lugar de setAttachments())
            review.getAttachments().clear();
            if (!attachmentsFromDb.isEmpty()) {
                review.getAttachments().addAll(attachmentsFromDb);
            }
        }

        // Cargar answer si existe
        Optional<com.tourya.api.models.ReviewAnswer> answer = reviewAnswerRepository.findByReviewId(review.getId());
        answer.ifPresent(review::setAnswer);

        return review;
    }

    /**
     * Guarda los archivos adjuntos de una reseña desde URLs (strings)
     */
    private List<ReviewAttachment> saveAttachmentsFromUrls(Long reviewId, List<String> attachmentUrls, Integer userId) {
        List<ReviewAttachment> attachments = new ArrayList<>();

        for (String fileUrl : attachmentUrls) {
            if (fileUrl != null && !fileUrl.isEmpty()) {
                // Extraer el nombre del archivo de la URL si es posible
                String fileName = extractFileNameFromUrl(fileUrl);
                
                ReviewAttachment attachment = ReviewAttachment.builder()
                        .reviewId(reviewId)
                        .fileUrl(fileUrl)
                        .fileName(fileName)
                        .fileType(extractContentTypeFromUrl(fileUrl))
                        .fileSize(null) // No tenemos el tamaño desde la URL
                        .createdBy(userId)
                        .createdDate(LocalDateTime.now())
                        .build();

                attachment = reviewAttachmentRepository.save(attachment);
                attachments.add(attachment);
            }
        }

        return attachments;
    }
    
    /**
     * Extrae el nombre del archivo de una URL
     */
    private String extractFileNameFromUrl(String url) {
        try {
            String path = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
            String fileName = path.substring(path.lastIndexOf("/") + 1);
            return fileName.isEmpty() ? "attachment" : fileName;
        } catch (Exception e) {
            return "attachment";
        }
    }
    
    /**
     * Intenta extraer el content type de la URL basándose en la extensión
     */
    private String extractContentTypeFromUrl(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.endsWith(".png")) {
            return "image/png";
        } else if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerUrl.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    /**
     * Actualiza el mapper para incluir la imagen del tour en la respuesta
     */
    public ReviewResponse enrichReviewResponse(ReviewResponse response) {
        if (response.getTourId() != null) {
            try {
                Integer tourId = Integer.parseInt(response.getTourId());
                List<TourGallery> galleries = tourGalleryRepository.findByTourIdOrderByOrderIndexAsc(tourId);
                if (!galleries.isEmpty()) {
                    response.setTourImage(galleries.get(0).getImageUrl());
                }

                // Obtener nombre del tour
                Tour tour = tourRepository.findById(tourId).orElse(null);
                if (tour != null && tour.getName() != null && tour.getName().getEs() != null) {
                    response.setTourName(tour.getName().getEs());
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid tourId format: {}", response.getTourId());
            }
        }
        return response;
    }
}

