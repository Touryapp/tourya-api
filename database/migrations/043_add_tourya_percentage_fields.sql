-- Migration: Porcentaje Tourya en tour y slots
-- porcentaje_tourya / slot_porcentaje_tourya: fracción decimal (ej. 0.15 = 15%)
-- Precio venta = provider_price + (provider_price * slot_porcentaje_tourya)

ALTER TABLE public.tour
    ADD COLUMN IF NOT EXISTS porcentaje_tourya NUMERIC(8, 4) NULL DEFAULT 0;

COMMENT ON COLUMN public.tour.porcentaje_tourya IS
    'Comisión Tourya como fracción (0.15 = 15%). Solo editable por backoffice Tourya.';

ALTER TABLE public.tour_schedule_config_slot
    ADD COLUMN IF NOT EXISTS slot_porcentaje_tourya NUMERIC(8, 4) NULL DEFAULT 0;

COMMENT ON COLUMN public.tour_schedule_config_slot.slot_porcentaje_tourya IS
    'Copia del porcentaje del tour al crear el slot. Solo editable por backoffice Tourya.';

-- Backfill: copiar porcentaje del tour a slots existentes cuando el slot no tiene valor
UPDATE public.tour_schedule_config_slot sl
SET slot_porcentaje_tourya = COALESCE(t.porcentaje_tourya, 0)
FROM public.tour_schedule_config sc
JOIN public.tour t ON t.id = sc.tour_id
WHERE sc.id = sl.config_id
  AND (sl.slot_porcentaje_tourya IS NULL OR sl.slot_porcentaje_tourya = 0);

-- Detalle de reserva tour: precio proveedor al momento de la compra
ALTER TABLE public.tour_reservation_detail
    ADD COLUMN IF NOT EXISTS provider_price_at_reservation NUMERIC(12, 2) NULL;

COMMENT ON COLUMN public.tour_reservation_detail.provider_price_at_reservation IS
    'Precio unitario del proveedor al momento de la reserva.';
