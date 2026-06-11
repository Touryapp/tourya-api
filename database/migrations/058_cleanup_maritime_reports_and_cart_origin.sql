-- Migration 058: Limpieza reportes DIMAR legacy + lugar de procedencia en carrito
-- Date: 2026-06-11
--
-- Ejecutar en DEV/QA/PROD después de 057.
-- Elimina reportes que quedaron incompletos tras la migración de esquema (sin ubicación/categoría/fechas).
-- Agrega país / departamento / ciudad de procedencia del turista en shopping_cart (checkout).

-- ---------------------------------------------------------------------------
-- 1) Diagnóstico (solo informativo; no modifica datos)
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_invalid bigint;
BEGIN
    SELECT COUNT(*) INTO v_invalid
    FROM public.maritim_activity_report
    WHERE country_id IS NULL
       OR state_id IS NULL
       OR city_id IS NULL
       OR business_category_id IS NULL
       OR subcategory_code IS NULL
       OR report_start_date IS NULL
       OR report_end_date IS NULL;

    RAISE NOTICE 'maritim_activity_report registros inválidos a eliminar: %', v_invalid;
END $$;

-- ---------------------------------------------------------------------------
-- 2) Limpieza automática: registros legacy no recuperables
--    (057 eliminó columnas texto country/city/department sin backfill a IDs)
-- ---------------------------------------------------------------------------
DELETE FROM public.maritim_activity_report
WHERE country_id IS NULL
   OR state_id IS NULL
   OR city_id IS NULL
   OR business_category_id IS NULL
   OR subcategory_code IS NULL
   OR report_start_date IS NULL
   OR report_end_date IS NULL;

-- ---------------------------------------------------------------------------
-- 3) Endurecer esquema: obligatorio para registros nuevos
-- ---------------------------------------------------------------------------
ALTER TABLE public.maritim_activity_report
    ALTER COLUMN country_id SET NOT NULL,
    ALTER COLUMN state_id SET NOT NULL,
    ALTER COLUMN city_id SET NOT NULL,
    ALTER COLUMN business_category_id SET NOT NULL,
    ALTER COLUMN subcategory_code SET NOT NULL,
    ALTER COLUMN report_start_date SET NOT NULL,
    ALTER COLUMN report_end_date SET NOT NULL;

-- ---------------------------------------------------------------------------
-- 4) Lugar de procedencia en carrito (País / Departamento / Ciudad)
--    Hospedaje sigue en accommodation_* (nombre + lat/long Google Maps)
-- ---------------------------------------------------------------------------
ALTER TABLE public.shopping_cart
    ADD COLUMN IF NOT EXISTS origin_country_id int4 NULL REFERENCES public.country(id),
    ADD COLUMN IF NOT EXISTS origin_state_id int4 NULL REFERENCES public.state(id),
    ADD COLUMN IF NOT EXISTS origin_city_id int4 NULL REFERENCES public.city(id);

CREATE INDEX IF NOT EXISTS idx_shopping_cart_origin_location
    ON public.shopping_cart (origin_country_id, origin_state_id, origin_city_id);

COMMENT ON COLUMN public.shopping_cart.origin_country_id IS 'País de procedencia del turista (checkout)';
COMMENT ON COLUMN public.shopping_cart.origin_state_id IS 'Departamento/estado de procedencia del turista (checkout)';
COMMENT ON COLUMN public.shopping_cart.origin_city_id IS 'Ciudad de procedencia del turista (checkout)';
