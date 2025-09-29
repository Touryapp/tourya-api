package com.tourya.api.models.mapper;

import com.tourya.api.models.Reservation;
import com.tourya.api.models.request.CreateReservationRequest;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.services.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre entidades Reservation y DTOs.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class ReservationMapper {

    private final QrCodeService qrCodeService;

    /**
     * Convierte un CreateReservationRequest a entidad Reservation
     */
    public Reservation toEntity(CreateReservationRequest request) {
        if (request == null) {
            return null;
        }

        return Reservation.builder()
                .paymentId(request.getPaymentId())
                .qrUrl(request.getQrToken()) // Mapear qrToken a qrUrl
                .reservationDate(request.getReservationDate())
                .deliveryStatus(request.getDeliveryStatus())
                .build();
    }

    /**
     * Convierte una entidad Reservation a ReservationResponse
     */
    public ReservationResponse toResponse(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        // Generar imagen QR automáticamente
        String qrImageBase64 = generateQrImage(reservation);

        return ReservationResponse.builder()
                .id(reservation.getReservationId())
                .paymentId(reservation.getPaymentId())
                .reservationDate(reservation.getReservationDate())
                .deliveryStatus(reservation.getDeliveryStatus())
                .qrImageBase64(qrImageBase64)
                .createdDate(reservation.getCreatedDate())
                .lastModifiedDate(reservation.getLastModifiedDate())
                .createdBy(reservation.getCreatedBy())
                .lastModifiedBy(reservation.getLastModifiedBy())
                .build();
    }

    /**
     * Actualiza una entidad Reservation con datos de un request
     */
    public void updateEntity(Reservation reservation, CreateReservationRequest request) {
        if (reservation == null || request == null) {
            return;
        }

        reservation.setPaymentId(request.getPaymentId());
        reservation.setQrUrl(request.getQrToken());
        reservation.setReservationDate(request.getReservationDate());
        reservation.setDeliveryStatus(request.getDeliveryStatus());
    }

    /**
     * Genera la imagen QR con los datos clave de la reserva
     */
    private String generateQrImage(Reservation reservation) {
        try {
            // Crear contenido del QR con datos clave de la reserva
            StringBuilder qrContent = new StringBuilder();
            qrContent.append("RESERVATION_ID:").append(reservation.getReservationId()).append("\n");
            qrContent.append("PAYMENT_ID:").append(reservation.getPaymentId()).append("\n");
            qrContent.append("RESERVATION_DATE:").append(reservation.getReservationDate()).append("\n");
            qrContent.append("DELIVERY_STATUS:").append(reservation.getDeliveryStatus()).append("\n");
            qrContent.append("QR_URL:").append(reservation.getQrUrl() != null ? reservation.getQrUrl() : "N/A").append("\n");
            
            // Generar imagen QR
            byte[] qrImageBytes = qrCodeService.generateQrCodeImage(qrContent.toString());
            
            // Convertir a Base64
            return java.util.Base64.getEncoder().encodeToString(qrImageBytes);
            
        } catch (Exception e) {
            // Si hay error generando QR, devolver string vacío
            return "";
        }
    }
}