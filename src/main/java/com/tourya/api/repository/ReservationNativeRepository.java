package com.tourya.api.repository;

import com.tourya.api.models.responses.ReservationDetailsResponse;

import java.util.List;

public interface ReservationNativeRepository {

    List<ReservationDetailsResponse> getProviderReservations(
            Integer providerId,
            Integer customerUserId,
            Long reservationId,
            String deliveryStatus,
            String subCategory,
            String sortBy,
            String sortDirection,
            int page,
            int size
    );

    long countProviderReservations(
            Integer providerId,
            Integer customerUserId,
            Long reservationId,
            String deliveryStatus,
            String subCategory
    );

    void deleteShoppingCartItemDirectly(Long itemId);
    
    void updateReservationItemIdToNull(Long reservationId);
}
