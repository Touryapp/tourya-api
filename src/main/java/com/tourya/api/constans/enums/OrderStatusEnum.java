package com.tourya.api.constans.enums;

/**
 * Enum que define los estados de una orden.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
public enum OrderStatusEnum {
    /**
     * Orden pendiente de pago
     */
    PENDING,
    
    /**
     * Orden pagada
     */
    PAID,
    
    /**
     * Orden fallida
     */
    FAILED,
    
    /**
     * Orden reembolsada
     */
    REFUND
}


