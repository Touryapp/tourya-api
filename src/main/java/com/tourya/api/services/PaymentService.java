package com.tourya.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.*;
import com.tourya.api.models.request.CreatePaymentRequest;
import com.tourya.api.models.responses.PaymentResponse;
import com.tourya.api.models.responses.ReservationResponse;
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

        return ReservationResponse.builder()
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
}