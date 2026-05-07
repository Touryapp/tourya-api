package com.tourya.api.repository;

import com.tourya.api.models.ProviderPayoutOrderReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProviderPayoutOrderReservationRepository extends JpaRepository<ProviderPayoutOrderReservation, ProviderPayoutOrderReservation.Pk> {
    boolean existsByReservationId(Long reservationId);

    List<ProviderPayoutOrderReservation> findByPayoutOrderId(Long payoutOrderId);
}

