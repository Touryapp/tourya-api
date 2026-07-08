package com.tourya.api.controller;

import com.tourya.api.services.WompiWebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recibe eventos webhook enviados por Wompi (server-to-server).
 * Endpoint publico (sin auth JWT). La autenticacion se hace verificando el
 * checksum SHA-256 firmado con el events secret de Wompi.
 * Siempre responde 200 OK si el JSON es valido (independiente de la firma)
 * para evitar reintentos inutiles de Wompi. La validez de la firma se
 * guarda en la BD para forensia y reconciliacion.
 */
@Slf4j
@RestController
@RequestMapping("/public/wompi")
@RequiredArgsConstructor
@Tag(name = "Wompi Webhook", description = "Recibe eventos server-to-server de Wompi")
public class WompiWebhookController {

    private final WompiWebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(@RequestBody String rawPayload) {
        log.info("Wompi webhook received. payload_length={}",
                rawPayload != null ? rawPayload.length() : 0);
        webhookService.processIncoming(rawPayload);
        return ResponseEntity.ok().build();
    }
}
