package com.tourya.api.services.translation;

import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * IA-09: implementacion productiva del servicio de traduccion usando
 * <b>Google Cloud Translation v3</b> (basic — sin AutoML / glossaries).
 *
 * <p><b>Decision Franklin 2026-08-15</b>: reusa infra GCP (misma SA
 * {@code tourya-dev-cloud-run} con {@code roles/cloudtranslate.user}), misma
 * facturacion que Vertex AI, cero proveedores nuevos. API
 * {@code translate.googleapis.com} habilitada en {@code tourya-project-dev}.</p>
 *
 * <p><b>Auth:</b> Application Default Credentials (ADC). En Cloud Run usa la SA
 * automaticamente; en local dev requiere
 * {@code gcloud auth application-default login} (Franklin ya lo hizo).</p>
 *
 * <p><b>Modo no-op degradado:</b> si {@code agents.translation.enabled=false} o
 * el SDK falla al inicializar (credenciales ausentes, endpoint no disponible)
 * el service devuelve {@code null} en todos los metodos y
 * {@link #isEnabled()} = {@code false}. Nunca throws — mismo contrato que
 * {@link com.tourya.api.agents.shared.GeminiClient}.</p>
 *
 * <p><b>Mapeo de codigos:</b> el JSONB de Tourya usa {@code "pt"} como clave;
 * Google Cloud Translation acepta {@code "pt"} (portugues generico) o
 * {@code "pt-BR"} (Brasil especifico). Luis pidio portugues de Brasil, asi que
 * internamente mapeamos {@code "pt"} -> {@code "pt-BR"} para el call a Google
 * y guardamos la traduccion bajo la clave {@code "pt"}. El {@code "en"} queda
 * como {@code "en"} (Google usa el neutro por defecto).</p>
 *
 * <p><b>v3 basic:</b> soporta hasta 1024 textos por request. Usamos un texto por
 * call para tener error handling granular — si un idioma falla, el otro sigue.
 * Volumen esperado: bajo (10-80 tours/mes), la latencia agregada es
 * despreciable.</p>
 */
@Slf4j
@Service
public class GoogleCloudTranslationService implements ITranslationService {

    private final boolean enabled;
    private final String projectId;
    private final String sourceLang;

    /** Lazy — evita costo de inicializacion en tests y cuando el flag esta OFF. */
    private volatile TranslationServiceClient client;
    /** Cache del flag "el SDK inicializo OK". Null = no intentado, TRUE = OK, FALSE = fallo. */
    private volatile Boolean sdkReady;

    public GoogleCloudTranslationService(
            @Value("${agents.translation.enabled:true}") boolean enabled,
            @Value("${agents.translation.project-id:tourya-project-dev}") String projectId,
            @Value("${agents.translation.source-lang:es}") String sourceLang) {
        this.enabled = enabled;
        this.projectId = projectId;
        this.sourceLang = sourceLang == null || sourceLang.isBlank() ? "es" : sourceLang;
    }

    @Override
    public String translate(String text, String targetLang) {
        if (!enabled) {
            log.debug("IA-09 Google Translate disabled by config");
            return null;
        }
        if (text == null || text.isBlank()) {
            return null;
        }
        if (targetLang == null || targetLang.isBlank()) {
            return null;
        }

        TranslationServiceClient c = getOrInitClient();
        if (c == null) {
            return null;
        }

        try {
            String googleTargetLang = toGoogleLangCode(targetLang);
            LocationName parent = LocationName.of(projectId, "global");
            TranslateTextRequest request = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setSourceLanguageCode(sourceLang)
                    .setTargetLanguageCode(googleTargetLang)
                    .addContents(text)
                    .build();
            TranslateTextResponse response = c.translateText(request);
            List<Translation> translations = response.getTranslationsList();
            if (translations == null || translations.isEmpty()) {
                log.warn("IA-09 Google Translate returned no translations targetLang={}", targetLang);
                return null;
            }
            String translated = translations.get(0).getTranslatedText();
            return translated == null || translated.isEmpty() ? null : translated;
        } catch (Exception ex) {
            log.error("IA-09 Google Translate call failed targetLang={} — falling back to null",
                    targetLang, ex);
            return null;
        }
    }

    @Override
    public Map<String, String> translateBatch(String text, List<String> targetLangs) {
        if (targetLangs == null || targetLangs.isEmpty()) {
            return Collections.emptyMap();
        }
        if (!enabled || text == null || text.isBlank()) {
            return Collections.emptyMap();
        }
        // LinkedHashMap para preservar el orden y facilitar los tests.
        Map<String, String> out = new LinkedHashMap<>();
        for (String lang : targetLangs) {
            if (lang == null || lang.isBlank()) continue;
            String result = translate(text, lang);
            if (result != null) {
                out.put(lang, result);
            }
            // Los langs que fallaron no aparecen — el caller trata la ausencia
            // como "conservar el ES" (TranslatedField auto-fallback).
        }
        return out;
    }

    @Override
    public boolean isEnabled() {
        if (!enabled) return false;
        Boolean ready = this.sdkReady;
        if (ready != null) return ready;
        // Inicializacion lazy — un check tempranero sin bloquear si el SDK falla.
        return getOrInitClient() != null;
    }

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Mapea el codigo de idioma que usa el JSONB de Tourya al codigo que espera
     * Google Cloud Translation. La unica traduccion no-trivial es {@code "pt"}
     * -> {@code "pt-BR"} (portugues de Brasil, decision con Luis en la propuesta
     * {@code traduccion-automatica-tours.md}).
     */
    static String toGoogleLangCode(String jsonbLang) {
        if (jsonbLang == null) return null;
        String lower = jsonbLang.trim().toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "pt" -> "pt-BR";
            default -> lower;
        };
    }

    /**
     * Inicializa el {@link TranslationServiceClient} la primera vez que se usa
     * (thread-safe via double-checked locking). Si falla — tipicamente ADC no
     * configurado o API no habilitada — loguea y devuelve {@code null} para
     * caer a modo disabled sin romper el arranque.
     */
    private TranslationServiceClient getOrInitClient() {
        TranslationServiceClient local = this.client;
        if (local == null) {
            synchronized (this) {
                local = this.client;
                if (local == null) {
                    try {
                        local = TranslationServiceClient.create();
                        this.client = local;
                        this.sdkReady = Boolean.TRUE;
                        log.info("IA-09 Google Translate client initialized project={}", projectId);
                    } catch (Exception ex) {
                        this.sdkReady = Boolean.FALSE;
                        log.error("IA-09 Google Translate init failed — falling back to disabled mode", ex);
                        return null;
                    }
                }
            }
        }
        return local;
    }

    @PreDestroy
    void shutdown() {
        TranslationServiceClient local = this.client;
        if (local != null) {
            try {
                local.close();
            } catch (Exception ex) {
                log.warn("IA-09 Google Translate client close failed", ex);
            }
        }
    }
}
