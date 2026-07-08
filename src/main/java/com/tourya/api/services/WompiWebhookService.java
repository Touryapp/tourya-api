package com.tourya.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.WompiWebhookEvent;
import com.tourya.api.repository.WompiWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Procesa eventos webhook recibidos desde Wompi.
 * Responsabilidades:
 *   1) Verificar la firma HMAC (checksum SHA-256 con events secret).
 *   2) Persistir el evento en wompi_webhook_event para reconciliacion posterior.
 * NO confirma reservas automaticamente: eso lo hace el job de reconciliacion (BE-17)
 * despues de matchear el transaction_id contra Payments existentes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WompiWebhookService {

    private final WompiWebhookEventRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${payment.wompi.events.secret}")
    private String eventsSecret;

    @Transactional
    public ProcessResult processIncoming(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            log.warn("Wompi webhook: empty payload");
            return ProcessResult.invalid("empty_payload");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            log.warn("Wompi webhook: invalid JSON payload", e);
            return ProcessResult.invalid("invalid_json");
        }

        JsonNode signatureNode = root.path("signature");
        JsonNode propsNode = signatureNode.path("properties");
        String checksum = signatureNode.path("checksum").asText(null);
        long timestamp = root.path("timestamp").asLong(0);

        if (checksum == null || !propsNode.isArray() || propsNode.isEmpty()) {
            log.warn("Wompi webhook missing signature.checksum or properties[]");
            persistEvent(root, rawPayload, false, checksum);
            return ProcessResult.invalid("missing_signature");
        }

        // Wompi arma el string a firmar concatenando los valores de las properties (en orden)
        // + timestamp + events_secret. Las properties son dot-paths bajo "data" del payload.
        // Algunas versiones envian "transaction.id", otras "data.transaction.id" -> normalizamos.
        StringBuilder toSign = new StringBuilder();
        JsonNode dataNode = root.path("data");
        for (JsonNode prop : propsNode) {
            String path = prop.asText();
            if (path.startsWith("data.")) {
                path = path.substring("data.".length());
            }
            String value = readByDotPath(dataNode, path);
            toSign.append(value != null ? value : "");
        }
        toSign.append(timestamp);
        toSign.append(eventsSecret);

        String computed = sha256Hex(toSign.toString());
        boolean valid = computed.equalsIgnoreCase(checksum);

        WompiWebhookEvent saved = persistEvent(root, rawPayload, valid, checksum);

        if (!valid) {
            log.warn("Wompi webhook SIGNATURE INVALID. tx_id={}, computed_len={}, received_len={}",
                    root.path("data").path("transaction").path("id").asText("unknown"),
                    computed.length(), checksum.length());
            return ProcessResult.invalid("checksum_mismatch");
        }

        log.info("Wompi webhook valid. event_id={}, tx_id={}, status={}",
                saved.getId(),
                saved.getTransactionId(),
                saved.getTransactionStatus());
        return ProcessResult.valid(saved.getId());
    }

    private WompiWebhookEvent persistEvent(JsonNode root, String rawPayload, boolean signatureValid, String checksum) {
        JsonNode tx = root.path("data").path("transaction");
        OffsetDateTime sentAt = null;
        String sentAtStr = root.path("sent_at").asText(null);
        if (sentAtStr != null) {
            try {
                sentAt = OffsetDateTime.parse(sentAtStr);
            } catch (Exception e) {
                log.debug("Wompi webhook: could not parse sent_at={}", sentAtStr);
            }
        }

        WompiWebhookEvent event = WompiWebhookEvent.builder()
                .wompiEventType(root.path("event").asText("unknown"))
                .transactionId(tx.path("id").asText("unknown"))
                .transactionReference(tx.path("reference").asText(null))
                .transactionStatus(tx.path("status").asText("UNKNOWN"))
                .amountInCents(tx.has("amount_in_cents") ? tx.get("amount_in_cents").asLong() : null)
                .currency(tx.path("currency").asText(null))
                .wompiSentAt(sentAt)
                .wompiTimestamp(root.has("timestamp") ? root.get("timestamp").asLong() : null)
                .rawPayload(rawPayload)
                .signatureValid(signatureValid)
                .signatureChecksum(checksum)
                .receivedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        return repository.save(event);
    }

    /**
     * Lee un valor por dot-path. "transaction.id" -> root.get("transaction").get("id").
     * Devuelve null si algun segmento no existe.
     */
    private String readByDotPath(JsonNode root, String dotPath) {
        JsonNode current = root;
        for (String segment : dotPath.split("\\.")) {
            current = current.path(segment);
            if (current.isMissingNode() || current.isNull()) {
                return null;
            }
        }
        if (current.isNumber()) return current.asText();
        if (current.isTextual()) return current.asText();
        if (current.isBoolean()) return String.valueOf(current.asBoolean());
        return current.toString();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record ProcessResult(boolean signatureValid, Long eventId, String errorCode) {
        public static ProcessResult valid(Long eventId) {
            return new ProcessResult(true, eventId, null);
        }

        public static ProcessResult invalid(String code) {
            return new ProcessResult(false, null, code);
        }
    }
}
