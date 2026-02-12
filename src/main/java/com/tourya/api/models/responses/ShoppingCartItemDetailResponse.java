package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.AgePriceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta para los detalles de un item del carrito de compras.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingCartItemDetailResponse {

    private Long id;
    private AgePriceType ageType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal providerUnitPrice; // Precio unitario que recibe el proveedor
    private BigDecimal totalPrice;
}


