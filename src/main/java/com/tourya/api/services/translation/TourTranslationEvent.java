package com.tourya.api.services.translation;

/**
 * IA-09: evento de dominio publicado por {@link com.tourya.api.services.TourService}
 * despues de un {@code saveCreateOrUpdateFullData}. Dispara la traduccion
 * automatica es -> en / pt-BR de los campos JSONB del tour.
 *
 * <p>Consumido por {@link TourTranslationEventListener} con
 * {@code @Async @TransactionalEventListener(AFTER_COMMIT)} — el save del tour
 * responde 200 al operador SIN esperar al call de Google Translate; las
 * traducciones se rellenan en background y se persisten cuando llegan. Mismo
 * patron que {@link com.tourya.api.services.credit.events.CreditRefundEvent} y
 * los emails de EmailService.</p>
 *
 * <p>El evento no lleva snapshot de los campos — el listener re-consulta desde
 * el {@link com.tourya.api.repository.TourRepository} para trabajar con el
 * estado post-commit y aplicar el guardrail (no sobrescribir en/pt que el
 * provider ya rellenó explícitamente).</p>
 *
 * @param tourId ID del tour recien guardado / actualizado.
 */
public record TourTranslationEvent(Integer tourId) {
}
