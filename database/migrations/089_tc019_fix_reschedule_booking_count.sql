-- Migration 089: TC-019 (#231) bug 3 — reagendamiento no refleja el cambio de disponibilidad
-- Date: 2026-08-12
-- Description:
--   Luis reporto 2026-08-12: al reagendar una reserva la cantidad no se descuenta del dia
--   original ni se suma al dia nuevo. La UI queda con disponibilidad "drift" ambos lados.
--
--   Causa raiz:
--     El helper `fn_slot_booked_units_on_schedule` (migracion 078, restaurado en 088) filtra
--     reservas activas por `delivery_status IN ('TEMPORAL','PENDING','DELIVERED')`. Cuando
--     una reserva se reagenda via `handleRescheduleEqualOrLower`:
--       - `shopping_cart_item.tour_schedule_id` y `slot_id` se actualizan al NUEVO schedule.
--       - `reservation.delivery_status` pasa a 'RESCHEDULED'.
--     El helper deja de contarla en el dia viejo (correcto: el item ya no apunta ahi) y NO
--     la cuenta en el dia nuevo (bug: RESCHEDULED excluido). Resultado: bookings=0 en ambos
--     lados; la nueva reserva se vuelve "invisible" para el calculo de disponibilidad.
--
--   Verificacion (dev, 2026-08-12):
--     SELECT reservation_id, delivery_status, i.slot_id, i.tour_schedule_id, ts.schedule_date
--       FROM reservation r JOIN shopping_cart_item i ON i.id=r.item_id
--       JOIN tour_schedule ts ON ts.id=i.tour_schedule_id
--       WHERE r.delivery_status='RESCHEDULED';
--       -> RES 333 -> slot 1032 / schedule 3428 (grupo, tour 72)   -> helper = 0 (deberia 1)
--       -> RES 335 -> slot 1042 / schedule 3465 (individual, tour 36) -> helper = 0 (deberia 2)
--
--   Esta migracion:
--     PARTE 1) Recrea `fn_slot_booked_units_on_schedule` incluyendo 'RESCHEDULED' en el filtro.
--              El helper es la fuente de verdad del SP `sp_get_tour_schedule_json` (via
--              migracion 088), por lo que el fix impacta directo a la disponibilidad de la UI.
--     PARTE 2) Backfill idempotente de los campos denormalizados
--              `tour_schedule_config_slot.bookings` y `.availability` usando la definicion
--              nueva (RESCHEDULED cuenta como activa). Cierra drift acumulado tanto por este
--              bug como por transiciones sin recalculate (patron BE-27).
--
--   Idempotente: correr N veces produce el mismo resultado. Diff Java del mismo PR ajusta la
--   query JPA `ReservationRepository.countActiveBookingUnitsForSlotOnDate` para que
--   `ensureSlotHasCapacity` no habilite oversell en la fecha destino de un reagendamiento.

------------------------------------------------------------------------
-- PARTE 1: fn_slot_booked_units_on_schedule — incluir RESCHEDULED
------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.fn_slot_booked_units_on_schedule(p_slot_id int, p_schedule_id int)
RETURNS int
LANGUAGE sql
STABLE
AS $$
  SELECT COALESCE(SUM(
    CASE
      WHEN t.price_type = 'grupo' THEN 1
      ELSE COALESCE((
        SELECT SUM(d.quantity)
        FROM shopping_cart_item_detail d
        WHERE d.shopping_cart_item_id = i.id
      ), 0)
    END
  ), 0)::int
  FROM reservation r
  JOIN shopping_cart_item i ON i.id = r.item_id
  JOIN tour_schedule ts ON ts.id = i.tour_schedule_id
  JOIN tour t ON t.id = ts.tour_id
  WHERE i.slot_id = p_slot_id
    AND ts.id = p_schedule_id
    -- TC-019 (#231) bug 3: RESCHEDULED tambien ocupa cupo — el cliente asistira al nuevo dia.
    AND r.delivery_status IN ('TEMPORAL', 'PENDING', 'DELIVERED', 'RESCHEDULED');
$$;

COMMENT ON FUNCTION public.fn_slot_booked_units_on_schedule(int, int) IS
  'TC-004 + TC-019 (#231): unidades reservadas activas (TEMPORAL/PENDING/DELIVERED/RESCHEDULED) para un slot en un tour_schedule especifico. Respeta priceType (grupo=1 por reserva, individual=suma pax). RESCHEDULED se cuenta porque el item apunta al nuevo (slot,schedule) tras el reagendamiento.';

------------------------------------------------------------------------
-- PARTE 2: backfill denormalizado `slot.bookings` y `slot.availability`
--          usando la definicion nueva (RESCHEDULED cuenta). Complementa
--          la migracion 080 (BE-27) para el nuevo caso.
------------------------------------------------------------------------
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
      AND r.delivery_status IN ('TEMPORAL', 'PENDING', 'DELIVERED', 'RESCHEDULED')
), 0)::int
WHERE s.bookings IS NOT NULL;

UPDATE public.tour_schedule_config_slot
SET availability = GREATEST(0, COALESCE(capacity, 0) - COALESCE(bookings, 0))
WHERE capacity IS NOT NULL;
