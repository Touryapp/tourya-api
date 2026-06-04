-- Limpieza opcional: overrides de prueba tour 36 junio 2026.
-- Ejecutar ANTES de repetir la secuencia 15% / 8% / 20%.

DELETE FROM public.tour_schedule_price_override po
USING public.tour_schedule ts
WHERE po.schedule_id = ts.id
  AND ts.tour_id = 36
  AND ts.schedule_date >= DATE '2026-06-01'
  AND ts.schedule_date <= DATE '2026-06-30';

DELETE FROM public.tour_schedule_slot_override so
USING public.tour_schedule ts
WHERE so.schedule_id = ts.id
  AND ts.tour_id = 36
  AND ts.schedule_date >= DATE '2026-06-01'
  AND ts.schedule_date <= DATE '2026-06-30';

-- Slot compartido 686 (config 483): quitar % legacy en tabla global
UPDATE public.tour_schedule_config_slot
SET slot_porcentaje_tourya = 0
WHERE id = 686;
