package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para agregar un item al carrito de compras.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddItemToCartRequest {

    private Long cartId; // ID del carrito existente (opcional)

    @NotNull(message = "El ID del producto es requerido")
    private Integer productId;

    @NotNull(message = "El tipo de producto es requerido")
    private String productType; // TOUR, SERVICE, etc.

    // quantity y unitPrice se manejan en los details por ageType

    private Integer serviceId;

    private LocalDate scheduleDate; // Obligatorio si productType es TOUR

    private Integer tourScheduleId; // Obligatorio si productType es TOUR

    @Valid
    private SlotRequest slot; // Obligatorio si productType es TOUR
}

