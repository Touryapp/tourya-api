package com.tourya.api.services;

import com.tourya.api.common.PageResponse;
import com.tourya.api.config.security.JwtService;
import com.tourya.api.constans.enums.AgePriceType;
import com.tourya.api.constans.enums.CancellationPolicyTypeEnum;
import com.tourya.api.constans.enums.CancellationReasonEnum;
import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.constans.enums.MaritimeFlagEnum;
import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import com.tourya.api.constans.enums.AccountPayableStatusEnum;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.ReservationMapper;
import com.tourya.api.models.request.CancelReservationRequest;
import com.tourya.api.models.request.RescheduleReservationRequest;
import com.tourya.api.models.responses.ReservationDetailsResponse;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.models.responses.CreditResponse;
import com.tourya.api.models.responses.RescheduleValidationResponse;
import com.tourya.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar reservas.
 * Implementa funcionalidad para crear y consultar reservas siguiendo programación funcional limpia.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationMapper reservationMapper;
    private final JwtService jwtService;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final TourRepository tourRepository;
    private final ProviderService providerService;
    private final AccountPayableRepository accountPayableRepository;
    private final TourMainAttractionRepository tourMainAttractionRepository;
    private final TourIncludesExcludesRepository tourIncludesExcludesRepository;
    private final TourAddressRepository tourAddressRepository;
    private final ReservationNativeRepository reservationNativeRepository;
    private final TourCancellationPolicyRepository tourCancellationPolicyRepository;
    private final CreditRepository creditRepository;
    private final MaritimActivityReportRepository maritimActivityReportRepository;
    private final TourScheduleConfigSlotRepository tourScheduleConfigSlotRepository;
    private final AppConfigService appConfigService;
    /**
     * Método createReservation removido - las reservas se crean automáticamente con los pagos
     */

    /**
     * Consulta una reserva por su ID con información detallada del tour.
     * 
     * @param id ID de la reserva
     * @return ReservationResponse con la información de la reserva incluyendo detalles del tour
     * @throws ResourceNotFoundException si la reserva no existe
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        log.info("Getting reservation by id: {}", id);
        
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        
        ReservationResponse response = reservationMapper.toResponse(reservation);
        enrichReservationResponse(response, reservation);
        
        return response;
    }
    
    /**
     * Enriquece un ReservationResponse con información adicional del tour y del pagador
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
        
        // maxCancellationDate y maxReschedulingDate vienen directamente de la BD (ya están en el mapper)
        // No se calculan dinámicamente porque se guardan en la tabla reservation
    }
    
    @Transactional(readOnly = true)
    public com.tourya.api.models.responses.BookingDetailsResponse getBookingDetailsById(Long reservationId) {
        log.info("Getting booking details for reservation id: {}", reservationId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
        
        return buildBookingDetailsResponse(reservation);
    }
    
    private com.tourya.api.models.responses.BookingDetailsResponse buildBookingDetailsResponse(Reservation reservation) {
        // Cargar relaciones necesarias
        Payment payment = paymentRepository.findById(reservation.getPaymentId())
                .orElse(null);
        ShoppingCartItem item = shoppingCartItemRepository.findById(reservation.getItemId())
                .orElse(null);
        
        // Obtener tour
        Integer tourId = null;
        String tourName = null;
        String tourType = null;
        String duration = null;
        LocalDateTime checkInDate = null;
        LocalDateTime returnDate = null;
        String destination = null;
        List<String> activities = new ArrayList<>();
        List<String> extraServices = new ArrayList<>();
        
        if (item != null && item.getTourSchedule() != null) {
            TourSchedule schedule = item.getTourSchedule();
            tourId = schedule.getTourId();
            checkInDate = schedule.getScheduleDate() != null 
                ? schedule.getScheduleDate().atStartOfDay() 
                : null;
            
            // Cargar tour completo desde repositorio
            if (tourId != null) {
                Tour tour = tourRepository.findById(tourId).orElse(null);
                if (tour != null) {
                    tourName = tour.getName() != null && tour.getName().getEs() != null 
                        ? tour.getName().getEs() 
                        : null;
                    tourType = tour.getTourCategory() != null && tour.getTourCategory().getName() != null 
                        ? tour.getTourCategory().getName() 
                        : null;
                    duration = tour.getDuration() != null ? tour.getDuration() : null;
                    
                    // Obtener actividades (main attractions) desde repositorio
                    activities = tourMainAttractionRepository.findByTourId(tourId).stream()
                        .filter(attr -> attr.getDescription() != null && attr.getDescription().getEs() != null)
                        .map(attr -> attr.getDescription().getEs())
                        .toList();
                    
                    // Obtener servicios extra (includes) desde repositorio
                    extraServices = tourIncludesExcludesRepository.findByTourIdAndType(tourId, IncludeExcludeTypeEnum.INCLUDE).stream()
                        .filter(inc -> inc.getDescription() != null && inc.getDescription().getEs() != null)
                        .map(inc -> inc.getDescription().getEs())
                        .toList();
                    
                    // Destination desde tour address
                    List<TourAddress> addresses = tourAddressRepository.findByTourId(tourId);
                    if (addresses != null && !addresses.isEmpty()) {
                        TourAddress firstAddress = addresses.get(0);
                        if (firstAddress.getCity() != null && firstAddress.getCity().getName() != null) {
                            destination = firstAddress.getCity().getName();
                        } else if (firstAddress.getLocation() != null && firstAddress.getLocation().getEs() != null) {
                            destination = firstAddress.getLocation().getEs();
                        }
                    }
                }
            }
            
            // Calcular returnDate basado en duration y checkInDate
            if (checkInDate != null && duration != null) {
                try {
                    String durationStr = duration.trim();
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
                        returnDate = checkInDate.plusDays(days);
                    }
                } catch (Exception e) {
                    log.warn("Could not parse duration: {}", duration);
                }
            }
        }
        
        // Construir travellers string desde los detalles del item
        String travellers = null;
        if (item != null && item.getDetails() != null && !item.getDetails().isEmpty()) {
            List<String> travellerParts = new ArrayList<>();
            for (ShoppingCartItemDetail detail : item.getDetails()) {
                if (detail.getQuantity() != null && detail.getQuantity() > 0) {
                    String ageType = detail.getAgeType() != null ? detail.getAgeType().name() : "Adult";
                    travellerParts.add(detail.getQuantity() + " " + ageType + (detail.getQuantity() > 1 ? "s" : ""));
                }
            }
            if (!travellerParts.isEmpty()) {
                travellers = String.join(", ", travellerParts);
            }
        }
        
        // Obtener precio
        Double price = null;
        if (item != null && item.getTotalPrice() != null) {
            price = item.getTotalPrice().doubleValue();
        }
        
        return com.tourya.api.models.responses.BookingDetailsResponse.builder()
                .id(reservation.getReservationId().intValue())
                .reservationId(reservation.getReservationId().toString())
                .paymentId(reservation.getPaymentId())
                .transactionId(payment != null ? payment.getTransactionId() : null)
                .payer(payment != null ? payment.getPayerName() : null)
                .email(payment != null ? payment.getPayerEmail() : null)
                .reservationDate(reservation.getReservationDate())
                .status(reservation.getDeliveryStatus() != null ? reservation.getDeliveryStatus().name() : null)
                .tourId(tourId)
                .tourName(tourName)
                .tourType(tourType)
                .price(price)
                .travellers(travellers)
                .duration(duration)
                .checkInDate(checkInDate)
                .returnDate(returnDate)
                .destination(destination)
                .customerPhone(payment != null ? payment.getPayerPhone() : null)
                .extraServices(extraServices.isEmpty() ? null : extraServices)
                .activities(activities.isEmpty() ? null : activities)
                .build();
    }

    /**
     * Consulta una reserva por su URL QR.
     * 
     * @param qrUrl URL QR de la reserva
     * @return ReservationResponse con la información de la reserva
     * @throws ResourceNotFoundException si la reserva no existe
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationByQrUrl(String qrUrl) {
        log.info("Getting reservation by QR URL: {}", qrUrl);
        
        Reservation reservation = reservationRepository.findByQrUrl(qrUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with QR URL: " + qrUrl));
        
        ReservationResponse response = reservationMapper.toResponse(reservation);
        enrichReservationResponse(response, reservation);
        return response;
    }

    /**
     * Consulta todas las reservas de un pago específico.
     * 
     * @param paymentId ID del pago
     * @return Lista de ReservationResponse
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByPaymentId(Long paymentId) {
        log.info("Getting reservations for payment: {}", paymentId);
        
        return reservationRepository.findByPaymentId(paymentId)
                .stream()
                .map(reservation -> {
                    ReservationResponse response = reservationMapper.toResponse(reservation);
                    enrichReservationResponse(response, reservation);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Consulta todas las reservas por estado de entrega.
     * 
     * @param deliveryStatus Estado de entrega
     * @return Lista de ReservationResponse
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByDeliveryStatus(DeliveryStatusEnum deliveryStatus) {
        log.info("Getting reservations by delivery status: {}", deliveryStatus);
        
        return reservationRepository.findByDeliveryStatus(deliveryStatus)
                .stream()
                .map(reservationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Consulta reservas por fecha de reserva.
     * 
     * @param reservationDate Fecha de reserva
     * @return Lista de ReservationResponse
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByReservationDate(LocalDateTime reservationDate) {
        log.info("Getting reservations by reservation date: {}", reservationDate);
        
        return reservationRepository.findByReservationDate(reservationDate)
                .stream()
                .map(reservationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Consulta reservas por rango de fechas de reserva.
     * 
     * @param startDate Fecha de inicio
     * @param endDate Fecha de fin
     * @return Lista de ReservationResponse
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByReservationDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting reservations by date range: {} to {}", startDate, endDate);
        
        return reservationRepository.findByReservationDateBetween(startDate, endDate)
                .stream()
                .map(reservationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Verifica si existe una reserva para un pago específico.
     * 
     * @param paymentId ID del pago
     * @return true si existe una reserva, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean existsReservationForPayment(Long paymentId) {
        log.info("Checking if payment {} has reservation", paymentId);
        
        return reservationRepository.existsByPaymentId(paymentId);
    }

    /**
     * Genera un token QR para la reserva.
     * Este método crea un token único basado en información de la reserva y pago.
     * 
     * @param paymentId ID del pago
     * @param payerId ID del pagador
     * @return String con el token QR generado
     */
    private String generateQrToken(Long paymentId, Integer payerId) {
        log.debug("Generating QR token for payment: {} and payer: {}", paymentId, payerId);
        
        // Crear un token único basado en los datos de la reserva
        String timestamp = String.valueOf(System.currentTimeMillis());
        String tokenData = String.format("RESERVATION_%d_%d_%s", paymentId, payerId, timestamp);
        
        // Codificar en Base64 para que sea más seguro y legible
        return java.util.Base64.getEncoder().encodeToString(tokenData.getBytes());
    }

    /**
     * Genera los datos del servicio en formato JSON string.
     * Este método crea un JSON con información relevante del servicio y pago.
     * 
     * @param paymentId ID del pago
     * @return String con los datos del servicio en formato JSON
     */
    private String generateServiceData(Long paymentId) {
        log.debug("Generating service data for payment: {}", paymentId);
        
        // Generar timestamp actual
        String timestamp = LocalDateTime.now().toString();
        
        // Crear JSON con datos básicos del servicio
        return String.format("{\"paymentId\":%d,\"timestamp\":\"%s\",\"serviceType\":\"tour_reservation\"}", 
                paymentId, timestamp);
    }

    /**
     * Consume/procesa una reserva, cambiando su estado y creando la cuenta por pagar al proveedor.
     * 
     * @param reservationId ID de la reserva a procesar
     * @return ReservationResponse con la información actualizada
     * @throws ResourceNotFoundException si la reserva no existe
     */
    @Transactional
    public ReservationResponse consumeReservation(Long reservationId) {
        log.info("Consuming reservation with id: {}", reservationId);
        
        // 1. Obtener la reserva
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found with id: " + reservationId));

        // Validar que la reserva esté en un estado válido para consumir
        if (reservation.getDeliveryStatus() == DeliveryStatusEnum.DELIVERED) {
            throw new IllegalStateException("Reservation is already consumed (DELIVERED)");
        }
        
        // Validar que la reserva no esté cancelada
        if (reservation.getDeliveryStatus() == DeliveryStatusEnum.CANCELED) {
            throw new IllegalStateException("Cannot consume a canceled reservation");
        }
        
        // Validar que la reserva no esté re-agendada
        if (reservation.getDeliveryStatus() == DeliveryStatusEnum.RESCHEDULED) {
            throw new IllegalStateException("Cannot consume a rescheduled reservation");
        }

        // 2. Obtener el shopping cart item relacionado
        final Long itemId = reservation.getItemId();
        ShoppingCartItem cartItem = shoppingCartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shopping cart item not found with id: " + itemId));

        // 3. Obtener el tour schedule
        TourSchedule tourSchedule = cartItem.getTourSchedule();
        if (tourSchedule == null || tourSchedule.getId() == null) {
            throw new IllegalStateException(
                    "Shopping cart item does not have an associated tour schedule");
        }

        // 4. Obtener el tour
        Tour tour = tourRepository.findById(tourSchedule.getTourId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tour not found with id: " + tourSchedule.getTourId()));

        // 5. Obtener el proveedor
        if (tour.getProvider() == null || tour.getProvider().getId() == null) {
            throw new IllegalStateException(
                    "Tour does not have an associated provider");
        }

        Provider provider = tour.getProvider();
        Integer providerId = provider.getId();

        // 6. Actualizar el estado de la reserva a DELIVERED
        reservation.setDeliveryStatus(DeliveryStatusEnum.DELIVERED);
        reservation = reservationRepository.save(reservation);

        // 7. Actualizar el estado del shopping cart item a COMPLETED
        cartItem.setStatus(ShoppingCartStatusEnum.COMPLETED);
        shoppingCartItemRepository.save(cartItem);

        // 8. Crear la cuenta por pagar al proveedor
        // Calcular el monto total del proveedor sumando providerUnitPrice * quantity de todos los details
        BigDecimal providerTotalAmount = cartItem.getDetails().stream()
                .filter(detail -> detail.getProviderUnitPrice() != null)
                .map(detail -> detail.getProviderUnitPrice()
                        .multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Si no hay providerPrice en los details, usar el precio total como fallback
        if (providerTotalAmount.compareTo(BigDecimal.ZERO) == 0) {
            providerTotalAmount = cartItem.getTotalPrice();
        }
        
        AccountPayable accountPayable = AccountPayable.builder()
                .reservationId(reservation.getReservationId())
                .providerId(providerId)
                .transactionDate(LocalDateTime.now())
                .amount(providerTotalAmount) // Usar el precio total del proveedor
                .deliveryStatus(AccountPayableStatusEnum.PENDING)
                .build();

        accountPayable = accountPayableRepository.save(accountPayable);

        log.info("Reservation {} consumed successfully. Account payable {} created for provider {} with amount {}", 
                reservationId, accountPayable.getId(), providerId, cartItem.getTotalPrice());

        // 9. Retornar la respuesta actualizada con información enriquecida del tour
        ReservationResponse response = reservationMapper.toResponse(reservation);
        enrichReservationResponse(response, reservation);
        return response;
    }

    /**
     * Obtiene las reservas para un proveedor o para el administrador con filtros y paginación.
     *
     * @param page               Número de página.
     * @param size               Tamaño de la página.
     * @param requestedProviderId ID opcional del proveedor (solo para admin).
     * @param reservationId      ID opcional para filtrar por una reserva específica.
     * @param deliveryStatus     Estado opcional para filtrar las reservas.
     * @param connectedUser      Usuario autenticado.
     * @return Una página de {@link ReservationDetailsResponse}.
     */
    @Transactional(readOnly = true)
    public PageResponse<ReservationDetailsResponse> getProviderReservations(
            int page,
            int size,
            @Nullable Integer requestedProviderId,
            @Nullable Long reservationId,
            @Nullable DeliveryStatusEnum deliveryStatus,
            Authentication connectedUser
    ) {
        //User user = (User) connectedUser.getPrincipal();
        //List<Role> roles = user.getRoles();
        Integer finalProviderId = null; // null = admin puede consultar todos

        // --- Lógica de roles ---
        /*if (Utils.isProvider(roles)) {
            Provider provider = providerService.findByUserAndStatusActive(user);
            finalProviderId = provider.getId(); // proveedor solo ve sus reservas
        } else if (Utils.isAdmin(roles)) {
            finalProviderId = requestedProviderId; // admin puede ver todos o filtrar
        } else {
            // sin rol adecuado
            return new PageResponse<>();
        }*/

        String status = (deliveryStatus != null) ? deliveryStatus.name() : null;

        // --- Consulta con JdbcTemplate (NO JPA) ---
        List<ReservationDetailsResponse> content =
                reservationNativeRepository.getProviderReservations(
                        finalProviderId,
                        reservationId,
                        status,
                        page,
                        size
                );

        Long total =
                reservationNativeRepository.countProviderReservations(
                        finalProviderId,
                        reservationId,
                        status
                );

        // --- Construcción del PageResponse ---
        int totalPages = (int) Math.ceil((double) total / size);

        return new PageResponse<>(
                content,
                page,
                size,
                total,
                totalPages,
                page == 0,
                page == (totalPages - 1)
        );
    }

    /**
     * Cancela una reserva según el motivo proporcionado.
     * Valida las condiciones de cancelación según la política del tour.
     * Crea un crédito si se cumple con las condiciones.
     * 
     * @param reservationId ID de la reserva a cancelar
     * @param request Request con el motivo de cancelación
     * @param authentication Autenticación del usuario
     * @return ReservationResponse con la reserva cancelada
     */
    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, CancelReservationRequest request, Authentication authentication) {
        log.info("Canceling reservation {} with reason: {}", reservationId, request.getCancellationReason());
        
        // Obtener usuario autenticado
        User user = (User) authentication.getPrincipal();
        
        // Buscar la reserva
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
        
        // Verificar que la reserva pertenece al usuario
        ShoppingCartItem item = shoppingCartItemRepository.findById(reservation.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Shopping cart item not found"));
        
        if (!item.getShoppingCart().getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You don't have permission to cancel this reservation");
        }
        
        // Validar que la reserva no esté ya cancelada
        if (reservation.getDeliveryStatus() == DeliveryStatusEnum.CANCELED) {
            throw new OperationNotPermittedException("Reservation is already canceled");
        }
        
        // Obtener información del tour para validaciones
        TourSchedule schedule = item.getTourSchedule();
        if (schedule == null || schedule.getTourId() == null) {
            throw new OperationNotPermittedException("Cannot cancel reservation: tour information not found");
        }
        
        Tour tour = tourRepository.findById(schedule.getTourId())
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found"));
        
        List<TourCancellationPolicy> policies = tourCancellationPolicyRepository.findByTourId(tour.getId());
        if (policies.isEmpty()) {
            throw new OperationNotPermittedException("Cancellation policy not found for this tour");
        }
        
        TourCancellationPolicy policy = policies.get(0);
        LocalDate today = LocalDate.now();
        
        // Obtener la fecha seleccionada por el usuario desde ShoppingCartItem
        LocalDate tourDate = (item.getScheduleDate() != null)
                ? item.getScheduleDate() 
                : schedule.getScheduleDate(); // Fallback si no hay fecha en ShoppingCartItem
        
        // Validar según el motivo de cancelación
        boolean canCancel = false;
        
        if (request.getCancellationReason() == CancellationReasonEnum.CANNOT_ATTEND) {
            // Validar que hoy <= fecha máxima de cancelación
            if (reservation.getMaxCancellationDate() != null) {
                canCancel = !today.isAfter(reservation.getMaxCancellationDate());
            }
            
            if (!canCancel) {
                throw new OperationNotPermittedException(
                        "Cannot cancel reservation: maximum cancellation date (" + 
                        reservation.getMaxCancellationDate() + ") has passed");
            }
        } else if (request.getCancellationReason() == CancellationReasonEnum.RAIN) {
            // Validar: allows_rain_refund = true, fecha del tour es hoy, y hay alerta DIMAR
            if (!policy.isAllowsRainRefund()) {
                throw new OperationNotPermittedException("Rain refund is not allowed for this tour");
            }
            
            if (!today.equals(tourDate)) {
                throw new OperationNotPermittedException("Rain cancellation is only allowed on the tour date");
            }
            
            // Si allowsRainRefund = true, el tour es acuático, buscar reporte DIMAR del día
            List<MaritimActivityReport> reports = maritimActivityReportRepository.findByReportDate(today);
            
            if (reports.isEmpty()) {
                throw new OperationNotPermittedException(
                        "Cannot cancel for rain: no DIMAR report found for today");
            }
            
            // Verificar que la bandera sea amarilla o roja
            boolean hasRainAlert = reports.stream()
                    .anyMatch(r -> r.getFlag() == MaritimeFlagEnum.YELLOW || 
                                  r.getFlag() == MaritimeFlagEnum.RED);
            
            if (!hasRainAlert) {
                throw new OperationNotPermittedException(
                        "Cannot cancel for rain: DIMAR report for today has green flag (normal conditions)");
            }
            
            canCancel = true;
        }
        
        // Si todas las validaciones pasan, cancelar la reserva y crear crédito
        Credit credit = null;
        if (canCancel) {
            reservation.setDeliveryStatus(DeliveryStatusEnum.CANCELED);
            reservation.setCancellationReason(request.getCancellationReason());
            reservation.setCancellationDate(LocalDateTime.now());
            reservation = reservationRepository.save(reservation);
            
            // Crear crédito para la reserva cancelada
            credit = createCreditForReservation(reservation, tour);
            
            log.info("Reservation {} canceled successfully", reservationId);
        }
        
        ReservationResponse response = reservationMapper.toResponse(reservation);
        enrichReservationResponse(response, reservation);
        
        // Agregar información del crédito creado si existe
        if (credit != null) {
            CreditResponse creditResponse = CreditResponse.builder()
                    .id(credit.getId())
                    .reservationId(credit.getReservationId())
                    .amount(credit.getAmount())
                    .creationDate(credit.getCreationDate())
                    .expirationDate(credit.getExpirationDate())
                    .status(credit.getStatus())
                    .build();
            response.setCredit(creditResponse);
        }
        
        return response;
    }
    
    /**
     * Valida si una reserva puede ser re-agendada según las políticas.
     * Solo realiza validaciones, no hace cambios.
     * 
     * @param reservationId ID de la reserva a validar
     * @param authentication Autenticación del usuario
     * @return RescheduleValidationResponse con el resultado de la validación
     */
    @Transactional(readOnly = true)
    public RescheduleValidationResponse validateRescheduleReservation(Long reservationId, Authentication authentication) {
        log.info("Validating reschedule for reservation: {}", reservationId);
        
        // Obtener usuario autenticado
        User user = (User) authentication.getPrincipal();
        
        // Buscar la reserva
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
        
        // Verificar que la reserva pertenece al usuario
        ShoppingCartItem item = shoppingCartItemRepository.findById(reservation.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Shopping cart item not found"));
        
        if (!item.getShoppingCart().getUser().getId().equals(user.getId())) {
            return RescheduleValidationResponse.builder()
                    .canReschedule(false)
                    .message("No tienes permiso para reagendar esta reserva. La reserva pertenece a otro usuario.")
                    .reason("PERMISSION_DENIED")
                    .build();
        }
        
        // Validar que la reserva no esté cancelada
        if (reservation.getDeliveryStatus() == DeliveryStatusEnum.CANCELED) {
            return RescheduleValidationResponse.builder()
                    .canReschedule(false)
                    .message("No se puede reagendar una reserva cancelada")
                    .reason("ALREADY_CANCELED")
                    .build();
        }
        
        // Obtener información del tour
        TourSchedule schedule = item.getTourSchedule();
        if (schedule == null || schedule.getTourId() == null) {
            return RescheduleValidationResponse.builder()
                    .canReschedule(false)
                    .message("No se puede reagendar: información del tour no encontrada")
                    .reason("TOUR_NOT_FOUND")
                    .build();
        }
        
        Tour tour = tourRepository.findById(schedule.getTourId())
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found"));
        
        List<TourCancellationPolicy> policies = tourCancellationPolicyRepository.findByTourId(tour.getId());
        if (policies.isEmpty()) {
            return RescheduleValidationResponse.builder()
                    .canReschedule(false)
                    .message("No se encontró política de cancelación para este tour")
                    .reason("POLICY_NOT_FOUND")
                    .build();
        }
        
        TourCancellationPolicy policy = policies.get(0);
        
        // Validar que allows_rescheduling = true
        if (!policy.isAllowsRescheduling()) {
            return RescheduleValidationResponse.builder()
                    .canReschedule(false)
                    .message("El reagendamiento no está permitido para este tour")
                    .reason("RESCHEDULING_NOT_ALLOWED")
                    .build();
        }
        
        // Validar que hoy <= fecha máxima de re-agendamiento
        LocalDate today = LocalDate.now();
        if (reservation.getMaxReschedulingDate() != null) {
            if (today.isAfter(reservation.getMaxReschedulingDate())) {
                return RescheduleValidationResponse.builder()
                        .canReschedule(false)
                        .message("No se puede reagendar: la fecha máxima de reagendamiento (" + 
                                reservation.getMaxReschedulingDate() + ") ya pasó")
                        .reason("MAX_DATE_PASSED")
                        .build();
            }
        }
        
        return RescheduleValidationResponse.builder()
                .canReschedule(true)
                .message("La reserva puede ser reagendada")
                .reason(null)
                .build();
    }

    /**
     * Re-agenda una reserva.
     * Valida políticas, compara precios y crea crédito si el precio nuevo es menor.
     * 
     * @param reservationId ID de la reserva a re-agendar
     * @param request Request con la nueva fecha
     * @param authentication Autenticación del usuario
     * @return ReservationResponse con la reserva actualizada
     */
    @Transactional
    public ReservationResponse rescheduleReservation(Long reservationId, RescheduleReservationRequest request, Authentication authentication) {
        log.info("Rescheduling reservation {} to date: {}", reservationId, request.getNewDate());
        
        // Obtener usuario autenticado
        User user = (User) authentication.getPrincipal();
        
        // Buscar la reserva
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
        
        // Verificar que la reserva pertenece al usuario
        ShoppingCartItem item = shoppingCartItemRepository.findById(reservation.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Shopping cart item not found"));
        
        if (!item.getShoppingCart().getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You don't have permission to reschedule this reservation");
        }
        
        // Validar que la reserva no esté cancelada
        if (reservation.getDeliveryStatus() == DeliveryStatusEnum.CANCELED) {
            throw new OperationNotPermittedException("Cannot reschedule a canceled reservation");
        }
        
        // Obtener información del tour
        TourSchedule schedule = item.getTourSchedule();
        if (schedule == null || schedule.getTourId() == null) {
            throw new OperationNotPermittedException("Cannot reschedule reservation: tour information not found");
        }
        
        Tour tour = tourRepository.findById(schedule.getTourId())
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found"));
        
        List<TourCancellationPolicy> policies = tourCancellationPolicyRepository.findByTourId(tour.getId());
        if (policies.isEmpty()) {
            throw new OperationNotPermittedException("Cancellation policy not found for this tour");
        }
        
        TourCancellationPolicy policy = policies.get(0);
        
        // Validar que allows_rescheduling = true
        if (!policy.isAllowsRescheduling()) {
            throw new OperationNotPermittedException("Rescheduling is not allowed for this tour");
        }
        
        // Validar que hoy <= fecha máxima de re-agendamiento
        LocalDate today = LocalDate.now();
        if (reservation.getMaxReschedulingDate() != null) {
            if (today.isAfter(reservation.getMaxReschedulingDate())) {
                throw new OperationNotPermittedException(
                        "Cannot reschedule: maximum rescheduling date (" + 
                        reservation.getMaxReschedulingDate() + ") has passed");
            }
        }
        
        // Obtener precio actual del item
        BigDecimal currentPrice = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
        
        // Buscar TourSchedule para la nueva fecha y validar que esté disponible
        TourSchedule newSchedule = tourScheduleRepository.findByTourIdAndScheduleDate(
                schedule.getTourId(), request.getNewDate())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tour schedule not found for tour " + schedule.getTourId() + 
                        " on date " + request.getNewDate()));
        
        // Validar que el schedule esté disponible
        if (newSchedule.getStatus() != TourScheduleStatusEnum.AVAILABLE) {
            throw new OperationNotPermittedException(
                    "The tour schedule for date " + request.getNewDate() + " is not available for rescheduling.");
        }
        
        // Calcular precio nuevo basado en el slot y los details del item actual
        BigDecimal newPrice = calculatePriceForNewDate(item);
        
        // Comparar precios
        int priceComparison = newPrice.compareTo(currentPrice);
        
        Credit credit = null;
        
        if (priceComparison > 0) {
            // Precio nuevo es mayor - rechazar reagendamiento
            throw new OperationNotPermittedException(
                    String.format("The price for the new date (%.2f) is higher than the current price (%.2f). " +
                            "Rescheduling is not allowed when the price increases.", 
                            newPrice, currentPrice));
        } else if (priceComparison < 0) {
            // Precio nuevo es menor - crear crédito SOLO por la diferencia (lo pagado - nuevo precio)
            BigDecimal priceDifference = currentPrice.subtract(newPrice);
            credit = Credit.builder()
                    .reservationId(reservation.getReservationId())
                    .amount(priceDifference)
                    .creationDate(LocalDate.now())
                    .expirationDate(LocalDate.now().plusYears(1))
                    .status(CreditStatusEnum.CREATED)
                    .build();
            credit = creditRepository.save(credit);
            log.info("Credit created for price difference: reservationId={}, amount={} (currentPrice={} - newPrice={})", 
                    reservationId, priceDifference, currentPrice, newPrice);
        }
        // Si priceComparison == 0 (precios iguales): NO se crea crédito, solo se actualiza la fecha
        
        // Recalcular fechas máximas de cancelación y re-agendamiento para la nueva fecha
        // Reutilizar las políticas ya obtenidas anteriormente (ya están validadas arriba)
        LocalDate newMaxCancellationDate = calculateMaxCancellationDate(
                policy.getCancellationPolicyType(), 
                request.getNewDate()
        );
        
        // Calcular fecha máxima de re-agendamiento (2 días antes del tour)
        LocalDate newMaxReschedulingDate = request.getNewDate().minusDays(2);
        
        log.info("Recalculating dates for reschedule - newDate: {}, maxCancellation: {}, maxRescheduling: {}", 
                request.getNewDate(), newMaxCancellationDate, newMaxReschedulingDate);
        
        // Actualizar la fecha en ShoppingCartItem
        item.setScheduleDate(request.getNewDate());
        // Actualizar el precio si cambió
        if (priceComparison != 0) {
            item.setTotalPrice(newPrice);
            // Actualizar los detalles del item con los nuevos precios
            updateItemDetailsForNewPrice(item);
        }
        shoppingCartItemRepository.save(item);
        
        // Actualizar reservationDate con la nueva fecha (item.getScheduleDate())
        reservation.setReservationDate(request.getNewDate().atStartOfDay());
        
        // Actualizar las fechas máximas en la reserva ANTES de cambiar el estado
        reservation.setMaxCancellationDate(newMaxCancellationDate);
        reservation.setMaxReschedulingDate(newMaxReschedulingDate);
        reservation.setDeliveryStatus(DeliveryStatusEnum.RESCHEDULED);
        
        // Guardar la reserva con todas las actualizaciones
        reservation = reservationRepository.saveAndFlush(reservation);
        
        log.info("After saveAndFlush - Reservation {} dates: maxCancellation={}, maxRescheduling={}", 
                reservationId, reservation.getMaxCancellationDate(), reservation.getMaxReschedulingDate());
        
        // Construir respuesta con la reserva re-agendada
        ReservationResponse response = reservationMapper.toResponse(reservation);
        
        log.info("After mapper - Response dates: maxCancellation={}, maxRescheduling={}", 
                response.getMaxCancellationDate(), response.getMaxReschedulingDate());
        enrichReservationResponse(response, reservation);
        
        // Agregar información del crédito creado si existe
        if (credit != null) {
            CreditResponse creditResponse = CreditResponse.builder()
                    .id(credit.getId())
                    .reservationId(credit.getReservationId())
                    .amount(credit.getAmount())
                    .creationDate(credit.getCreationDate())
                    .expirationDate(credit.getExpirationDate())
                    .status(credit.getStatus())
                    .build();
            response.setCredit(creditResponse);
        }
        
        return response;
    }
    
    /**
     * Crea un crédito para una reserva cancelada o re-agendada.
     * 
     * @param reservation Reserva para la cual crear el crédito
     * @param tour Tour asociado a la reserva
     * @return El crédito creado
     */
    private Credit createCreditForReservation(Reservation reservation, Tour tour) {
        // Obtener el monto total desde el item del carrito asociado a la reserva
        ShoppingCartItem cartItem = shoppingCartItemRepository.findById(reservation.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Shopping cart item not found for reservation"));
        
        // El monto del crédito es el total del item del carrito (precio de la reserva)
        BigDecimal creditAmount = cartItem.getTotalPrice() != null ? 
                cartItem.getTotalPrice() : BigDecimal.ZERO;
        
        // Crear crédito con fecha de vencimiento de 1 año desde hoy
        Credit credit = Credit.builder()
                .reservationId(reservation.getReservationId())
                .amount(creditAmount)
                .creationDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusYears(1))
                .status(CreditStatusEnum.CREATED)
                .build();
        
        credit = creditRepository.save(credit);
        log.info("Credit created for reservation {}: amount={}, expiration={}", 
                reservation.getReservationId(), creditAmount, credit.getExpirationDate());
        return credit;
    }
    
    /**
     * Calcula el precio para una nueva fecha basándose en el slot y los details del item actual.
     * 
     * @param item Item del carrito con los details actuales
     * @return Precio total calculado para la nueva fecha
     */
    private BigDecimal calculatePriceForNewDate(ShoppingCartItem item) {
        if (item.getSlot() == null || item.getDetails() == null || item.getDetails().isEmpty()) {
            throw new IllegalStateException("Cannot calculate price: item missing slot or details");
        }
        
        // Usar el slot que ya tiene el item (no buscar en el nuevo schedule)
        TourScheduleConfigSlot slot = item.getSlot();
        
        // Calcular precio total usando los details del item actual y los precios del slot
        BigDecimal totalPrice = BigDecimal.ZERO;
        
        for (ShoppingCartItemDetail detail : item.getDetails()) {
            // Buscar precio en el slot para el mismo ageType
            BigDecimal unitPrice = slot.getPrices().stream()
                    .filter(price -> price.getAgeType().equals(detail.getAgeType()))
                    .findFirst()
                    .map(TourScheduleConfigPrice::getPrice)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Price not found for ageType " + detail.getAgeType() + 
                            " in slot " + slot.getId()));
            
            BigDecimal detailTotalPrice = unitPrice.multiply(BigDecimal.valueOf(detail.getQuantity()));
            totalPrice = totalPrice.add(detailTotalPrice);
        }
        
        return totalPrice;
    }
    
    /**
     * Actualiza los detalles del item con los nuevos precios para la nueva fecha.
     * 
     * @param item Item del carrito a actualizar
     */
    private void updateItemDetailsForNewPrice(ShoppingCartItem item) {
        if (item.getSlot() == null || item.getDetails() == null || item.getDetails().isEmpty()) {
            return;
        }
        
        // Usar el slot que ya tiene el item
        TourScheduleConfigSlot slot = item.getSlot();
        
        // Actualizar cada detail con el precio del slot
        for (ShoppingCartItemDetail detail : item.getDetails()) {
            BigDecimal newUnitPrice = slot.getPrices().stream()
                    .filter(price -> price.getAgeType().equals(detail.getAgeType()))
                    .findFirst()
                    .map(TourScheduleConfigPrice::getPrice)
                    .orElse(detail.getUnitPrice()); // Mantener precio actual si no se encuentra
            
            detail.setUnitPrice(newUnitPrice);
            detail.setTotalPrice(newUnitPrice.multiply(BigDecimal.valueOf(detail.getQuantity())));
        }
    }
    
    /**
     * Calcula la fecha máxima de cancelación según el tipo de política.
     * Busca la configuración en app_config para obtener los días.
     * 
     * @param policyType Tipo de política de cancelación
     * @param tourDate Fecha del tour
     * @return Fecha máxima de cancelación
     */
    private LocalDate calculateMaxCancellationDate(CancellationPolicyTypeEnum policyType, LocalDate tourDate) {
        if (policyType == null || tourDate == null) {
            return null;
        }

        try {
            // Buscar la configuración de políticas de cancelación
            Map<String, Object> config = appConfigService.getConfigValue(ConfigKeyEnum.CANCELLATION_POLICY);
            
            // Buscar la política en el JSON (usar español como default)
            @SuppressWarnings("unchecked")
            Map<String, Object> esPolicies = (Map<String, Object>) config.get("es");
            if (esPolicies == null) {
                // Fallback al método anterior si no hay configuración
                return getDefaultCancellationDate(policyType, tourDate);
            }
            
            // Mapear el enum al nombre en el JSON
            String policyKey = switch (policyType) {
                case FLEXIBLE -> "Flexible";
                case STANDARD -> "Standard";
                case MODERATE -> "Moderate";
                case STRICT -> "Strict";
            };
            
            @SuppressWarnings("unchecked")
            Map<String, Object> policy = (Map<String, Object>) esPolicies.get(policyKey);
            if (policy == null) {
                return getDefaultCancellationDate(policyType, tourDate);
            }
            
            // Obtener los días desde la configuración
            Object daysObj = policy.get("days");
            if (daysObj instanceof Number) {
                int days = ((Number) daysObj).intValue();
                return tourDate.minusDays(days);
            }
        } catch (Exception e) {
            log.warn("Error getting cancellation policy from config, using default: {}", e.getMessage());
        }
        
        // Fallback al método anterior si hay algún error
        return getDefaultCancellationDate(policyType, tourDate);
    }
    
    /**
     * Método de fallback con los valores por defecto
     */
    private LocalDate getDefaultCancellationDate(CancellationPolicyTypeEnum policyType, LocalDate tourDate) {
        return switch (policyType) {
            case FLEXIBLE -> tourDate.minusDays(1); // hasta 24 horas antes
            case STANDARD -> tourDate.minusDays(2); // hasta 48 horas antes
            case MODERATE -> tourDate.minusDays(4); // hasta 4 días antes
            case STRICT -> tourDate.minusDays(7); // hasta 7 días antes
        };
    }
}
