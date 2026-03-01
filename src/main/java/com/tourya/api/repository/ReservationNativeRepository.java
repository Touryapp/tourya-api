package com.tourya.api.repository;

import com.tourya.api.models.responses.ReservationDetailsResponse;

import java.util.List;

public interface ReservationNativeRepository {

    List<ReservationDetailsResponse> getProviderReservations(
            Integer providerId,
            Long reservationId,
            String deliveryStatus,
            int page,
            int size
    );

    long countProviderReservations(
            Integer providerId,
            Long reservationId,
            String deliveryStatus
    );

    void deleteShoppingCartItemDirectly(Long itemId);
    
    void updateReservationItemIdToNull(Long reservationId);
}
