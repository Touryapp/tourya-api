package com.tourya.api.constans.enums;

/**
 * Enum que define los estados de una cuenta por pagar.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
public enum AccountPayableStatusEnum {
    /**
     * Cuenta por pagar pendiente
     */
    PENDING,
    
    /**
     * Cuenta por pagar procesada/pagada
     */
    PAID,
    
    /**
     * Cuenta por pagar cancelada
     */
    CANCELLED
}

