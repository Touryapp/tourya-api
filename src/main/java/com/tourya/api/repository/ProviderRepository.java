package com.tourya.api.repository;

import com.tourya.api.constans.enums.ProviderStatusEnum;
import com.tourya.api.models.Provider;
import com.tourya.api.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderRepository extends JpaRepository<Provider, Integer> {
     Provider findByUser(User user);

     @Query("""
            SELECT provider
            FROM Provider provider
            WHERE ( ((:status) IS NULL ) OR ( provider.status = :status ) )
            """)
     Page<Provider> findAllProvider(@Param("status") ProviderStatusEnum status, Pageable pageable);
}
