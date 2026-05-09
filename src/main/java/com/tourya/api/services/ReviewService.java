package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.constans.enums.ReviewStatusEnum;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.ReviewMapper;
import com.tourya.api.models.mapper.ReviewAnswerMapper;
import com.tourya.api.models.request.CreateReviewRequest;
import com.tourya.api.models.request.UpdateReviewRequest;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.models.responses.ReviewResponse;
import com.tourya.api.models.mapper.ReservationMapper;
import com.tourya.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
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
import java.math.RoundingMode;
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
    private final ReviewAnswerAttachmentRepository reviewAnswerAttachmentRepository;
    private final ReservationRepository reservationRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final TourGalleryRepository tourGalleryRepository;
    private final PaymentRepository paymentRepository;
    private final TourAddressRepository tourAddressRepository;
    private final TourMainAttractionRepository tourMainAttractionRepository;
    private final TourIncludesExcludesRepository tourIncludesExcludesRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewAnswerMapper reviewAnswerMapper;
    private final ReservationMapper reservationMapper;
    private final ProviderService providerService;
    private final IStorageService s3Service;
    private final com.tourya.api.config.security.JwtService jwtService;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Transactional(readOnly = true)
    public com.tourya.api.models.responses.TourReviewSummaryResponse getTourReviewSummary(Integer tourId) {
        java.math.BigDecimal avg = reviewRepository.avgPublishedRatingByTourId(tourId);
        if (avg != null) {
            avg = avg.setScale(1, java.math.RoundingMode.HALF_UP);
        }
        long total = reviewRepository.countPublishedByTourId(tourId);

        java.util.Map<Integer, Long> counts = new java.util.HashMap<>();
        for (int s = 1; s <= 5; s++) counts.put(s, 0L);
        List<Object[]> rows = reviewRepository.countPublishedByStars(tourId);
        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            Integer stars = row[0] != null ? ((Number) row[0]).intValue() : null;
            Long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            if (stars != null && stars >= 1 && stars <= 5) {
                counts.put(stars, cnt);
            }
        }

        return com.tourya.api.models.responses.TourReviewSummaryResponse.builder()
                .tourId(tourId)
                .averageRating(avg)
                .totalReviews(total)
                .countByStars(counts)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getPublishedReviewsForTourByStars(Integer tourId, Integer stars, Integer pageSize, Integer pageNumber) {
        if (pageSize == null || pageNumber == null) {
            throw new IllegalArgumentException("pageSize and pageNumber are required parameters");
        }
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Review> page;
        if (stars != null) {
            if (stars < 1 || stars > 5) throw new IllegalArgumentException("stars must be between 1 and 5");
            java.math.BigDecimal min = new java.math.BigDecimal(stars).setScale(2);
            java.math.BigDecimal max = new java.math.BigDecimal(stars + 1).setScale(2);
            page = reviewRepository.findPublishedByTourIdAndStars(tourId, min, max, pageable);
        } else {
            page = reviewRepository.findWithFiltersForAdmin(tourId, null, ReviewStatusEnum.PUBLISHED, pageable);
        }

        List<ReviewResponse> responses = page.getContent().stream()
                .map(r -> loadReviewRelations(r, true))
                .map(reviewMapper::toResponse)
                .map(this::enrichReviewResponse)
                .collect(Collectors.toList());

        return PageResponse.<ReviewResponse>builder()
                .content(responses)
                .number(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * Crea una nueva reseña
     */
    public ReviewResponse createReview(CreateReviewRequest request, List<MultipartFile> files, Authentication authentication) throws IOException {
        log.info("Creating review for reservation: {}", request.getReservationId());

        // Validar máximo 5 imágenes
        if (files != null && files.size() > 5) {
            throw new IllegalStateException("Se permite un máximo de 5 imágenes por review. Se intentaron subir " + files.size() + " imágenes.");
        }

        // Obtener el usuario autenticado
        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            throw new com.tourya.api.exceptions.InsufficientPrivilegesException("Authentication is required to create a review. Please provide a valid Bearer token in the Authorization header.");
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
        
        // Guardar imágenes si se proporcionaron
        if (files != null && !files.isEmpty()) {
            saveAttachmentsFromMultipartFiles(review.getId(), files, user.getId());
        }

        // Cargar relaciones para la respuesta (recargar desde BD para incluir attachments recién guardados)
        review = reviewRepository.findById(review.getId()).orElse(review);
        review = loadReviewRelations(review, true);

        ReviewResponse response = reviewMapper.toResponse(review);
        return enrichReviewResponse(response);
    }

    /**
     * Obtiene reseñas con filtros basado en el rol del usuario autenticado
     * - Cliente: solo sus propias reviews
     * - Proveedor: reviews de sus tours
     * - Admin: todas las reviews
     * REQUIERE AUTENTICACIÓN
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviews(
            Integer pageSize,
            Integer pageNumber,
            BigDecimal rating,
            Integer tourId,
            ReviewStatusEnum status,
            @Nullable Authentication authentication) {
        log.info("Getting reviews with filters - pageSize: {}, pageNumber: {}, rating: {}, tourId: {}, status: {}",
                pageSize, pageNumber, rating, tourId, status);

        // Validar que pageSize y pageNumber sean proporcionados
        if (pageSize == null || pageNumber == null) {
            throw new IllegalArgumentException("pageSize and pageNumber are required parameters");
        }

        // Obtener usuario autenticado - REQUERIDO
        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            throw new com.tourya.api.exceptions.InsufficientPrivilegesException("Authentication is required to get reviews. Please provide a valid Bearer token in the Authorization header.");
        }
        
        // Determinar filtros según el rol del usuario
        Integer finalUserId = user.getId(); // SIEMPRE usar el userId del usuario autenticado
        List<Integer> providerTourIds = null;
        boolean isAdmin = false;
        List<Role> roles = user.getRoles();
        
        if (Utils.isAdmin(roles)) {
            // Admin: puede ver todas las reviews, usar query especial sin filtro de userId
            isAdmin = true;
            providerTourIds = null;
        } else if (Utils.isProvider(roles)) {
            // Proveedor: reviews de sus tours
            try {
                Provider provider = providerService.findByUser(user);
                if (provider != null) {
                    // Buscar todos los tours del proveedor
                    List<Tour> providerTours = tourRepository.findAllByProviderId(provider.getId());
                    if (!providerTours.isEmpty()) {
                        providerTourIds = providerTours.stream()
                                .map(Tour::getId)
                                .collect(Collectors.toList());
                    }
                    // Si no tiene tours, providerTourIds permanece null y se comportará como cliente
                }
            } catch (Exception e) {
                log.warn("Error getting provider tours: {}", e.getMessage());
                // Si hay error, providerTourIds permanece null
            }
        }
        // Si no es Provider o no tiene tours, providerTourIds es null y se filtrará solo por userId (cliente)

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        Page<Review> reviewsPage;
        if (isAdmin) {
            // Admin: usar query sin filtro de userId
            reviewsPage = reviewRepository.findWithFiltersForAdmin(tourId, rating, status, pageable);
        } else if (providerTourIds != null && !providerTourIds.isEmpty()) {
            // Proveedor con tours: usar query con lista de tourIds (no filtra por userId para ver todas las reviews de sus tours)
            reviewsPage = reviewRepository.findWithFiltersAndTourIds(
                    providerTourIds, null, rating, status, pageable);
        } else {
            // Cliente o Provider sin tours: usar query normal con filtro de userId (solo sus propias reviews)
            reviewsPage = reviewRepository.findWithFilters(tourId, finalUserId, rating, status, pageable);
        }

        List<ReviewResponse> responses = reviewsPage.getContent().stream()
                .map(review -> loadReviewRelations(review, true))
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
     * Obtiene reservas entregadas (DELIVERED) del cliente autenticado que aún no tienen review asociada
     * Requiere autenticación para filtrar las reservas del usuario
     */
    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getPendingReviews(Integer pageSize, Integer pageNumber, @Nullable Authentication authentication) {
        log.info("Getting reservations delivered without review for authenticated user - pageSize: {}, pageNumber: {}", pageSize, pageNumber);

        // Validar que pageSize y pageNumber sean proporcionados
        if (pageSize == null || pageNumber == null) {
            throw new IllegalArgumentException("pageSize and pageNumber are required parameters");
        }

        // Obtener usuario autenticado - es requerido para filtrar las reservas del cliente
        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            throw new com.tourya.api.exceptions.InsufficientPrivilegesException("Authentication is required to get pending reviews. Please provide a valid Bearer token in the Authorization header.");
        }

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Reservation> reservationsPage = reservationRepository.findDeliveredWithoutReviewByUserId(
                DeliveryStatusEnum.DELIVERED, user.getId(), pageable);

        List<ReservationResponse> responses = reservationsPage.getContent().stream()
                .map(reservation -> {
                    ReservationResponse response = reservationMapper.toResponse(reservation);
                    enrichReservationResponse(response, reservation);
                    return response;
                })
                .collect(Collectors.toList());

        return PageResponse.<ReservationResponse>builder()
                .content(responses)
                .number(reservationsPage.getNumber())
                .size(reservationsPage.getSize())
                .totalElements(reservationsPage.getTotalElements())
                .totalPages(reservationsPage.getTotalPages())
                .first(reservationsPage.isFirst())
                .last(reservationsPage.isLast())
                .build();
    }

    /**
     * Actualiza una reseña
     */
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request, List<MultipartFile> answerFiles, Authentication authentication) throws IOException {
        log.info("Updating review: {}", reviewId);

        // Las imágenes del PATCH van en el answer, no en el review
        // Validar máximo 5 imágenes para answer
        if (answerFiles != null && answerFiles.size() > 5) {
            throw new IllegalStateException("Se permite un máximo de 5 imágenes por answer. Se intentaron subir " + answerFiles.size() + " imágenes.");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        ReviewStatusEnum prevStatus = review.getStatus();
        BigDecimal prevRating = review.getRating();

        // Obtener usuario autenticado
        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            throw new com.tourya.api.exceptions.InsufficientPrivilegesException("Authentication is required to update a review. Please provide a valid Bearer token in the Authorization header.");
        }

        // Verificar permisos
        boolean hasPermission = false;
        List<Role> roles = user.getRoles();
        
        // El creador de la review puede actualizarla
        if (review.getCreatedBy().equals(user.getId())) {
            hasPermission = true;
        }
        // Un admin puede actualizar cualquier review
        else if (Utils.isAdmin(roles)) {
            hasPermission = true;
        }
        // Un provider puede responder a reviews de sus tours
        else if (Utils.isProvider(roles)) {
            try {
                Provider provider = providerService.findByUser(user);
                if (provider != null && review.getTourId() != null) {
                    Tour tour = tourRepository.findById(review.getTourId()).orElse(null);
                    if (tour != null && tour.getProvider() != null && tour.getProvider().getId().equals(provider.getId())) {
                        hasPermission = true;
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking provider permission: {}", e.getMessage());
            }
        }
        
        if (!hasPermission) {
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
        // Si cambió el estado/rating y afecta reviews publicadas, recalcular rating del tour persistido
        if (review.getTourId() != null && (request.getStatus() != null || request.getRating() != null)) {
            boolean wasPublished = prevStatus == ReviewStatusEnum.PUBLISHED;
            boolean isPublished = review.getStatus() == ReviewStatusEnum.PUBLISHED;
            boolean ratingChanged = request.getRating() != null && (prevRating == null || prevRating.compareTo(review.getRating()) != 0);
            boolean statusChanged = request.getStatus() != null && prevStatus != review.getStatus();
            if (wasPublished || isPublished || ratingChanged || statusChanged) {
                refreshAndPersistTourRating(review.getTourId());
            }
        }

        // NO se modifican las imágenes del review en PATCH - solo se pueden agregar al crear

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
            
            // Procesar imágenes del answer (answerFiles)
            if (answerFiles != null && !answerFiles.isEmpty()) {
                // Eliminar imágenes existentes del answer si hay
                List<ReviewAnswerAttachment> existingAnswerAttachments = reviewAnswerAttachmentRepository.findByAnswerId(answer.getAnswerId());
                for (ReviewAnswerAttachment attachment : existingAnswerAttachments) {
                    s3Service.deleteFile(attachment.getFileUrl());
                    reviewAnswerAttachmentRepository.delete(attachment);
                }
                // Guardar nuevas imágenes del answer
                saveAnswerAttachmentsFromMultipartFiles(answer.getAnswerId(), answerFiles, user.getId());
            }
        }

        // Cargar relaciones para la respuesta
        review = loadReviewRelations(review, true);

        ReviewResponse response = reviewMapper.toResponse(review);
        return enrichReviewResponse(response);
    }

    /**
     * Persistimos el promedio de reviews publicadas en tour.rating para acelerar listados/búsquedas.
     */
    private void refreshAndPersistTourRating(Integer tourId) {
        if (tourId == null) return;
        BigDecimal avg = reviewRepository.avgPublishedRatingByTourId(tourId);
        if (avg != null) {
            avg = avg.setScale(1, RoundingMode.HALF_UP);
        }
        Tour tour = tourRepository.findById(tourId).orElse(null);
        if (tour == null) return;
        tour.setRating(avg);
        tourRepository.save(tour);
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
        
        // Si no hay autenticación o no tiene principal, retornar null
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        
        Object principal = auth.getPrincipal();
        
        // Si el principal es un User, retornarlo directamente
        if (principal instanceof User) {
            return (User) principal;
        }
        
        // Si el principal es un String (email o anonymousUser)
        if (principal instanceof String) {
            String emailOrAnonymous = (String) principal;
            
            // Si es "anonymousUser", significa que no hay autenticación válida
            // Intentar procesar el token JWT del header si existe
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
                // Si llegamos aquí, no hay token válido - retornar null (la excepción se lanzará en el método que llama)
                return null;
            }
            
            // Si no es anonymousUser, buscar el usuario por email
            return userRepository.findByEmail(emailOrAnonymous)
                    .orElseThrow(() -> new IllegalStateException("User not found with email: " + emailOrAnonymous));
        }
        
        // Si el principal no es User ni String, retornar null
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
        if (answer.isPresent()) {
            ReviewAnswer answerEntity = answer.get();
            // Cargar attachments del answer
            if (answerEntity.getAttachments() == null) {
                answerEntity.setAttachments(new ArrayList<>());
            }
            List<ReviewAnswerAttachment> answerAttachments = reviewAnswerAttachmentRepository.findByAnswerId(answerEntity.getAnswerId());
            answerEntity.getAttachments().clear();
            if (!answerAttachments.isEmpty()) {
                answerEntity.getAttachments().addAll(answerAttachments);
            }
            review.setAnswer(answerEntity);
        }

        return review;
    }

    /**
     * Guarda los archivos adjuntos de una reseña desde MultipartFile
     * Sube las imágenes a S3 y guarda las referencias en la base de datos
     */
    private List<ReviewAttachment> saveAttachmentsFromMultipartFiles(Long reviewId, List<MultipartFile> files, Integer userId) throws IOException {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<ReviewAttachment> attachments = new ArrayList<>();
        String prefix = "reviews/" + reviewId;

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                // Subir archivo a S3
                String fileUrl = s3Service.uploadFile(prefix, file);

                ReviewAttachment attachment = ReviewAttachment.builder()
                        .reviewId(reviewId)
                        .fileUrl(fileUrl)
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
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
     * Guarda los archivos adjuntos de una reseña desde URLs (strings)
     * Valida que no se exceda el límite de 5 imágenes por review
     */
    private List<ReviewAttachment> saveAttachmentsFromUrls(Long reviewId, List<String> attachmentUrls, Integer userId) {
        // Validar límite de máximo 5 imágenes por review
        if (attachmentUrls != null && attachmentUrls.size() > 5) {
            throw new IllegalStateException("Se permite un máximo de 5 imágenes por review. Se intentaron subir " + attachmentUrls.size() + " imágenes.");
        }

        // Verificar cuántas imágenes ya existen para esta review
        long existingAttachmentsCount = reviewAttachmentRepository.findByReviewId(reviewId).size();
        int newAttachmentsCount = attachmentUrls != null ? (int) attachmentUrls.stream()
                .filter(url -> url != null && !url.isEmpty())
                .count() : 0;
        
        if (existingAttachmentsCount + newAttachmentsCount > 5) {
            throw new IllegalStateException(
                    String.format("Se permite un máximo de 5 imágenes por review. Ya existen %d imágenes y se intentaron agregar %d más.", 
                            existingAttachmentsCount, newAttachmentsCount));
        }

        List<ReviewAttachment> attachments = new ArrayList<>();

        if (attachmentUrls != null) {
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
        }

        return attachments;
    }

    /**
     * Guarda los archivos adjuntos de una respuesta de review desde MultipartFile
     * Sube las imágenes a S3 y guarda las referencias en la base de datos
     */
    private List<ReviewAnswerAttachment> saveAnswerAttachmentsFromMultipartFiles(Long answerId, List<MultipartFile> files, Integer userId) throws IOException {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<ReviewAnswerAttachment> attachments = new ArrayList<>();
        String prefix = "reviews/answers/" + answerId;

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                // Subir archivo a S3
                String fileUrl = s3Service.uploadFile(prefix, file);

                ReviewAnswerAttachment attachment = ReviewAnswerAttachment.builder()
                        .answerId(answerId)
                        .fileUrl(fileUrl)
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
                        .createdBy(userId)
                        .createdDate(LocalDateTime.now())
                        .build();

                attachment = reviewAnswerAttachmentRepository.save(attachment);
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
            Integer tourId = response.getTourId();
            List<TourGallery> galleries = tourGalleryRepository.findByTourIdOrderByOrderIndexAsc(tourId);
            if (!galleries.isEmpty()) {
                response.setTourImage(galleries.get(0).getImageUrl());
            }

            // Obtener nombre del tour
            Tour tour = tourRepository.findById(tourId).orElse(null);
            if (tour != null && tour.getName() != null && tour.getName().getEs() != null) {
                response.setTourName(tour.getName().getEs());
            }
        }
        return response;
    }

    /**
     * Enriquece un ReservationResponse con información adicional del tour
     */
    private void enrichReservationResponse(ReservationResponse response, Reservation reservation) {
        ShoppingCartItem item = shoppingCartItemRepository.findById(reservation.getItemId()).orElse(null);
        
        if (item == null || item.getTourSchedule() == null) {
            return;
        }
        
        TourSchedule schedule = item.getTourSchedule();
        Integer tourId = schedule.getTourId();
        
        if (tourId == null) {
            return;
        }
        
        Tour tour = tourRepository.findById(tourId).orElse(null);
        if (tour == null) {
            return;
        }
        
        // Información básica del tour
        response.setTourId(tourId);
        if (tour.getName() != null && tour.getName().getEs() != null) {
            response.setTourName(tour.getName().getEs());
        }
        if (tour.getTourCategory() != null && tour.getTourCategory().getName() != null) {
            response.setTourType(tour.getTourCategory().getName());
        }
        response.setDuration(tour.getDuration());
        
        // checkInDate = fecha del tour que el usuario seleccionó (scheduleDate del request)
        // returnDate = checkInDate + duration (número de días del tour)
        if (item.getScheduleDate() != null) {
            response.setCheckInDate(item.getScheduleDate().atStartOfDay());
            // Calcular returnDate basado en duration
            if (tour.getDuration() != null && response.getCheckInDate() != null) {
                try {
                    String durationStr = tour.getDuration().trim();
                    int days = 0;
                    
                    // Intentar parsear directamente si es solo un número
                    try {
                        days = Integer.parseInt(durationStr);
                    } catch (NumberFormatException e) {
                        // Si no es solo un número, buscar "Days" o "Day" en el string
                        String[] parts = durationStr.split(" ");
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equalsIgnoreCase("Days") || parts[i].equalsIgnoreCase("Day")) {
                                if (i > 0) {
                                    days = Integer.parseInt(parts[i-1]);
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (days > 0) {
                        response.setReturnDate(response.getCheckInDate().plusDays(days));
                    }
                } catch (Exception e) {
                    log.warn("Could not parse duration: {}", tour.getDuration());
                }
            }
        }
        
        // Destination
        List<TourAddress> addresses = tourAddressRepository.findByTourId(tourId);
        if (addresses != null && !addresses.isEmpty()) {
            TourAddress firstAddress = addresses.get(0);
            if (firstAddress.getCity() != null && firstAddress.getCity().getName() != null) {
                response.setDestination(firstAddress.getCity().getName());
            } else if (firstAddress.getLocation() != null && firstAddress.getLocation().getEs() != null) {
                response.setDestination(firstAddress.getLocation().getEs());
            }
        }
        
        // Precio
        if (item.getTotalPrice() != null) {
            response.setPrice(item.getTotalPrice().doubleValue());
        }
        
        // Travellers
        if (item.getDetails() != null && !item.getDetails().isEmpty()) {
            List<String> travellerParts = new ArrayList<>();
            for (ShoppingCartItemDetail detail : item.getDetails()) {
                if (detail.getQuantity() != null && detail.getQuantity() > 0) {
                    String ageType = detail.getAgeType() != null ? detail.getAgeType().name() : "Adult";
                    travellerParts.add(detail.getQuantity() + " " + ageType + (detail.getQuantity() > 1 ? "s" : ""));
                }
            }
            if (!travellerParts.isEmpty()) {
                response.setTravellers(String.join(", ", travellerParts));
            }
        }
        
        // Actividades (main attractions)
        List<String> activities = tourMainAttractionRepository.findByTourId(tourId).stream()
                .filter(attr -> attr.getDescription() != null && attr.getDescription().getEs() != null)
                .map(attr -> attr.getDescription().getEs())
                .toList();
        if (!activities.isEmpty()) {
            response.setActivities(activities);
        }
        
        // Servicios extra (includes)
        List<String> extraServices = tourIncludesExcludesRepository.findByTourIdAndType(tourId, IncludeExcludeTypeEnum.INCLUDE).stream()
                .filter(inc -> inc.getDescription() != null && inc.getDescription().getEs() != null)
                .map(inc -> inc.getDescription().getEs())
                .toList();
        if (!extraServices.isEmpty()) {
            response.setExtraServices(extraServices);
        }
    }
}

