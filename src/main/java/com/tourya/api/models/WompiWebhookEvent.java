package com.tourya.api.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Registro de un evento webhook recibido desde Wompi.
 * No extiende BaseEntity porque received_at + processed_at cubren la trazabilidad temporal
 * y no hay auditoria de usuario (los webhooks son server-to-server, no hay usuario Tourya asociado).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "wompi_webhook_event")
public class WompiWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wompi_event_type", nullable = false, length = 50)
    private String wompiEventType;

    @Column(name = "transaction_id", nullable = false, length = 120)
    private String transactionId;

    @Column(name = "transaction_reference", length = 255)
    private String transactionReference;

    @Column(name = "transaction_status", nullable = false, length = 30)
    private String transactionStatus;

    @Column(name = "amount_in_cents")
    private Long amountInCents;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "wompi_sent_at")
    private OffsetDateTime wompiSentAt;

    @Column(name = "wompi_timestamp")
    private Long wompiTimestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid;

    @Column(name = "signature_checksum", length = 128)
    private String signatureChecksum;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "linked_payment_id")
    private Long linkedPaymentId;
}
