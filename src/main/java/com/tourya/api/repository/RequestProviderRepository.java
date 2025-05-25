package com.tourya.api.repository;


import com.tourya.api.constans.enums.RequestProviderStatusEnum;
import com.tourya.api.models.Provider;
import com.tourya.api.models.RequestProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestProviderRepository extends JpaRepository<RequestProvider, Integer> {
    RequestProvider findByProvider(Provider provider);

    @Query("""
            SELECT requestProvider
            FROM RequestProvider requestProvider
            WHERE ((:status IS NULL ) OR (requestProvider.status = :status))
            """)
    Page<RequestProvider> findAllRequestProviderPending(@Param("status") RequestProviderStatusEnum status, Pageable pageable);
}
