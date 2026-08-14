package com.tourya.api.agents.shared;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * IA-02: selector runtime del {@link ILlmClient} inyectado a los agentes.
 *
 * <p>Ambos clientes concretos ({@link AnthropicClient} y {@link GeminiClient})
 * quedan como {@code @Component} — Spring instancia los dos. Este {@code @Primary}
 * bean expone el elegido por {@code agents.provider} para el resto de la app.</p>
 *
 * <p>Cambiar de provider es una env var:</p>
 * <ul>
 *   <li>{@code AGENTS_PROVIDER=gemini}  → default 2026-08-14 (Vertex AI Gemini)</li>
 *   <li>{@code AGENTS_PROVIDER=anthropic} → fallback a Claude si Franklin quiere A/B</li>
 * </ul>
 *
 * <p>El agente jamás inyecta {@code AnthropicClient} o {@code GeminiClient}
 * concreto — siempre {@code ILlmClient}. Este bean garantiza que "el @Primary"
 * sea uno predecible.</p>
 */
@Slf4j
@Configuration
public class LlmClientConfig {

    /**
     * @param provider  {@code gemini} (default) o {@code anthropic}.
     * @param anthropic instancia inyectada de {@link AnthropicClient}.
     * @param gemini    instancia inyectada de {@link GeminiClient}.
     */
    @Bean
    @Primary
    public ILlmClient primaryLlmClient(
            @Value("${agents.provider:gemini}") String provider,
            AnthropicClient anthropic,
            GeminiClient gemini) {
        boolean useAnthropic = "anthropic".equalsIgnoreCase(provider);
        ILlmClient chosen = useAnthropic ? anthropic : gemini;
        log.info("IA-02 LLM provider seleccionado: {} (impl={})",
                useAnthropic ? "anthropic" : "gemini",
                chosen.getClass().getSimpleName());
        return chosen;
    }
}
