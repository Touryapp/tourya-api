package com.tourya.api.models.mapper.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO para agregar items al carrito de compras
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
public class AddItemToCartRequest {

    private Integer productId;

    private Integer serviceTypeId;

    private Integer tourScheduleId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Min(value = 1, message = "Age must be at least 1")
    private Integer age;
}