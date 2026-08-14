package com.tourya.api.agents.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * IA-01: precios oficiales de Anthropic por 1M tokens (input / output).
 *
 * <p>Actualizados 2026-07-15. Hardcoded en vez de en app_config porque
 * Anthropic cambia precios ~1x al año — mover a BD suma queries y no aporta.
 * Cuando cambien, se ajusta esta clase y se despliega.</p>
 *
 * <p>Ver <a href="../../../docs/16-agentes-ia.md">doc 16 stack de modelos</a>.</p>
 */
public final class ModelPricing {

    // Precio introductorio hasta 2026-08-31 ($2/$10 por 1M).
    // Estándar tras esa fecha: $3 / $15 por 1M.
    public static final String CLAUDE_SONNET_5 = "claude-sonnet-5";
    public static final String CLAUDE_HAIKU_4_5 = "claude-haiku-4-5-20251001";
    public static final String CLAUDE_OPUS_4_8 = "claude-opus-4-8";

    // IA-02 (2026-08-14): Vertex AI Gemini pricing por 1M tokens (context <=200k).
    // gemini-2.5-flash cubre lo que antes hacia Haiku (clasificacion), y
    // gemini-2.5-pro cubre lo que antes hacia Sonnet (razonamiento + tool use).
    public static final String GEMINI_2_5_FLASH = "gemini-2.5-flash";
    public static final String GEMINI_2_5_PRO = "gemini-2.5-pro";

    private static final Map<String, Prices> PRICING = Map.of(
            CLAUDE_SONNET_5, new Prices(new BigDecimal("2.00"), new BigDecimal("10.00")),
            CLAUDE_HAIKU_4_5, new Prices(new BigDecimal("1.00"), new BigDecimal("5.00")),
            CLAUDE_OPUS_4_8, new Prices(new BigDecimal("15.00"), new BigDecimal("75.00")),
            GEMINI_2_5_FLASH, new Prices(new BigDecimal("0.075"), new BigDecimal("0.30")),
            GEMINI_2_5_PRO, new Prices(new BigDecimal("1.25"), new BigDecimal("10.00"))
    );

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    /** Fallback conservador cuando el modelo no está en la tabla — no queremos undercount. */
    private static final Prices UNKNOWN_MODEL_FALLBACK = new Prices(new BigDecimal("5.00"), new BigDecimal("25.00"));

    private ModelPricing() {}

    /**
     * Calcula el costo total del call en USD, con 6 decimales de precisión
     * (suficiente para tokens sueltos de Haiku, que valen ~0.000005 USD c/u).
     */
    public static BigDecimal computeCostUsd(String model, int inputTokens, int outputTokens) {
        Prices p = PRICING.getOrDefault(model, UNKNOWN_MODEL_FALLBACK);
        BigDecimal in = p.inputPer1M
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
        BigDecimal out = p.outputPer1M
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
        return in.add(out);
    }

    private record Prices(BigDecimal inputPer1M, BigDecimal outputPer1M) {}
}
