package com.tourya.api.agents.backoffice;

import lombok.Builder;

/**
 * IA-08: pasajero individual del borrador de manifiesto DIMAR
 * ({@link com.tourya.api.models.MaritimActivityReport}, RN-054).
 *
 * <p>Se agrega una fila por {@link com.tourya.api.models.Reservation} confirmada
 * (delivery_status PENDING o DELIVERED). El operador humano revisa antes de
 * subir a DIMAR — mismo patron manual de hoy, el agente solo pre-arma.</p>
 *
 * @param reservationId    Id de la reserva origen.
 * @param tourName         Nombre del tour en espanol (RN-011 default).
 * @param payerName        Nombre del titular del pago (payment.payer_name).
 * @param documentType     Tipo de documento (CC/CE/PPT/PA...).
 * @param documentNumber   Numero del documento del titular.
 * @param ageType          ADULT | CHILD | INFANT — desglosa a partir de
 *                         {@code shopping_cart_item_detail.age_type}.
 * @param quantity         Cantidad de pasajeros de este {@code ageType} en la reserva.
 */
@Builder
public record DimarPassengerRow(
        Long reservationId,
        String tourName,
        String payerName,
        String documentType,
        String documentNumber,
        String ageType,
        Integer quantity
) {}
