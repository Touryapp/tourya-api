package com.tourya.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.*;
import com.tourya.api.models.request.CreatePaymentRequest;
import com.tourya.api.models.responses.PaymentResponse;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.models.responses.ServiceResponsibleResponse;
import com.tourya.api.models.responses.PayerResponse;
import com.tourya.api.repository.*;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.constans.enums.CancellationPolicyTypeEnum;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private final ObjectMapper objectMapper;

    /**
     * Crea un pago y automáticamente genera la reserva con sus items.
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for transaction: {}", request.getTransactionId());
        
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }

        // Validar que todos los items del carrito existen
        List<Long> itemIds = request.getItems().stream()
                .map(CreatePaymentRequest.PaymentItemRequest::getShoppingCartItemId)
                .collect(Collectors.toList());
        
        List<ShoppingCartItem> items = shoppingCartItemRepository.findAllById(itemIds);
        if (items.size() != itemIds.size()) {
            throw new IllegalArgumentException("Some shopping cart items were not found");
        }

        // Crear el pago
        Payment payment = Payment.builder()
                .transactionId(request.getTransactionId())
                .transactionData(request.getTransactionData())
                .payerId(request.getPayer().getId())
                .payerName(request.getPayer().getName())
                .payerEmail(request.getPayer().getEmail())
                .payerPhone(request.getPayer().getPhone())
                .payerDocumentType(request.getPayer().getDocumentType())
                .payerDocumentNumber(request.getPayer().getDocumentNumber())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Crear una reserva por cada item del pago
        List<Reservation> reservations = createReservationsForItems(savedPayment, request.getItems(), items);

        // Actualizar el estado de los items del carrito a PAGADO
        updateShoppingCartItemsStatus(items, reservations);

        // Verificar si el carrito debe pasar a inactivo
        checkAndDeactivateCart(items);

        log.info("Payment and {} reservations created successfully. Payment ID: {}", 
                reservations.size(), savedPayment.getPaymentId());

        return buildPaymentResponse(savedPayment, reservations);
    }

    /**
     * Crea una reserva por cada item del pago, cada una con su propio QR.
     */
    private List<Reservation> createReservationsForItems(Payment payment,
                                                        List<CreatePaymentRequest.PaymentItemRequest> requestItems,
                                                        List<ShoppingCartItem> cartItems) {
        List<Reservation> reservations = new java.util.ArrayList<>();
        
        for (CreatePaymentRequest.PaymentItemRequest requestItem : requestItems) {
            ShoppingCartItem cartItem = cartItems.stream()
                    .filter(item -> item.getId().equals(requestItem.getShoppingCartItemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

            // Calcular fechas máximas de cancelación y re-agendamiento
            LocalDate maxCancellationDate = null;
            LocalDate maxReschedulingDate = null;

            LocalDate selectedTourDate = cartItem.getScheduleDate();
            
            if (selectedTourDate != null && cartItem.getTourSchedule() != null) {
                TourSchedule schedule = tourScheduleRepository.findById(cartItem.getTourSchedule().getId())
                        .orElse(null);
                
                if (schedule != null && schedule.getTourId() != null) {
                    Tour tour = tourRepository.findById(schedule.getTourId()).orElse(null);
                    
                    if (tour != null) {
                        // Obtener política de cancelación del tour
                        List<TourCancellationPolicy> policies = tourCancellationPolicyRepository.findByTourId(tour.getId());
                        
                        if (!policies.isEmpty()) {
                            TourCancellationPolicy policy = policies.get(0); // Tomar la primera política
                            
                            // Calcular fecha máxima de cancelación según el tipo de política
                            maxCancellationDate = calculateMaxCancellationDate(
                                    policy.getCancellationPolicyType(), 
                                    selectedTourDate
                            );
                            
                            // Calcular fecha máxima de re-agendamiento (2 días antes del tour)
                            // Se calcula siempre, pero solo se permite re-agendar si allowsRescheduling es true
                            maxReschedulingDate = selectedTourDate.minusDays(2);
                        }
                    }
                }
            }

            // Crear reserva para este item
            Reservation reservation = Reservation.builder()
                    .paymentId(payment.getPaymentId())
                    .itemId(cartItem.getId())
                    .qrUrl(null) // Se generará y subirá después
                    .reservationDate(LocalDateTime.now().plusDays(1)) // Fecha por defecto
                    .deliveryStatus(DeliveryStatusEnum.PENDING)
                    .serviceResponsibleName(requestItem.getServiceResponsible().getName())
                    .serviceResponsibleEmail(requestItem.getServiceResponsible().getEmail())
                    .serviceResponsiblePhone(requestItem.getServiceResponsible().getPhone())
                    .maxCancellationDate(maxCancellationDate)
                    .maxReschedulingDate(maxReschedulingDate)
                    .build();

            reservation = reservationRepository.save(reservation);
            
            // Generar y subir QR code a S3 para esta reserva
            String qrUrl = reservationQrService.generateAndUploadQrCode(reservation.getReservationId());
            reservation.setQrUrl(qrUrl);
            reservation = reservationRepository.save(reservation);
            
            reservations.add(reservation);
            
            log.debug("Created reservation {} for item {} in payment {}", 
                    reservation.getReservationId(), cartItem.getId(), payment.getPaymentId());
        }
        
        return reservations;
    }

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
                .map(this::buildReservationResponse)
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

        // Construir respuesta del pago
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionId(payment.getTransactionId())
                .transactionData(payment.getTransactionData())
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
    private ReservationResponse buildReservationResponse(Reservation reservation) {
        // Construir respuesta del responsable del servicio
        ServiceResponsibleResponse serviceResponsible = ServiceResponsibleResponse.builder()
                .name(reservation.getServiceResponsibleName())
                .email(reservation.getServiceResponsibleEmail())
                .phone(reservation.getServiceResponsiblePhone() != null ? 
                      Long.parseLong(reservation.getServiceResponsiblePhone().replaceAll("[^0-9]", "")) : null)
                .build();

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
                .build();
        
        // Enriquecer con información del tour y del payer
        enrichReservationResponse(response, reservation);
        
        return response;
    }
    
    /**
     * Enriquece la respuesta de la reserva con información adicional del tour.
     * Copia exacta de ReservationService.enrichReservationResponse para mantener consistencia.
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
}