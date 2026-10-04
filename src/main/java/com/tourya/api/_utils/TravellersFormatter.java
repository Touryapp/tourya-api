package com.tourya.api._utils;

import com.tourya.api.constans.enums.AgePriceType;
import com.tourya.api.models.ShoppingCartItemDetail;

import java.util.ArrayList;
import java.util.List;

/**
 * Issue #33 TCM-020 refinamiento (Luis 2026-10-03): formatea el string de
 * "travellers" que viaja en el correo de confirmación de compra y en los
 * endpoints de detalle de reserva. Antes se armaba crudo con el nombre del
 * enum ({@code "2 ADULTs, 1 CHILD"}) — Luis pidió que apareciera en español
 * ({@code "2 Adultos, 1 Niño"}).
 *
 * <p>Centraliza la lógica para no duplicarla en PaymentService,
 * ReservationService y ReviewService (los 3 lugares donde antes se armaba
 * inline con {@code String.join}).</p>
 */
public final class TravellersFormatter {

    private TravellersFormatter() { }

    /**
     * Construye el string "N TipoES[s], M TipoES[s]" a partir de los details
     * del cart item. Devuelve null si la lista está vacía (mismo comportamiento
     * que el inline original — el caller decide si setea el campo o no).
     */
    public static String format(List<ShoppingCartItemDetail> details) {
        if (details == null || details.isEmpty()) return null;

        List<String> parts = new ArrayList<>();
        for (ShoppingCartItemDetail detail : details) {
            Integer qty = detail.getQuantity();
            if (qty == null || qty <= 0) continue;
            String label = singularLabel(detail.getAgeType());
            parts.add(qty + " " + (qty > 1 ? pluralize(label) : label));
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static String singularLabel(AgePriceType ageType) {
        if (ageType == null) return "Adulto";
        return switch (ageType) {
            case ADULT -> "Adulto";
            case CHILD -> "Niño";
            case INFANT -> "Infante";
        };
    }

    /** Pluralización simple en español. */
    private static String pluralize(String singular) {
        return switch (singular) {
            case "Adulto" -> "Adultos";
            case "Niño" -> "Niños";
            case "Infante" -> "Infantes";
            default -> singular + "s";
        };
    }
}
