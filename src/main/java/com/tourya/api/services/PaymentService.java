package com.tourya.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.*;
import com.tourya.api.models.request.CreatePaymentRequest;
import com.tourya.api.models.responses.PaymentResponse;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.models.responses.ReservationItemResponse;
import com.tourya.api.models.responses.ServiceResponsibleResponse;
import com.tourya.api.models.responses.PayerResponse;
import com.tourya.api.repository.*;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ReservationItemRepository reservationItemRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final QrCodeService qrCodeService;
    private final ReservationQrService reservationQrService;
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

        // Crear la reserva automáticamente
        Reservation reservation = createReservation(savedPayment);
        
        // Actualizar el pago con el ID de la reserva
        savedPayment.setReservationId(reservation.getReservationId());
        paymentRepository.save(savedPayment);

        // Crear los items de la reserva
        createReservationItems(reservation, request.getItems(), items);

        // Actualizar el estado de los items del carrito a PAGADO
        updateShoppingCartItemsStatus(items, reservation.getReservationId());

        // Verificar si el carrito debe pasar a inactivo
        checkAndDeactivateCart(items);

        log.info("Payment and reservation created successfully. Payment ID: {}, Reservation ID: {}", 
                savedPayment.getPaymentId(), reservation.getReservationId());

        return buildPaymentResponse(savedPayment, reservation);
    }

    /**
     * Crea una reserva automáticamente después del pago.
     */
    private Reservation createReservation(Payment payment) {
        log.debug("Creating reservation for payment: {}", payment.getPaymentId());

        Reservation reservation = Reservation.builder()
                .paymentId(payment.getPaymentId())
                .qrUrl(null) // Se generará y subirá después
                .reservationDate(LocalDateTime.now().plusDays(1)) // Fecha por defecto
                .deliveryStatus(DeliveryStatusEnum.PENDING)
                .build();

        reservation = reservationRepository.save(reservation);
        
        // Generar y subir QR code a S3
        String qrUrl = reservationQrService.generateAndUploadQrCode(reservation.getReservationId());
        reservation.setQrUrl(qrUrl);
        reservation = reservationRepository.save(reservation);
        
        return reservation;
    }

    /**
     * Crea los items de la reserva.
     */
    private void createReservationItems(Reservation reservation, 
                                      List<CreatePaymentRequest.PaymentItemRequest> requestItems,
                                      List<ShoppingCartItem> cartItems) {
        
        for (CreatePaymentRequest.PaymentItemRequest requestItem : requestItems) {
            ShoppingCartItem cartItem = cartItems.stream()
                    .filter(item -> item.getId().equals(requestItem.getShoppingCartItemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

            ReservationItem reservationItem = ReservationItem.builder()
                    .reservationId(reservation.getReservationId())
                    .itemId(cartItem.getId())
                    .serviceResponsibleName(requestItem.getServiceResponsible().getName())
                    .serviceResponsibleEmail(requestItem.getServiceResponsible().getEmail())
                    .serviceResponsiblePhone(requestItem.getServiceResponsible().getPhone())
                    .build();

            reservationItemRepository.save(reservationItem);
        }
    }

    /**
     * Actualiza el estado de los items del carrito a PAGADO y asigna reservationId.
     */
    private void updateShoppingCartItemsStatus(List<ShoppingCartItem> items, Long reservationId) {
        for (ShoppingCartItem item : items) {
            item.setStatus(ShoppingCartStatusEnum.PAID);
            item.setReservationId(reservationId);
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
     * Construye la respuesta del pago.
     */
    private PaymentResponse buildPaymentResponse(Payment payment, Reservation reservation) {
        // Construir items de la reserva
        List<ReservationItemResponse> itemResponses = 
                reservationItemRepository.findByReservationId(reservation.getReservationId())
                        .stream()
                        .map(this::buildReservationItemResponse)
                        .collect(Collectors.toList());

        // Construir respuesta de la reserva
        ReservationResponse reservationResponse = ReservationResponse.builder()
                .reservationId(reservation.getReservationId())
                .paymentId(reservation.getPaymentId())
                .qrUrl(reservation.getQrUrl()) // URL del QR en S3
                .reservationDate(reservation.getReservationDate())
                .deliveryStatus(reservation.getDeliveryStatus())
                .createdDate(reservation.getCreatedDate())
                .items(itemResponses)
                .lastModifiedDate(reservation.getLastModifiedDate())
                .createdBy(reservation.getCreatedBy())
                .lastModifiedBy(reservation.getLastModifiedBy())
                .build();

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
                .reservation(reservationResponse)
                .payer(payerResponse)
                .createdDate(payment.getCreatedDate())
                .lastModifiedDate(payment.getLastModifiedDate())
                .createdBy(payment.getCreatedBy())
                .lastModifiedBy(payment.getLastModifiedBy())
                .build();
    }

    /**
     * Construye la respuesta de un item de reserva.
     */
    private ReservationItemResponse buildReservationItemResponse(ReservationItem item) {
        // Construir respuesta del responsable del servicio
        ServiceResponsibleResponse serviceResponsible = ServiceResponsibleResponse.builder()
                .name(item.getServiceResponsibleName())
                .email(item.getServiceResponsibleEmail())
                .phone(item.getServiceResponsiblePhone() != null ? 
                      Long.parseLong(item.getServiceResponsiblePhone().replaceAll("[^0-9]", "")) : null)
                .build();

        return ReservationItemResponse.builder()
                .shoppingCartItemId(item.getItemId())
                .serviceResponsible(serviceResponsible)
                .build();
    }

    /**
     * Obtiene un pago por su ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        log.info("Getting payment by id: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + paymentId));

        Reservation reservation = reservationRepository.findByPaymentId(payment.getPaymentId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found for payment: " + paymentId));

        return buildPaymentResponse(payment, reservation);
    }

    /**
     * Obtiene un pago por ID de transacción.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        log.info("Getting payment by transaction id: {}", transactionId);
        
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with transaction id: " + transactionId));

        Reservation reservation = reservationRepository.findByPaymentId(payment.getPaymentId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found for payment: " + payment.getPaymentId()));

        return buildPaymentResponse(payment, reservation);
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
}