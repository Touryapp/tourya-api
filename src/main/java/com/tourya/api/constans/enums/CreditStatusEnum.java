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
    DELETED,

    /**
     * BE-19 (RN-036): crédito con expiration_date vencida. Marca inaccesible en
     * todos los flujos de checkout y transferencia. El job CreditExpirationJob
     * hace la transicion CREATED -> EXPIRED en la fecha exacta de expiracion.
     */
    EXPIRED,

    /**
     * TC-022 (#253): el turista solicito la devolucion en efectivo del credito.
     * Estado terminal para el turista (no puede reusar el credito). Espera a que
     * un usuario ADMIN o BACKOFFICE_OPERATION suba el comprobante para pasar a REFUNDED.
     * Transicion permitida: CREATED -> REFUND_REQUESTED.
     */
    REFUND_REQUESTED,

    /**
     * TC-022 (#253): ADMIN/BACKOFFICE_OPERATION confirmo la devolucion en efectivo
     * y subio el comprobante (S3/GCS). Estado terminal.
     * Transicion permitida: REFUND_REQUESTED -> REFUNDED.
     */
    REFUNDED
}


