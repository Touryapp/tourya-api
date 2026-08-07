package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TC-016 (#219): desglose de viajeros por ageType dentro de un shopping_cart_item.
 * Cada item de reserva puede tener varios ageTypes (ADULT/CHILD/INFANT) con distinta cantidad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelerBreakdownDto {
    /** Valor del enum AgePriceType almacenado en shopping_cart_item_detail.age_type (ADULT/CHILD/INFANT). */
    private String ageType;
    private Integer quantity;
    private Double unitPrice;
    private Double providerUnitPrice;
}
