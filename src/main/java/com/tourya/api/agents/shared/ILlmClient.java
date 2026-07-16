package com.tourya.api.agents.shared;

/**
 * IA-01: abstracción del proveedor LLM. El MVP usa solo Anthropic
 * ({@link AnthropicClient}) — la interface se mantiene para permitir cambiar
 * de proveedor o hacer A/B testing entre proveedores sin reescribir agentes.
 *
 * <p>Los agentes reciben {@link ILlmClient} vía DI, nunca instancian el
 * cliente concreto. Los tests mockean esta interface para escenarios
 * deterministas (golden files) sin llamar al LLM real.</p>
 */
public interface ILlmClient {

    /**
     * Genera una completion para el prompt indicado con el modelo indicado.
     *
     * @param prompt Prompt renderizado ({@link PromptTemplate#render}).
     * @param model  ID del modelo (ver {@link ModelPricing}).
     * @return Respuesta normalizada con contenido + tokens + costo pre-calculado.
     *         Si el provider no está configurado, devuelve {@link AgentLlmResponse#disabled}.
     *         Si el call falla (HTTP error, timeout, rate limit), devuelve
     *         {@link AgentLlmResponse#error} — nunca throws.
     */
    AgentLlmResponse complete(String prompt, String model);
}
