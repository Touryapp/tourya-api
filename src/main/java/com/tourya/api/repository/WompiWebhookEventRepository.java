package com.tourya.api.repository;

import com.tourya.api.models.WompiWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WompiWebhookEventRepository extends JpaRepository<WompiWebhookEvent, Long> {

    List<WompiWebhookEvent> findByProcessedAtIsNull();

    List<WompiWebhookEvent> findByTransactionId(String transactionId);
}
