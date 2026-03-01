package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response para el reagendamiento de una reserva.
 * Incluye el estado de la transacción, validación del precio y datos del carrito si aplica.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RescheduleResponse {
    
    /**
     * Estado de la transacción: SUCCESS, CANCELLED_AND_ADDED_TO_CART
     */
    private String transactionStatus;
    
    /**
     * Resultado de la validación del precio: LOWER, EQUAL, HIGHER
     */
    private String priceComparison;
    
    /**
     * Mensaje descriptivo del resultado
     */
    private String message;
    
    /**
     * Datos de la reserva actualizada (si el precio es igual o menor)
     */
    private ReservationResponse reservation;
    
    /**
     * Datos del carrito con el nuevo item (si el precio es mayor)
     */
    private ShoppingCartResponse shoppingCart;
    
    /**
     * Crédito creado (si aplica)
     */
    private CreditResponse credit;
}
