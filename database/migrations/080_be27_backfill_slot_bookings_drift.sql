-- Migration 080: BE-27 — backfill de slot.bookings / slot.availability
-- Date: 2026-07-22
-- Description:
--   El TemporalReservationExpiryJob y el PendingReservationNoShowJob marcaban
--   reservas como CANCELED/NO_SHOW sin llamar a recalculate. El campo denormalizado
--   `tour_schedule_config_slot.bookings` quedaba inflado, acumulando drift sobre el tiempo.
--
--   Antes de este PR, dev tenia 33 slots con drift (drift total = 54 unidades, max = 20).
--
--   El fix del codigo (esta misma rama) evita drift futuro. Esta migracion limpia el
--   drift acumulado recalculando `bookings` y `availability` como la suma de unidades
--   de reservas activas (TEMPORAL/PENDING/DELIVERED), respetando priceType:
--     - grupo: 1 unidad por reserva
--     - individual: suma de pax (shopping_cart_item_detail.quantity)
--
--   Idempotente: correr varias veces produce el mismo resultado.

UPDATE public.tour_schedule_config_slot s
SET bookings = COALESCE((
    SELECT SUM(
        CASE
            WHEN t.price_type = 'grupo' THEN 1
            ELSE COALESCE((
                SELECT SUM(d.quantity)
                FROM shopping_cart_item_detail d
                WHERE d.shopping_cart_item_id = i.id
            ), 0)
        END
    )
    FROM reservation r
    JOIN shopping_cart_item i ON i.id = r.item_id
    JOIN tour_schedule ts ON ts.id = i.tour_schedule_id
    JOIN tour t ON t.id = ts.tour_id
    WHERE i.slot_id = s.id
      AND r.delivery_status IN ('TEMPORAL', 'PENDING', 'DELIVERED')
), 0)::int
WHERE s.bookings IS NOT NULL;

UPDATE public.tour_schedule_config_slot
SET availability = GREATEST(0, COALESCE(capacity, 0) - COALESCE(bookings, 0))
WHERE capacity IS NOT NULL;
