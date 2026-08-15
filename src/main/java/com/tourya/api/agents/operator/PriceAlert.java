package com.tourya.api.agents.operator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * IA-07: alerta de precio desalineado devuelta por
 * {@link OperatorSupportService#priceAlert}.
 *
 * <p>RN-014: <b>solo alerta, nunca modifica</b> {@code providerPrice}. El
 * operador decide si ajusta o mantiene su precio.</p>
 *
 * @param severity            Nivel de la alerta.
 * @param currentAvgPrice     Precio publico promedio actual del tour del
 *                            operador (ADULT). Se usa el {@code price} publico
 *                            —nunca {@code providerPrice} interno— para poder
 *                            comparar like-for-like con otros tours sin filtrar
 *                            margenes.
 * @param comparablePriceRange Rango observado en tours comparables (misma
 *                            subcategoria + zona).
 * @param comparablesCount    Cantidad de tours comparables usados en el analisis.
 * @param reasoning           Explicacion natural para el operador.
 * @param escalatedToHuman    {@code true} si no hay comparables suficientes o
 *                            el LLM fallo — el frontend debe mostrar mensaje neutro.
 */
@Builder
@Schema(description = "Alerta de pricing (RN-014: nunca modifica el precio, solo informa)")
public record PriceAlert(
        Severity severity,
        BigDecimal currentAvgPrice,
        PriceRange comparablePriceRange,
        int comparablesCount,
        String reasoning,
        boolean escalatedToHuman
) {

    public enum Severity {
        /** Precio dentro del rango esperado. */
        OK,
        /** Precio fuera del rango pero no en extremo — vale la pena revisar. */
        WARN,
        /** Precio muy por encima o debajo del mediano — el operador podria estar perdiendo mercado. */
        CRITICAL,
        /** No hay comparables suficientes para emitir juicio. */
        UNKNOWN
    }

    /**
     * @param min    Menor precio observado en comparables.
     * @param max    Mayor precio observado.
     * @param median Mediana — el "precio de mercado" implicito.
     */
    @Schema(description = "Rango de precios de tours comparables (mismo subcategoria + zona)")
    public record PriceRange(BigDecimal min, BigDecimal max, BigDecimal median) {}
}
