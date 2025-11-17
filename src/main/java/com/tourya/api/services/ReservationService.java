package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.config.security.JwtService;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import com.tourya.api.constans.enums.AccountPayableStatusEnum;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.ReservationMapper;
import com.tourya.api.models.responses.ReservationDetailsResponse;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private final ReservationNativeRepository reservationNativeRepository;
    /**
     * Método createReservation removido - las reservas se crean automáticamente con los pagos
     */

    /**
     * Consulta una reserva por su ID.
     * 
     * @param id ID de la reserva
     * @return ReservationResponse con la información de la reserva
     * @throws ResourceNotFoundException si la reserva no existe
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        log.info("Getting reservation by id: {}", id);
        
        return reservationRepository.findById(id)
                .map(reservationMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
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
        
        return reservationRepository.findByQrUrl(qrUrl)
                .map(reservationMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with QR URL: " + qrUrl));
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
                .map(reservationMapper::toResponse)
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
        AccountPayable accountPayable = AccountPayable.builder()
                .reservationId(reservation.getReservationId())
                .providerId(providerId)
                .transactionDate(LocalDateTime.now())
                .amount(cartItem.getTotalPrice()) // Usar el precio total del item
                .deliveryStatus(AccountPayableStatusEnum.PENDING)
                .build();

        accountPayable = accountPayableRepository.save(accountPayable);

        log.info("Reservation {} consumed successfully. Account payable {} created for provider {} with amount {}", 
                reservationId, accountPayable.getId(), providerId, cartItem.getTotalPrice());

        // 9. Retornar la respuesta actualizada
        return reservationMapper.toResponse(reservation);
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

}