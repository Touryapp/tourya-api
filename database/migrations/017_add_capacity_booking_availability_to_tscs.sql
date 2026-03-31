-- Migration: Add capacity, bookings, availability and checkAvailability fields to tour_schedule_config_slot
-- Date: 2026-03-31
-- Description: Implements slot capacity model on TSCS with derived fields.

ALTER TABLE public.tour_schedule_config_slot
    ADD COLUMN IF NOT EXISTS capacity int4 NULL,
    ADD COLUMN IF NOT EXISTS bookings int4 NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS availability int4 NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS min_capacity_calc int4 NULL,
    ADD COLUMN IF NOT EXISTS check_availability bool NOT NULL DEFAULT false;

COMMENT ON COLUMN public.tour_schedule_config_slot.capacity IS 'Capacidad del slot (unidades según tipo de precio del tour: personas o grupos)';
COMMENT ON COLUMN public.tour_schedule_config_slot.bookings IS 'Reservas vendidas/hold (TEMPORAL/PENDING/COMPLETED) excluyendo canceladas';
COMMENT ON COLUMN public.tour_schedule_config_slot.availability IS 'Disponibilidad calculada = capacity - bookings';
COMMENT ON COLUMN public.tour_schedule_config_slot.min_capacity_calc IS '40% de capacity (solo si tour.is_unlimited_capacity = false)';
COMMENT ON COLUMN public.tour_schedule_config_slot.check_availability IS 'Flag calculado según reglas de minCapacity y tipo de precio';

