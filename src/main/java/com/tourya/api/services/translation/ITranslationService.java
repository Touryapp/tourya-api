package com.tourya.api.services.translation;

import java.util.List;
import java.util.Map;

/**
 * IA-09: abstraccion del servicio de traduccion es -> en / pt-BR.
 *
 * <p>El MVP usa {@link GoogleCloudTranslationService} (Google Cloud Translation v3).
 * La interface se mantiene para: (a) permitir mock en tests sin llamar al API real,
 * (b) permitir cambiar de proveedor (DeepL, LLM Gemini, etc.) sin reescribir los
 * callers, (c) permitir modo no-op degradado (isEnabled=false) sin acoplar los
 * callers al codigo del SDK.</p>
 *
 * <p>Contrato: <b>nunca throws</b>. Mismo enfoque que
 * {@link com.tourya.api.agents.shared.ILlmClient}: si el service esta desactivado
 * o el call falla, devuelve {@code null} y loguea. El caller decide (tipicamente
 * fallback al ES original — el {@link com.tourya.api.models.TranslatedField} ya
 * hace auto-fallback a ES cuando en/pt son null/vacios).</p>
 *
 * <p><b>Codigos de idioma:</b> se usan los mismos codigos que el JSONB del proyecto
 * ({@code "es"}, {@code "en"}, {@code "pt"}). El mapeo interno a codigos de Google
 * ({@code "pt"} -> {@code "pt-BR"}) es una decision del provider — los callers no
 * lo ven.</p>
 */
public interface ITranslationService {

    /**
     * Traduce un texto plano de {@code es} -> {@code targetLang}.
     *
     * @param text       Texto en espanol a traducir. Si es {@code null} o blank, devuelve {@code null}.
     * @param targetLang Codigo del idioma destino ({@code "en"}, {@code "pt"}).
     * @return Texto traducido, o {@code null} si el service esta desactivado o el call fallo.
     */
    String translate(String text, String targetLang);

    /**
     * Traduce el texto a multiples idiomas en una sola pasada. Optimiza calls
     * agrupando cuando el proveedor lo soporta (Google Cloud Translation permite
     * un target lang por request en v3 basic — este metodo hace N calls
     * secuenciales pero centraliza el error handling).
     *
     * @param text        Texto en espanol a traducir.
     * @param targetLangs Idiomas destino (codigos JSONB: {@code "en"}, {@code "pt"}).
     * @return Map lang -> traduccion. Los langs que fallaron NO aparecen en el map
     *         (el caller usa {@link Map#get} y trata {@code null} como "conservar ES").
     */
    Map<String, String> translateBatch(String text, List<String> targetLangs);

    /**
     * @return {@code true} cuando el service esta configurado y el SDK inicializo
     *         OK. {@code false} en modo no-op degradado (flag off, ADC ausente,
     *         API deshabilitada). Los callers pueden usar esto para skip-early
     *         antes de armar un batch.
     */
    boolean isEnabled();
}
