package com.tourya.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@RequestMapping("/reference")
public class ReferenceController {

    // 🔑 Secreto de integridad leído desde application.properties
    @Value("${payment.wompi.integrity.secret}")
    private String integritySecret;

    /**
     * Genera (o recibe) una referencia de transacción y devuelve el hash SHA-256
     * basado en: referencia + montoEnCentavos + moneda + secretoIntegridad
     */
    @GetMapping("/generate")
    public Map<String, Object> generateHash(
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "2490000") String amountInCents,
            @RequestParam(defaultValue = "COP") String currency
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 🆔 Si no se pasa referencia, se genera automáticamente
        if (reference == null || reference.isBlank()) {
            reference = generateReference("TOURYA");
        }

        // 🔗 Construcción exacta del string a hashear
        String dataToHash = reference + amountInCents + currency + integritySecret;

        // 🧮 Generar hash SHA-256
        String hash = generateSHA256(dataToHash);

        // 📦 Respuesta sin concatenated_string
        result.put("reference", reference);
        result.put("amount_in_cents", amountInCents);
        result.put("currency", currency);
        result.put("sha256_hash", hash);

        return result;
    }

    // 🔒 Generar SHA-256
    private String generateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generando hash SHA-256", e);
        }
    }

    // 🆔 Generar referencia única
    private String generateReference(String prefix) {
        return prefix + "_" + UUID.randomUUID();
    }
}
