package com.tourya.api.services.translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IA-09: mock deterministico de {@link ITranslationService} para tests
 * unitarios. Mismo patron que {@code MockLlmClient} en el paquete IA:
 *
 * <ul>
 *   <li>Se puede desactivar via {@link #disable()} para simular
 *       {@code agents.translation.enabled=false}.</li>
 *   <li>Se puede forzar fallo via {@link #failNext()} para simular
 *       {@code translation_api_error}.</li>
 *   <li>Devuelve traducciones sinteticas del tipo {@code [lang] text} para que
 *       los asserts sepan que idioma se pidio.</li>
 * </ul>
 */
public class MockTranslationClient implements ITranslationService {

    private boolean enabled = true;
    private boolean failOnce = false;
    private final List<String> capturedTexts = new ArrayList<>();
    private final List<String> capturedLangs = new ArrayList<>();

    public MockTranslationClient disable() {
        this.enabled = false;
        return this;
    }

    public MockTranslationClient failNext() {
        this.failOnce = true;
        return this;
    }

    public List<String> capturedTexts() {
        return capturedTexts;
    }

    public List<String> capturedLangs() {
        return capturedLangs;
    }

    @Override
    public String translate(String text, String targetLang) {
        if (!enabled) return null;
        if (text == null || text.isBlank()) return null;
        if (targetLang == null || targetLang.isBlank()) return null;
        if (failOnce) {
            failOnce = false;
            return null;
        }
        capturedTexts.add(text);
        capturedLangs.add(targetLang);
        return "[" + targetLang + "] " + text;
    }

    @Override
    public Map<String, String> translateBatch(String text, List<String> targetLangs) {
        if (!enabled || text == null || text.isBlank() || targetLangs == null || targetLangs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (String lang : targetLangs) {
            String r = translate(text, lang);
            if (r != null) out.put(lang, r);
        }
        return out;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
