package com.tourya.api.repository;

import com.tourya.api.models.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationItemRepository extends JpaRepository<ReservationItem, Long> {
    List<ReservationItem> findByReservationId(Long reservationId);
    List<ReservationItem> findByShoppingCartItemId(Long shoppingCartItemId);
}
