package com.tourya.api.models.request;

import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar el estado de un item del carrito.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateItemStatusRequest {

    @NotNull(message = "El estado es requerido")
    private ShoppingCartStatusEnum status;
}


