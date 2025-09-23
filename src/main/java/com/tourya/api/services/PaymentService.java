package com.tourya.api.services;

import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Payment;
import com.tourya.api.models.ShoppingCartItem;
import com.tourya.api.models.mapper.PaymentMapper;
import com.tourya.api.models.request.CreatePaymentRequest;
import com.tourya.api.models.request.CreateReservationRequest;
import com.tourya.api.models.responses.PaymentResponse;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.repository.PaymentRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import com.tourya.api.constans.enums.OrderStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar pagos.
 * Implementa funcionalidad para crear y consultar pagos siguiendo programación funcional limpia.
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
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final PaymentMapper paymentMapper;
    private final ReservationService reservationService;

    /**
     * Crea un nuevo pago para un item del carrito de compras.
     * Si el pago es exitoso (PAID), crea automáticamente una reserva.
     * 
     * @param request Datos del pago a crear
     * @return PaymentResponse con la información del pago creado
     * @throws ResourceNotFoundException si el item del carrito no existe
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for shopping cart item: {}", request.getShoppingCartItemId());
        
        // Validar que el request no sea null
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }
        
        // Validar que el item del carrito existe
        ShoppingCartItem shoppingCartItem = shoppingCartItemRepository.findById(request.getShoppingCartItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shopping cart item not found with id: " + request.getShoppingCartItemId()));
        
        // Crear la entidad Payment
        Payment payment = paymentMapper.toEntity(request);
        payment.setShoppingCartItem(shoppingCartItem);
        
        // Guardar el pago
        Payment savedPayment = paymentRepository.save(payment);
        
        // Si el pago es exitoso, crear automáticamente una reserva
        if (savedPayment.getStatus() == OrderStatusEnum.PAID) {
            log.info("Payment is successful (PAID), creating automatic reservation for payment id: {}", savedPayment.getId());
            createAutomaticReservation(savedPayment);
        }
        
        // Convertir a response
        return paymentMapper.toResponse(savedPayment);
    }

    /**
     * Crea automáticamente una reserva cuando el pago es exitoso.
     * 
     * @param payment Pago exitoso
     */
    private void createAutomaticReservation(Payment payment) {
        try {
            CreateReservationRequest reservationRequest = CreateReservationRequest.builder()
                    .paymentId(payment.getId())
                    .reservationDate(payment.getScheduleDate() != null ? 
                            payment.getScheduleDate().atTime(9, 0) : 
                            java.time.LocalDateTime.now().plusDays(1))
                    .deliveryStatus(com.tourya.api.constans.enums.DeliveryStatusEnum.PENDING)
                    .serviceResponsibleName("Guía de Turismo - Pendiente Asignación")
                    .serviceResponsibleEmail("guias@tourya.com")
                    .serviceResponsibleId(999) // ID temporal hasta asignar guía real
                    .payerName(payment.getPayerName())
                    .payerEmail(payment.getPayerEmail())
                    .payerId(payment.getPayerId())
                    .providerRating(null) // Sin calificación inicial
                    .comments("Reserva creada automáticamente. Pendiente asignación de guía responsable.")
                    .build();
            
            ReservationResponse reservation = reservationService.createReservation(reservationRequest);
            log.info("Automatic reservation created successfully with id: {} - Pending guide assignment", reservation.getId());
            
        } catch (Exception e) {
            log.error("Error creating automatic reservation for payment {}: {}", payment.getId(), e.getMessage());
            // No lanzamos la excepción para no afectar el flujo del pago
        }
    }

    /**
     * Consulta un pago por su ID.
     * 
     * @param id ID del pago
     * @return PaymentResponse con la información del pago
     * @throws ResourceNotFoundException si el pago no existe
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        log.info("Getting payment by id: {}", id);
        
        return paymentRepository.findById(id)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }


    /**
     * Consulta un pago por su ID de transacción.
     * 
     * @param transactionId ID de transacción
     * @return PaymentResponse con la información del pago
     * @throws ResourceNotFoundException si el pago no existe
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        log.info("Getting payment by transaction id: {}", transactionId);
        
        return paymentRepository.findByTransactionId(transactionId)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with transaction id: " + transactionId));
    }


    /**
     * Consulta todos los pagos de un item del carrito de compras.
     * 
     * @param shoppingCartItemId ID del item del carrito
     * @return Lista de PaymentResponse
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByShoppingCartItemId(Long shoppingCartItemId) {
        log.info("Getting payments for shopping cart item: {}", shoppingCartItemId);
        
        return paymentRepository.findByShoppingCartItemId(shoppingCartItemId)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Consulta todos los pagos por estado.
     * 
     * @param status Estado del pago
     * @return Lista de PaymentResponse
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStatus(String status) {
        log.info("Getting payments by status: {}", status);
        
        return paymentRepository.findByStatus(status)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Verifica si existe un pago exitoso para un item del carrito.
     * 
     * @param shoppingCartItemId ID del item del carrito
     * @return true si existe un pago aprobado, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean hasApprovedPayment(Long shoppingCartItemId) {
        log.info("Checking if shopping cart item {} has approved payment", shoppingCartItemId);
        
        return paymentRepository.existsApprovedPaymentByShoppingCartItemId(shoppingCartItemId);
    }

    /**
     * Consulta todos los pagos de un pagador específico.
     * 
     * @param payerId ID del pagador
     * @return Lista de PaymentResponse
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByPayerId(Integer payerId) {
        log.info("Getting payments by payer id: {}", payerId);
        
        return paymentRepository.findByPayerId(payerId)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Consulta todos los pagos por email del pagador.
     * 
     * @param payerEmail Email del pagador
     * @return Lista de PaymentResponse
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByPayerEmail(String payerEmail) {
        log.info("Getting payments by payer email: {}", payerEmail);
        
        return paymentRepository.findByPayerEmail(payerEmail)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

}
