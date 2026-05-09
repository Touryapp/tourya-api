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
                .payoutStatus(Reservation.PAYOUT_STATUS_PENDING)
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

        // Construir respuesta del responsable del servicio
        com.tourya.api.models.responses.ServiceResponsibleResponse serviceResponsible = null;
        if (reservation.getServiceResponsibleName() != null || 
            reservation.getServiceResponsibleEmail() != null ||
            reservation.getServiceResponsiblePhone() != null) {
            serviceResponsible = com.tourya.api.models.responses.ServiceResponsibleResponse.builder()
                    .name(reservation.getServiceResponsibleName())
                    .email(reservation.getServiceResponsibleEmail())
                    .phone(reservation.getServiceResponsiblePhone() != null ? 
                          Long.parseLong(reservation.getServiceResponsiblePhone().replaceAll("[^0-9]", "")) : null)
                    .build();
        }

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
                // Campos de cancelación y re-agendamiento
                .maxCancellationDate(reservation.getMaxCancellationDate())
                .maxReschedulingDate(reservation.getMaxReschedulingDate())
                .cancellationReason(reservation.getCancellationReason())
                .cancellationDate(reservation.getCancellationDate())
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