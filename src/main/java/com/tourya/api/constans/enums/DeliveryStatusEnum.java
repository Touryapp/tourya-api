package com.tourya.api.constans.enums;

/**
 * Enum que define los estados de entrega de un producto.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
public enum DeliveryStatusEnum {
    /**
     * Entrega pendiente
     */
    PENDING,
    
    /**
     * Entrega reservada
     */
    RESERVED,
    
    /**
     * Entrega en tránsito
     */
    IN_TRANSIT,
    
    /**
     * Entrega completada
     */
    DELIVERED,
    
    /**
     * Entrega cancelada
     */
    CANCELED
}


