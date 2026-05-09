package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para agregar items al carrito de compras.
 * Puede contener uno o múltiples items.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddMultipleItemsToCartRequest {

    private Long cartId; // ID del carrito existente (opcional)

    @NotNull(message = "La lista de items es requerida")
    @NotEmpty(message = "Debe agregar al menos un item")
    @Valid
    private List<AddItemToCartRequest> items;
}