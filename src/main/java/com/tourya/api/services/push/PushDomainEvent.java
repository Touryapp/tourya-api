package com.tourya.api.services.push;

/**
 * MO-40b: eventos de dominio de push notifications, publicados con
 * {@code ApplicationEventPublisher} desde flows de negocio.
 *
 * <p>El listener corre con {@code @TransactionalEventListener(AFTER_COMMIT)} y
 * {@code @Async}. Esto garantiza:</p>
 * <ul>
 *   <li>Si la tx principal hace rollback, el push nunca se envia.</li>
 *   <li>Si el push falla, la tx principal ya cerro — nada afecta al negocio.</li>
 *   <li>El listener no bloquea al productor (respuesta HTTP mas rapida).</li>
 * </ul>
 *
 * <p>Snapshot: los eventos llevan los datos ya resueltos (tourName,
 * providerUserId, etc.) en vez de IDs, para evitar rehacer queries en el
 * listener y evitar problemas de sesion JPA cerrada.</p>
 */
public sealed interface PushDomainEvent {

    /** Push al turista al confirmarse una reserva. Uno por reserva. */
    record ReservationConfirmedForTourist(
            Integer touristUserId,
            String tourName,
            Long reservationId
    ) implements PushDomainEvent {}

    /** Push al provider por una nueva reserva. Ya dedup-eado por proveedor por pago. */
    record NewReservationForProvider(
            Integer providerUserId,
            String tourName,
            Long reservationId
    ) implements PushDomainEvent {}

    /** Push cuando el provider responde una review. */
    record ReviewReplied(
            Integer touristUserId,
            String tourName,
            Long reservationId
    ) implements PushDomainEvent {}

    /** Recordatorio 24h antes del tour. */
    record TourReminder24h(
            Integer touristUserId,
            String tourName,
            Long reservationId
    ) implements PushDomainEvent {}

    /** Recordatorio 30d/7d antes de que expire un credito. */
    record CreditExpiringSoon(
            Integer userId,
            int daysAdvance
    ) implements PushDomainEvent {}

    /** Notificacion de que un credito ha expirado. */
    record CreditExpired(
            Integer userId
    ) implements PushDomainEvent {}

    /** BE-23: notifica al turista que su reserva fue cancelada por bandera roja DIMAR. */
    record ReservationCanceledByRain(
            Integer touristUserId,
            String tourName,
            Long reservationId
    ) implements PushDomainEvent {}
}
