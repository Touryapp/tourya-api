package com.tourya.api.models.mapper;

import com.tourya.api.models.Reservation;
import com.tourya.api.models.request.CreateReservationRequest;
import com.tourya.api.models.responses.ReservationResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre entidades Reservation y DTOs.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Component
public class ReservationMapper {

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

        return ReservationResponse.builder()
                .reservationId(reservation.getReservationId())
                .paymentId(reservation.getPaymentId())
                .qrUrl(reservation.getQrUrl()) // URL del QR en S3
                .reservationDate(reservation.getReservationDate())
                .deliveryStatus(reservation.getDeliveryStatus())
                .createdDate(reservation.getCreatedDate())
                .items(null) // Se llenará desde el servicio si es necesario
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

}