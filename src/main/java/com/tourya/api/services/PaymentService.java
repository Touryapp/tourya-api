package com.tourya.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.*;
import com.tourya.api.models.request.CreatePaymentRequest;
import com.tourya.api.models.responses.PaymentCreditItemResponse;
import com.tourya.api.models.responses.PaymentResponse;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.models.responses.ServiceResponsibleResponse;
import com.tourya.api.models.responses.PayerResponse;
import com.tourya.api.repository.*;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.constans.enums.CancellationPolicyTypeEnum;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import com.tourya.api.constans.enums.CreditStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de pagos.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final QrCodeService qrCodeService;
    private final ReservationQrService reservationQrService;
    private final TourRepository tourRepository;
    private final TourCancellationPolicyRepository tourCancellationPolicyRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final TourMainAttractionRepository tourMainAttractionRepository;
    private final TourIncludesExcludesRepository tourIncludesExcludesRepository;
    private final TourAddressRepository tourAddressRepository;
    private final TourGalleryRepository tourGalleryRepository;
    private final ObjectMapper objectMapper;
    private final AgeRangeConfigService ageRangeConfigService;
    private final CreditRepository creditRepository;
    private final PaymentCreditRepository paymentCreditRepository;
    private final EmailService emailService;

    /**
     * Crea un pago y automáticamente genera la reserva con sus items.
     * Cuando el pago incluye créditos, authentication es obligatorio: los créditos se validan contra el usuario del token (dueño del carrito); el pagador puede ser un tercero.
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, Authentication authentication) {
        log.info("Creating payment for transaction: {}", request.getTransactionId());
        
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }

        // Validar que todos los items del carrito existen
        List<Long> reservationIds = request.getReservationIds();
        List<Reservation> reservationsToPay = reservationRepository.findAllByReservationIdIn(reservationIds);
        if (reservationsToPay.size() != reservationIds.size()) {
            throw new IllegalArgumentException("Algunas reservas no fueron encontradas");
        }

        List<Long> itemIds = reservationsToPay.stream().map(Reservation::getItemId).toList();
        List<ShoppingCartItem> items = shoppingCartItemRepository.findAllById(itemIds);
        if (items.size() != itemIds.size()) {
            throw new IllegalArgumentException("Algunos items del carrito asociados a las reservas no fueron encontrados");
        }

        // Calcular precio total desde las RESERVAS (total_amount) y validar consistencia
        java.math.BigDecimal totalPrice = reservationsToPay.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        Integer cartOwnerUserId = null;
        if (request.getPaymentType() != null && 
                (request.getPaymentType().equals("CREDIT") || request.getPaymentType().equals("CREDIT_AND_PLATFORM"))) {
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                throw new IllegalArgumentException("Para pagos con crédito es necesario estar autenticado (usuario del token = dueño del carrito)");
            }
            User tokenUser = (User) authentication.getPrincipal();
            cartOwnerUserId = tokenUser.getId();
            final Integer cartOwnerId = cartOwnerUserId;
            // Los items deben ser del carrito del usuario del token
            boolean allItemsBelongToTokenUser = items.stream()
                    .allMatch(item -> item.getShoppingCart().getUser().getId().equals(cartOwnerId));
            if (!allItemsBelongToTokenUser) {
                throw new IllegalArgumentException("Los items del carrito deben corresponder al usuario autenticado (token)");
            }
        }

        // Validar y manejar consumo de crédito si aplica
        if (request.getPaymentType() != null && 
                (request.getPaymentType().equals("CREDIT") || request.getPaymentType().equals("CREDIT_AND_PLATFORM"))) {
            
            // Validar que creditData esté presente
            if (request.getCreditData() == null || request.getCreditData().getCreditIds() == null 
                    || request.getCreditData().getCreditIds().isEmpty()) {
                throw new IllegalArgumentException("Credit data with creditIds is required when payment type includes CREDIT");
            }
            
            // Validar que amountCredit esté presente si se usa crédito
            if (request.getAmountCredit() == null) {
                throw new IllegalArgumentException("amountCredit is required when payment type includes CREDIT");
            }
            
            // Validar que la suma de amountCredit + amountPlatform = totalPrice
            java.math.BigDecimal amountCredit = request.getAmountCredit();
            java.math.BigDecimal amountPlatform = request.getAmountPlatform() != null 
                    ? request.getAmountPlatform() 
                    : java.math.BigDecimal.ZERO;
            
            java.math.BigDecimal sum = amountCredit.add(amountPlatform);
            if (sum.compareTo(totalPrice) != 0) {
                throw new IllegalArgumentException(
                        String.format("La suma de amountCredit (%.2f) + amountPlatform (%.2f) = %.2f " +
                                "debe ser igual al totalPrice (%.2f)", 
                                amountCredit, amountPlatform, sum, totalPrice));
            }
            
            // Validar amountPlatform según el tipo de pago
            if (request.getPaymentType().equals("CREDIT")) {
                if (amountPlatform.compareTo(java.math.BigDecimal.ZERO) != 0) {
                    throw new IllegalArgumentException("amountPlatform must be 0 or null when paymentType is CREDIT");
                }
            } else if (request.getPaymentType().equals("CREDIT_AND_PLATFORM")) {
                if (amountPlatform == null || amountPlatform.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("amountPlatform is required and must be greater than 0 when paymentType is CREDIT_AND_PLATFORM");
                }
            }
        } else if (request.getPaymentType() != null && request.getPaymentType().equals("PLATFORM")) {
            // Validar que amountPlatform = totalPrice para pago solo con plataforma
            java.math.BigDecimal amountPlatform = request.getAmountPlatform() != null 
                    ? request.getAmountPlatform() 
                    : java.math.BigDecimal.ZERO;
            
            if (amountPlatform.compareTo(totalPrice) != 0) {
                throw new IllegalArgumentException(
                        String.format("amountPlatform (%.2f) must be equal to totalPrice (%.2f) when paymentType is PLATFORM",
                                amountPlatform, totalPrice));
            }
        }

        // Crear el pago (guardar amount_credit cuando se paga con créditos)
        java.math.BigDecimal paymentAmountCredit = (request.getPaymentType() != null
                && (request.getPaymentType().equals("CREDIT") || request.getPaymentType().equals("CREDIT_AND_PLATFORM")))
                ? request.getAmountCredit()
                : null;
        Payment payment = Payment.builder()
                .transactionId(request.getTransactionId())
                .transactionData(request.getTransactionData())
                .payerId(request.getPayer().getId())
                .payerName(request.getPayer().getName())
                .payerEmail(request.getPayer().getEmail())
                .payerPhone(request.getPayer().getPhone())
                .payerDocumentType(request.getPayer().getDocumentType())
                .payerDocumentNumber(request.getPayer().getDocumentNumber())
                .amountCredit(paymentAmountCredit)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Si es pago con crédito: validar reservas, consumir créditos y guardar detalle (ids + monto por crédito)
        if (request.getPaymentType() != null
                && (request.getPaymentType().equals("CREDIT") || request.getPaymentType().equals("CREDIT_AND_PLATFORM"))) {
            validateAndConsumeReservedCredits(
                    new java.util.HashSet<>(itemIds),
                    request.getCreditData().getCreditIds(),
                    request.getAmountCredit(),
                    cartOwnerUserId,
                    savedPayment.getPaymentId());
        }

        // Confirmar reservas TEMPORAL existentes (en vez de crear reservas aquí)
        List<Reservation> reservations = confirmTemporalReservations(savedPayment, reservationsToPay);

        // Actualizar el estado de los items del carrito a PAGADO y asigna reservationId (si no estaba)
        updateShoppingCartItemsStatus(items, reservations);

        // Verificar si el carrito debe pasar a inactivo
        checkAndDeactivateCart(items);

        log.info("Payment and {} reservations confirmed successfully. Payment ID: {}",
                reservations.size(), savedPayment.getPaymentId());

        PaymentResponse response = buildPaymentResponse(savedPayment, reservations);
        sendPurchaseEmailBestEffort(savedPayment, response);
        return response;
    }

    private void sendPurchaseEmailBestEffort(Payment payment, PaymentResponse response) {
        try {
            if (payment == null || payment.getPayerEmail() == null || payment.getPayerEmail().isBlank()) return;
            String subject = "Confirmación de compra - Tourya";
            String username = payment.getPayerName() != null && !payment.getPayerName().isBlank()
                    ? payment.getPayerName()
                    : "Turista";
            emailService.sendPurchaseConfirmationEmail(
                    payment.getPayerEmail(),
                    username,
                    response != null ? response.getReservations() : null,
                    subject
            );
        } catch (Exception ignored) {
            // best-effort: no bloquear el pago por fallo de correo
        }
    }

    /**
     * Confirma reservas TEMPORAL asociadas a los items del pago.
     * Reglas:
     * - Deben existir reservas TEMPORAL para cada item del request.
     * - No pueden estar expiradas (expiresAt > now()).
     * - Se actualizan a PENDING, se asigna paymentId, y se asegura QR.
     */
    private List<Reservation> confirmTemporalReservations(Payment payment,
                                                         List<Reservation> reservationsToPay) {
        // Mantener coherencia con creación del hold (UTC) para evitar falsos expirados por zona horaria
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("UTC"));
        List<Reservation> confirmed = new java.util.ArrayList<>();

        for (Reservation reservation : reservationsToPay) {
            if (reservation.getDeliveryStatus() != DeliveryStatusEnum.TEMPORAL) {
                throw new IllegalArgumentException(
                        "La reserva " + reservation.getReservationId()
                                + " no está en estado TEMPORAL (estado actual: " + reservation.getDeliveryStatus() + ")");
            }
            if (reservation.getExpiresAt() != null && !reservation.getExpiresAt().isAfter(now)) {
                throw new IllegalArgumentException("La reserva temporal " + reservation.getReservationId() + " está expirada");
            }

            // Calcular ventanas (maxCancellationDate / maxReschedulingDate) al confirmar el pago
            LocalDate tourDate = resolveTourDateForReservation(reservation);
            setWindowsFromTourPolicy(reservation, tourDate);

            reservation.setPaymentId(payment.getPaymentId());
            reservation.setDeliveryStatus(DeliveryStatusEnum.PENDING);
            reservation.setExpiresAt(null);

            reservation = reservationRepository.save(reservation);

            if (reservation.getQrUrl() == null) {
                String qrUrl = reservationQrService.generateAndUploadQrCode(reservation.getReservationId());
                reservation.setQrUrl(qrUrl);
                reservation = reservationRepository.save(reservation);
            }

            confirmed.add(reservation);
        }

        return confirmed;
    }

    private LocalDate resolveTourDateForReservation(Reservation reservation) {
        ShoppingCartItem item = shoppingCartItemRepository.findById(reservation.getItemId()).orElse(null);
        if (item == null) {
            return null;
        }
        if (item.getScheduleDate() != null) {
            return item.getScheduleDate();
        }
        if (item.getTourSchedule() != null) {
            return item.getTourSchedule().getScheduleDate();
        }
        // fallback: usar la parte de fecha del reservationDate
        return reservation.getReservationDate() != null ? reservation.getReservationDate().toLocalDate() : null;
    }

    private void setWindowsFromTourPolicy(Reservation reservation, LocalDate tourDate) {
        if (tourDate == null) {
            reservation.setMaxCancellationDate(null);
            reservation.setMaxReschedulingDate(null);
            return;
        }

        // maxReschedulingDate: regla actual del servicio de reservas
        reservation.setMaxReschedulingDate(tourDate.minusDays(2));

        // maxCancellationDate: usar política del tour si existe; fallback a null si no se puede inferir
        ShoppingCartItem item = shoppingCartItemRepository.findById(reservation.getItemId()).orElse(null);
        Integer tourId = item != null && item.getTourSchedule() != null ? item.getTourSchedule().getTourId() : null;
        if (tourId == null) {
            reservation.setMaxCancellationDate(null);
            return;
        }
        List<TourCancellationPolicy> policies = tourCancellationPolicyRepository.findByTourId(tourId);
        if (policies == null || policies.isEmpty()) {
            reservation.setMaxCancellationDate(null);
            return;
        }
        CancellationPolicyTypeEnum type = policies.get(0).getCancellationPolicyType();
        reservation.setMaxCancellationDate(defaultMaxCancellationDate(type, tourDate));
    }

    private LocalDate defaultMaxCancellationDate(CancellationPolicyTypeEnum policyType, LocalDate tourDate) {
        if (policyType == null || tourDate == null) {
            return null;
        }
        return switch (policyType) {
            case FLEXIBLE -> tourDate.minusDays(1);
            case STANDARD -> tourDate.minusDays(2);
            case MODERATE -> tourDate.minusDays(4);
            case STRICT -> tourDate.minusDays(7);
        };
    }

    // createReservationsForItems removido: ahora payment confirma reservas temporales existentes.

    /**
     * Actualiza el estado de los items del carrito a PAGADO y asigna reservationId.
     */
    private void updateShoppingCartItemsStatus(List<ShoppingCartItem> items, List<Reservation> reservations) {
        // Crear un mapa de itemId -> reservationId
        java.util.Map<Long, Long> itemToReservationMap = new java.util.HashMap<>();
        for (Reservation reservation : reservations) {
            itemToReservationMap.put(reservation.getItemId(), reservation.getReservationId());
        }
        
        for (ShoppingCartItem item : items) {
            item.setStatus(ShoppingCartStatusEnum.PAID);
            Long reservationId = itemToReservationMap.get(item.getId());
            if (reservationId != null) {
                item.setReservationId(reservationId);
            }
            shoppingCartItemRepository.save(item);
        }
    }

    /**
     * Verifica si el carrito debe pasar a estado inactivo.
     */
    private void checkAndDeactivateCart(List<ShoppingCartItem> items) {
        if (!items.isEmpty()) {
            ShoppingCart cart = items.get(0).getShoppingCart();
            
            // Verificar si todos los items del carrito están pagados o completados
            List<ShoppingCartItem> allCartItems = shoppingCartItemRepository.findByShoppingCart(cart);
            boolean allItemsInactive = allCartItems.stream()
                    .allMatch(item -> item.getStatus() == ShoppingCartStatusEnum.PAID || 
                                    item.getStatus() == ShoppingCartStatusEnum.COMPLETED ||
                                    item.getStatus() == ShoppingCartStatusEnum.ABANDONED);

            if (allItemsInactive) {
                // Desactivar el carrito
                cart.setStatus(ShoppingCartStatusEnum.COMPLETED);
                shoppingCartRepository.save(cart);
                log.info("Cart {} deactivated - all items are inactive", cart.getId());
            }
        }
    }

    // Método generateQrUrl removido - ahora se usa ReservationQrService

    /**
     * Construye la respuesta del pago con lista de reservas.
     */
    private PaymentResponse buildPaymentResponse(Payment payment, List<Reservation> reservations) {
        // Construir respuestas de las reservas
        List<ReservationResponse> reservationResponses = reservations.stream()
                .map(r -> buildReservationResponse(r, payment))
                .collect(Collectors.toList());

        // Construir respuesta del pagador
        PayerResponse payerResponse = 
                PayerResponse.builder()
                        .name(payment.getPayerName())
                        .email(payment.getPayerEmail())
                        .id(payment.getPayerId())
                        .phone(payment.getPayerPhone())
                        .documentType(payment.getPayerDocumentType())
                        .documentNumber(payment.getPayerDocumentNumber())
                        .build();

        // Detalle de créditos usados (si aplica)
        List<PaymentCreditItemResponse> creditsUsed = null;
        if (payment.getAmountCredit() != null && payment.getAmountCredit().compareTo(java.math.BigDecimal.ZERO) > 0) {
            creditsUsed = paymentCreditRepository.findByPaymentId(payment.getPaymentId()).stream()
                    .map(pc -> PaymentCreditItemResponse.builder()
                            .creditId(pc.getCreditId())
                            .amountUsed(pc.getAmountUsed())
                            .build())
                    .collect(Collectors.toList());
        }

        // Construir respuesta del pago
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionId(payment.getTransactionId())
                .transactionData(payment.getTransactionData())
                .amountCredit(payment.getAmountCredit())
                .creditsUsed(creditsUsed)
                .reservations(reservationResponses)
                .payer(payerResponse)
                .createdDate(payment.getCreatedDate())
                .lastModifiedDate(payment.getLastModifiedDate())
                .createdBy(payment.getCreatedBy())
                .lastModifiedBy(payment.getLastModifiedBy())
                .build();
    }

    /**
     * Construye la respuesta de una reserva.
     */
    private ReservationResponse buildReservationResponse(Reservation reservation, Payment payment) {
        // Construir respuesta del responsable del servicio
        ServiceResponsibleResponse serviceResponsible = null;
        if (reservation.getServiceResponsibleName() != null ||
                reservation.getServiceResponsibleEmail() != null ||
                reservation.getServiceResponsiblePhone() != null) {
            serviceResponsible = ServiceResponsibleResponse.builder()
                    .name(reservation.getServiceResponsibleName())
                    .email(reservation.getServiceResponsibleEmail())
                    .phone(reservation.getServiceResponsiblePhone() != null ?
                            Long.parseLong(reservation.getServiceResponsiblePhone().replaceAll("[^0-9]", "")) : null)
                    .build();
        }

        ReservationResponse response = ReservationResponse.builder()
                .reservationId(reservation.getReservationId())
                .paymentId(reservation.getPaymentId())
                .itemId(reservation.getItemId())
                .qrUrl(reservation.getQrUrl()) // URL del QR en S3
                .reservationDate(reservation.getReservationDate())
                .deliveryStatus(reservation.getDeliveryStatus())
                .serviceResponsible(serviceResponsible)
                .createdDate(reservation.getCreatedDate())
                .lastModifiedDate(reservation.getLastModifiedDate())
                .createdBy(reservation.getCreatedBy())
                .lastModifiedBy(reservation.getLastModifiedBy())
                // Campos de cancelación y re-agendamiento
                .maxCancellationDate(reservation.getMaxCancellationDate())
                .maxReschedulingDate(reservation.getMaxReschedulingDate())
                .cancellationReason(reservation.getCancellationReason())
                .cancellationDate(reservation.getCancellationDate())
                .payerName(payment != null ? payment.getPayerName() : null)
                .payerEmail(payment != null ? payment.getPayerEmail() : null)
                .payerPhone(payment != null ? payment.getPayerPhone() : null)
                .payerDocumentType(payment != null ? payment.getPayerDocumentType() : null)
                .payerDocumentNumber(payment != null ? payment.getPayerDocumentNumber() : null)
                .build();

        // Enriquecer con información del tour (y payer desde BD si hiciera falta vía paymentId en otro flujo)
        enrichReservationResponse(response, reservation);

        return response;
    }
    
    /**
     * Enriquece la respuesta de la reserva con información adicional del tour.
     * Copia exacta de ReservationService.enrichReservationResponse para mantener consistencia.
     */
    private void enrichReservationResponse(ReservationResponse response, Reservation reservation) {
        if (reservation.getPaymentId() != null) {
            paymentRepository.findById(reservation.getPaymentId()).ifPresent(payment -> {
                response.setPayerName(payment.getPayerName());
                response.setPayerEmail(payment.getPayerEmail());
                response.setPayerPhone(payment.getPayerPhone());
                response.setPayerDocumentType(payment.getPayerDocumentType());
                response.setPayerDocumentNumber(payment.getPayerDocumentNumber());
            });
        }

        Long itemId = reservation.getItemId();
        ShoppingCartItem item = itemId != null ? shoppingCartItemRepository.findById(itemId).orElse(null) : null;

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

        // Imagen principal (order_index = 1); fallback a primera imagen si existe
        List<TourGallery> galleries = tourGalleryRepository.findByTourIdAndOrderIndex(tourId, 1);
        if (galleries == null || galleries.isEmpty()) {
            galleries = tourGalleryRepository.findByTourIdOrderByOrderIndexAsc(tourId);
        }
        if (galleries != null && !galleries.isEmpty()) {
            response.setTourImageUrl(galleries.get(0).getImageUrl());
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
            List<String> travellerParts = new java.util.ArrayList<>();
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
        
        // Actividades (main attractions) - USANDO .toList() como ReservationService
        List<String> activities = tourMainAttractionRepository.findByTourId(tourId).stream()
                .filter(attr -> attr.getDescription() != null && attr.getDescription().getEs() != null)
                .map(attr -> attr.getDescription().getEs())
                .toList();
        if (!activities.isEmpty()) {
            response.setActivities(activities);
        }
        
        // Servicios extra (includes) - USANDO .toList() como ReservationService
        List<String> extraServices = tourIncludesExcludesRepository.findByTourIdAndType(tourId, IncludeExcludeTypeEnum.INCLUDE).stream()
                .filter(inc -> inc.getDescription() != null && inc.getDescription().getEs() != null)
                .map(inc -> inc.getDescription().getEs())
                .toList();
        if (!extraServices.isEmpty()) {
            response.setExtraServices(extraServices);
        }
    }

    /**
     * Obtiene un pago por su ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        log.info("Getting payment by id: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + paymentId));

        List<Reservation> reservations = reservationRepository.findByPaymentId(payment.getPaymentId());
        if (reservations.isEmpty()) {
            throw new IllegalArgumentException("Reservations not found for payment: " + paymentId);
        }

        return buildPaymentResponse(payment, reservations);
    }

    /**
     * Obtiene un pago por ID de transacción.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        log.info("Getting payment by transaction id: {}", transactionId);
        
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with transaction id: " + transactionId));

        List<Reservation> reservations = reservationRepository.findByPaymentId(payment.getPaymentId());
        if (reservations.isEmpty()) {
            throw new IllegalArgumentException("Reservations not found for payment: " + payment.getPaymentId());
        }

        return buildPaymentResponse(payment, reservations);
    }

    /**
     * Genera la imagen QR en Base64 para una reserva específica.
     * Ahora usa la URL del QR almacenada en S3.
     */
    public String generateQrImageBase64(String qrUrl) {
        try {
            // Si la URL ya es una URL de S3, devolverla directamente
            if (qrUrl != null && qrUrl.startsWith("https://")) {
                return qrUrl;
            }
            
            // Si es un token, buscar la reserva correspondiente
            Reservation reservation = reservationRepository.findByQrUrl(qrUrl)
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found for QR: " + qrUrl));
            
            return reservation.getQrUrl();
        } catch (Exception e) {
            log.error("Error getting QR image URL: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Calcula la fecha máxima de cancelación según el tipo de política.
     * 
     * @param policyType Tipo de política de cancelación
     * @param tourDate Fecha del tour
     * @return Fecha máxima de cancelación
     */
    private LocalDate calculateMaxCancellationDate(CancellationPolicyTypeEnum policyType, LocalDate tourDate) {
        if (policyType == null || tourDate == null) {
            return null;
        }

        return switch (policyType) {
            case FLEXIBLE -> tourDate.minusDays(1); // hasta 24 horas antes
            case STANDARD -> tourDate.minusDays(2); // hasta 48 horas antes
            case MODERATE -> tourDate.minusDays(4); // hasta 4 días antes
            case STRICT -> tourDate.minusDays(7); // hasta 7 días antes
        };
    }
    
    /**
     * Valida que los IDs de créditos enviados sean exactamente los reservados para los items del pago,
     * que la suma de montos reservados coincida con amountCredit, y luego consume lo reservado.
     * Cada crédito: amount = amount - reserved_amount; reserved_amount = 0; shopping_cart_item_id = null;
     * status = amount > 0 ? CREATED : CONSUMED.
     *
     * @param paymentItemIds IDs de los items del carrito que se están pagando
     * @param creditIds       IDs de créditos enviados en el pago (deben ser todos los reservados para esos items)
     * @param amountCredit    Monto a pagar con crédito (debe coincidir con la suma de reserved_amount)
     * @param cartOwnerUserId ID del usuario del token (dueño del carrito); los créditos deben ser de este usuario. El pagador puede ser un tercero.
     * @param paymentId       ID del pago ya guardado; se usa para persistir el detalle de créditos usados en payment_credit.
     */
    private void validateAndConsumeReservedCredits(Set<Long> paymentItemIds, List<Long> creditIds,
                                                   java.math.BigDecimal amountCredit, Integer cartOwnerUserId, Long paymentId) {
        if (creditIds == null || creditIds.isEmpty()) {
            throw new IllegalArgumentException("La lista de IDs de créditos no puede estar vacía");
        }
        if (amountCredit == null || amountCredit.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a pagar con crédito debe ser mayor a 0");
        }
        if (paymentItemIds == null || paymentItemIds.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un item del carrito en el pago");
        }

        List<Credit> requestedCredits = creditRepository.findAllById(creditIds);
        if (requestedCredits.size() != creditIds.size()) {
            throw new IllegalArgumentException("Algunos créditos no fueron encontrados");
        }

        // Todos deben estar RESERVED y asociados a uno de los items del pago y ser del pagador
        for (Credit c : requestedCredits) {
            if (c.getStatus() != CreditStatusEnum.RESERVED) {
                throw new IllegalArgumentException(
                        "Crédito " + c.getId() + " no está reservado para este pago. Estado: " + c.getStatus());
            }
            if (c.getShoppingCartItemId() == null || !paymentItemIds.contains(c.getShoppingCartItemId())) {
                throw new IllegalArgumentException(
                        "Crédito " + c.getId() + " no está reservado para ninguno de los items de este pago");
            }
            if (!c.getUserId().equals(cartOwnerUserId)) {
                throw new IllegalArgumentException("Crédito " + c.getId() + " no pertenece al usuario del carrito (usuario del token)");
            }
        }

        // Créditos reservados para (algún) item del pago: debe ser exactamente el mismo conjunto que creditIds
        List<Credit> reservedForItems = creditRepository.findByShoppingCartItemIdInAndStatusReserved(paymentItemIds);
        Set<Long> reservedIds = reservedForItems.stream().map(Credit::getId).collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(creditIds);
        if (!reservedIds.equals(requestedIds)) {
            if (!requestedIds.containsAll(reservedIds)) {
                throw new IllegalArgumentException(
                        "Faltan IDs de créditos reservados para los items del pago. " +
                                "Debe enviar todos los créditos reservados para los items que está pagando.");
            }
            throw new IllegalArgumentException(
                    "Se envió un ID de crédito que no está reservado para ninguno de los items de este pago");
        }

        java.math.BigDecimal sumReserved = reservedForItems.stream()
                .map(c -> c.getReservedAmount() != null ? c.getReservedAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        if (sumReserved.compareTo(amountCredit) != 0) {
            throw new IllegalArgumentException(
                    String.format("La suma de créditos reservados (%.2f) no coincide con el monto a pagar con crédito (%.2f)",
                            sumReserved, amountCredit));
        }

        // Consumir: amount = amount - reserved_amount; reserved_amount = 0; item = null; status = CONSUMED o CREATED
        // y guardar detalle en payment_credit (id del crédito y monto usado)
        for (Credit credit : reservedForItems) {
            java.math.BigDecimal reserved = credit.getReservedAmount() != null
                    ? credit.getReservedAmount()
                    : java.math.BigDecimal.ZERO;
            java.math.BigDecimal newAmount = credit.getAmount().subtract(reserved);
            credit.setAmount(newAmount);
            credit.setReservedAmount(java.math.BigDecimal.ZERO);
            credit.setShoppingCartItemId(null);
            credit.setStatus(newAmount.compareTo(java.math.BigDecimal.ZERO) > 0
                    ? CreditStatusEnum.CREATED
                    : CreditStatusEnum.CONSUMED);
            creditRepository.save(credit);
            if (paymentId != null) {
                PaymentCredit paymentCredit = PaymentCredit.builder()
                        .paymentId(paymentId)
                        .creditId(credit.getId())
                        .amountUsed(reserved)
                        .build();
                paymentCreditRepository.save(paymentCredit);
            }
            log.info("Credit {} consumed reserved {}; new amount={}, status={}",
                    credit.getId(), reserved, newAmount, credit.getStatus());
        }
        log.info("Successfully consumed {} reserved credits for total amount: {}", reservedForItems.size(), amountCredit);
    }
}