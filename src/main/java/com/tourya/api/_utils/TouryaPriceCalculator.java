package com.tourya.api._utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula precio de venta a partir del precio del proveedor y el porcentaje Tourya del slot.
 * Almacenamiento interno: fracción decimal (0.15 = 15%).
 * API de backoffice (request/response): puntos porcentuales (15 = 15%).
 * Fórmula: salePrice = providerPrice + (providerPrice × fracción)
 */
public final class TouryaPriceCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int PCT_SCALE = 4;

    private TouryaPriceCalculator() {
    }

    public static BigDecimal normalizePercentage(BigDecimal percentage) {
        if (percentage == null) {
            return BigDecimal.ZERO;
        }
        return percentage;
    }

    /**
     * Convierte valor enviado por API (15 = 15%) a fracción para persistencia (0.15).
     */
    public static BigDecimal fromApiPercentPoints(BigDecimal apiValue) {
        if (apiValue == null) {
            return BigDecimal.ZERO;
        }
        if (apiValue.compareTo(BigDecimal.ONE) > 0) {
            return apiValue.divide(BigDecimal.valueOf(100), PCT_SCALE, RoundingMode.HALF_UP);
        }
        return apiValue.setScale(PCT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Convierte fracción almacenada a puntos para la API (0.15 → 15).
     */
    public static BigDecimal toApiPercentPoints(BigDecimal storedFraction) {
        if (storedFraction == null) {
            return BigDecimal.ZERO;
        }
        if (storedFraction.compareTo(BigDecimal.ONE) <= 0) {
            return storedFraction.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }
        return storedFraction.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateSalePrice(BigDecimal providerPrice, BigDecimal slotPorcentajeFraction) {
        BigDecimal base = providerPrice != null ? providerPrice : BigDecimal.ZERO;
        BigDecimal pct = normalizePercentage(slotPorcentajeFraction);
        return base.add(base.multiply(pct)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
