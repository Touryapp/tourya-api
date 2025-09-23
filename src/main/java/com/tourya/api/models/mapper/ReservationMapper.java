package com.tourya.api.models.mapper;

import com.tourya.api.models.Reservation;
import com.tourya.api.models.request.CreateReservationRequest;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.services.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
                .qrToken(request.getQrToken())
                .reservationDate(request.getReservationDate())
                .deliveryStatus(request.getDeliveryStatus())
                .serviceResponsibleName(request.getServiceResponsibleName())
                .serviceResponsibleEmail(request.getServiceResponsibleEmail())
                .serviceResponsibleId(request.getServiceResponsibleId())
                .payerName(request.getPayerName())
                .payerEmail(request.getPayerEmail())
                .payerId(request.getPayerId())
                .providerRating(request.getProviderRating())
                .comments(request.getComments())
                .serviceData(request.getServiceData())
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
                .id(reservation.getId())
                .paymentId(reservation.getPayment() != null ? reservation.getPayment().getId() : null)
                .reservationDate(reservation.getReservationDate())
                .deliveryStatus(reservation.getDeliveryStatus())
                .serviceResponsibleName(reservation.getServiceResponsibleName())
                .serviceResponsibleEmail(reservation.getServiceResponsibleEmail())
                .serviceResponsibleId(reservation.getServiceResponsibleId())
                .payerName(reservation.getPayerName())
                .payerEmail(reservation.getPayerEmail())
                .payerId(reservation.getPayerId())
                .providerRating(reservation.getProviderRating())
                .comments(reservation.getComments())
                .serviceData(reservation.getServiceData())
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

        reservation.setQrToken(request.getQrToken());
        reservation.setReservationDate(request.getReservationDate());
        reservation.setDeliveryStatus(request.getDeliveryStatus());
        reservation.setServiceResponsibleName(request.getServiceResponsibleName());
        reservation.setServiceResponsibleEmail(request.getServiceResponsibleEmail());
        reservation.setServiceResponsibleId(request.getServiceResponsibleId());
        reservation.setPayerName(request.getPayerName());
        reservation.setPayerEmail(request.getPayerEmail());
        reservation.setPayerId(request.getPayerId());
        reservation.setProviderRating(request.getProviderRating());
        reservation.setComments(request.getComments());
        reservation.setServiceData(request.getServiceData());
    }

    /**
     * Genera la imagen QR con los datos clave de la reserva
     */
    private String generateQrImage(Reservation reservation) {
        try {
            // Crear contenido del QR con datos clave de la reserva
            StringBuilder qrContent = new StringBuilder();
            qrContent.append("RESERVATION_ID:").append(reservation.getId()).append("\n");
            qrContent.append("PAYMENT_ID:").append(reservation.getPayment() != null ? reservation.getPayment().getId() : "N/A").append("\n");
            qrContent.append("PAYER_NAME:").append(reservation.getPayerName()).append("\n");
            qrContent.append("PAYER_EMAIL:").append(reservation.getPayerEmail()).append("\n");
            qrContent.append("RESERVATION_DATE:").append(reservation.getReservationDate()).append("\n");
            qrContent.append("DELIVERY_STATUS:").append(reservation.getDeliveryStatus()).append("\n");
            qrContent.append("SERVICE_RESPONSIBLE:").append(reservation.getServiceResponsibleName()).append("\n");
            qrContent.append("SERVICE_EMAIL:").append(reservation.getServiceResponsibleEmail()).append("\n");
            
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
