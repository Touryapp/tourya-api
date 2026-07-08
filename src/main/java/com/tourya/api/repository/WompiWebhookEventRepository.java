package com.tourya.api.repository;

import com.tourya.api.models.WompiWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WompiWebhookEventRepository extends JpaRepository<WompiWebhookEvent, Long> {

    List<WompiWebhookEvent> findByProcessedAtIsNull();

    List<WompiWebhookEvent> findByTransactionId(String transactionId);

    /**
     * Eventos pendientes de reconciliacion: firma valida, no procesados aun,
     * y con un status especifico (tipicamente APPROVED). Limitado y ordenado
     * por received_at ASC para procesar los mas viejos primero.
     */
    List<WompiWebhookEvent> findTop100BySignatureValidTrueAndProcessedAtIsNullAndTransactionStatusOrderByReceivedAtAsc(
            String transactionStatus);
}
