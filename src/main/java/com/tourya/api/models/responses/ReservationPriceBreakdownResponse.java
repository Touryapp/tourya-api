package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.AgePriceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Desglose de precios por tipo de turista (adulto, niño, bebé) en reserva/compra.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationPriceBreakdownResponse {

    private AgePriceType ageType;
    private Integer quantity;
    /** Precio unitario de venta al turista. */
    private BigDecimal unitPrice;
    /** Precio unitario del proveedor. */
    private BigDecimal providerUnitPrice;
    /** Subtotal venta (unitPrice × quantity o total del detalle). */
    private BigDecimal subtotal;
    /** Subtotal proveedor. */
    private BigDecimal providerSubtotal;
}
