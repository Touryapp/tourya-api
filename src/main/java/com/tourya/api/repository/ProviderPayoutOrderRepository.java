package com.tourya.api.repository;

import com.tourya.api.constans.enums.ProviderPayoutOrderStatusEnum;
import com.tourya.api.models.ProviderPayoutOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ProviderPayoutOrderRepository extends JpaRepository<ProviderPayoutOrder, Long> {

    @Query("""
            SELECT o FROM ProviderPayoutOrder o
            WHERE (:providerId IS NULL OR o.providerId = :providerId)
              AND (:status IS NULL OR o.status = :status)
              AND (:fromDate IS NULL OR o.createdAt >= :fromDate)
              AND (:toDate IS NULL OR o.createdAt <= :toDate)
            ORDER BY o.createdAt DESC
            """)
    List<ProviderPayoutOrder> findFiltered(
            @Param("providerId") Integer providerId,
            @Param("status") ProviderPayoutOrderStatusEnum status,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate);
}

