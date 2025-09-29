package com.tourya.api.services;

import com.tourya.api.config.security.JwtService;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Payment;
import com.tourya.api.models.Reservation;
import com.tourya.api.models.mapper.ReservationMapper;
import com.tourya.api.models.request.CreateReservationRequest;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.repository.PaymentRepository;
import com.tourya.api.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Crea una nueva reserva después de un pago exitoso.
     * Si no se proporciona serviceData, se genera automáticamente.
     * 
     * @param request Datos de la reserva a crear
     * @return ReservationResponse con la información de la reserva creada
     * @throws ResourceNotFoundException si el pago no existe
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        log.info("Creating reservation for payment: {}", request.getPaymentId());
        
        return Optional.ofNullable(request)
                .map(this::validatePayment)
                .map(payment -> {
                    // Generar QR token si no se proporciona
                    if (request.getQrToken() == null || request.getQrToken().trim().isEmpty()) {
                        String qrToken = generateQrToken(request.getPaymentId(), request.getPayerId());
                        request.setQrToken(qrToken);
                        log.debug("Generated QR token for payment: {}", request.getPaymentId());
                    }
                    
                    // Generar serviceData si no se proporciona
                    if (request.getServiceData() == null || request.getServiceData().trim().isEmpty()) {
                        request.setServiceData(generateServiceData(request.getPaymentId()));
                        log.debug("Generated serviceData for payment: {}", request.getPaymentId());
                    }
                    
                    Reservation reservation = reservationMapper.toEntity(request);
                    reservation.setPayment(payment);
                    return reservationRepository.save(reservation);
                })
                .map(reservationMapper::toResponse)
                .orElseThrow(() -> new IllegalStateException("Error creating reservation"));
    }

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
     * Valida que el pago existe.
     * 
     * @param request Request con el ID del pago
     * @return Payment si existe
     * @throws ResourceNotFoundException si el pago no existe
     */
    private Payment validatePayment(CreateReservationRequest request) {
        return paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + request.getPaymentId()));
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
}