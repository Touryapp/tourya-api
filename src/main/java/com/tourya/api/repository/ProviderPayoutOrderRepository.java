package com.tourya.api.repository;

import com.tourya.api.models.ProviderPayoutOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderPayoutOrderRepository extends JpaRepository<ProviderPayoutOrder, Long> {
}

