package com.tourya.api.models.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request para reservar créditos para un item del carrito.
 * Se distribuye el monto entre los créditos indicados (mayor a menor) y se asocia al item.
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ReserveCreditRequest {

    @NotNull(message = "El ID del item del carrito es obligatorio")
    @Positive(message = "El ID del item del carrito debe ser positivo")
    private Long shoppingCartItemId;

    @NotNull(message = "El monto a reservar es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a reservar debe ser mayor a 0")
    @Digits(integer = 8, fraction = 2, message = "Monto con formato inválido")
    private BigDecimal amountToReserve;

    @NotEmpty(message = "Debe incluir al menos un ID de crédito")
    private List<@Positive(message = "Cada ID de crédito debe ser positivo") Long> creditIds;
}
