package com.tourya.api.constans.enums;

/**
 * Enum que define los estados de un crédito.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
public enum CreditStatusEnum {
    /**
     * Crédito disponible para usar o reservar
     */
    CREATED,

    /**
     * Crédito reservado para un item del carrito (tiene reserved_amount y shopping_cart_item_id)
     */
    RESERVED,

    /**
     * Crédito consumido (amount = 0)
     */
    CONSUMED,

    /**
     * Crédito cancelado
     */
    CANCELED,

    /**
     * Crédito eliminado
     */
    DELETED
}


