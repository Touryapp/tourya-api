package com.tourya.api.repository;

import com.tourya.api.models.ProviderPayoutAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProviderPayoutAttachmentRepository extends JpaRepository<ProviderPayoutAttachment, Long> {
    List<ProviderPayoutAttachment> findByPayoutOrderId(Long payoutOrderId);

    List<ProviderPayoutAttachment> findByPayoutOrderIdIn(Collection<Long> payoutOrderIds);
}

