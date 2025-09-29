package com.tourya.api.services;

import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Payment;
import com.tourya.api.models.Reservation;
import com.tourya.api.repository.PaymentRepository;
import com.tourya.api.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Servicio para manejar códigos QR de reservas y subirlos a S3.
 * Sigue la misma lógica que TourGalleryService para consistencia.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationQrService {

    private final QrCodeService qrCodeService;
    private final S3Service s3Service;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Genera y sube un código QR para una reserva específica.
     * Si ya existe un QR para esa reserva, lo elimina y sube uno nuevo.
     * 
     * @param reservationId ID de la reserva
     * @return URL del QR subido a S3
     * @throws ResourceNotFoundException si la reserva no existe
     */
    @Transactional
    public String generateAndUploadQrCode(Long reservationId) {
        log.info("Generating and uploading QR code for reservation: {}", reservationId);
        
        // 1. Validar que la reserva existe
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
        
        // 2. Obtener el pago asociado
        Payment payment = paymentRepository.findById(reservation.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for reservation: " + reservationId));
        
        // 3. Generar contenido del QR
        String qrContent = buildQrContent(payment, reservation);
        
        try {
            // 4. Generar imagen QR
            byte[] qrImageBytes = qrCodeService.generateQrCodeImage(qrContent);
            
            // 5. Crear MultipartFile
            String filename = "reservation_" + reservationId + "_qr.png";
            MultipartFile qrMultipartFile = qrCodeService.createQrMultipartFile(qrImageBytes, filename);
            
            // 6. Si ya existe un QR, eliminarlo primero (siguiendo lógica de TourGalleryService)
            if (reservation.getQrUrl() != null && !reservation.getQrUrl().isEmpty()) {
                log.info("Deleting existing QR for reservation: {}", reservationId);
                s3Service.deleteFile(reservation.getQrUrl());
            }
            
            // 7. Subir nuevo QR a S3
            String s3Prefix = "reservations/" + reservationId;
            String qrUrl = s3Service.uploadFile(s3Prefix, qrMultipartFile);
            
            // 8. Actualizar la reserva con la nueva URL del QR
            reservation.setQrUrl(qrUrl);
            reservationRepository.save(reservation);
            
            log.info("QR code uploaded successfully for reservation: {} at URL: {}", reservationId, qrUrl);
            return qrUrl;
            
        } catch (Exception e) {
            log.error("Error generating and uploading QR code for reservation: {}", reservationId, e);
            throw new RuntimeException("Failed to generate and upload QR code", e);
        }
    }

    /**
     * Regenera y actualiza el QR de una reserva existente.
     * 
     * @param reservationId ID de la reserva
     * @return URL del nuevo QR subido a S3
     */
    @Transactional
    public String regenerateQrCode(Long reservationId) {
        log.info("Regenerating QR code for reservation: {}", reservationId);
        return generateAndUploadQrCode(reservationId);
    }

    /**
     * Elimina el QR de una reserva de S3 y actualiza la base de datos.
     * 
     * @param reservationId ID de la reserva
     */
    @Transactional
    public void deleteQrCode(Long reservationId) {
        log.info("Deleting QR code for reservation: {}", reservationId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
        
        if (reservation.getQrUrl() != null && !reservation.getQrUrl().isEmpty()) {
            s3Service.deleteFile(reservation.getQrUrl());
            reservation.setQrUrl(null);
            reservationRepository.save(reservation);
            log.info("QR code deleted successfully for reservation: {}", reservationId);
        } else {
            log.warn("No QR code found for reservation: {}", reservationId);
        }
    }

    /**
     * Construye el contenido del código QR con redirección a la página web.
     * 
     * @param payment Datos del pago
     * @param reservation Datos de la reserva
     * @return URL de redirección con parámetros
     */
    private String buildQrContent(Payment payment, Reservation reservation) {
        // URL base de redirección
        String baseUrl = "http://44.203.38.85:8080/home";
        
        // Construir URL con parámetros de la reserva
        StringBuilder qrUrl = new StringBuilder(baseUrl);
        qrUrl.append("?reservationId=").append(reservation.getReservationId());
        qrUrl.append("&paymentId=").append(payment.getPaymentId());
        qrUrl.append("&transactionId=").append(payment.getTransactionId());
        qrUrl.append("&payer=").append(java.net.URLEncoder.encode(payment.getPayerName(), java.nio.charset.StandardCharsets.UTF_8));
        qrUrl.append("&email=").append(java.net.URLEncoder.encode(payment.getPayerEmail(), java.nio.charset.StandardCharsets.UTF_8));
        qrUrl.append("&reservationDate=").append(reservation.getReservationDate());
        qrUrl.append("&status=").append(reservation.getDeliveryStatus());
        
        return qrUrl.toString();
    }
}
