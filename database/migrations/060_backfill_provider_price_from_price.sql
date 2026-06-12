-- La migración 006 agregó provider_price sin rellenar filas existentes.
-- Copiar price donde provider_price quedó NULL (datos creados antes del campo).

UPDATE public.tour_schedule_config_price
SET provider_price = price
WHERE provider_price IS NULL
  AND price IS NOT NULL;
