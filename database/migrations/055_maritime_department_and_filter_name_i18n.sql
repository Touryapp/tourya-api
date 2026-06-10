-- Migration: Maritime report department + i18n name for tags and business categories
-- Date: 2026-06-09

-- 1) Department en reportes DIMAR
ALTER TABLE public.maritim_activity_report
    ADD COLUMN IF NOT EXISTS department varchar(100) NULL;

COMMENT ON COLUMN public.maritim_activity_report.department IS 'Departamento donde se realiza la actividad';

-- 2) Tags: columna name (jsonb) para respuestas multilenguaje
ALTER TABLE public.tags
    ADD COLUMN IF NOT EXISTS name jsonb NULL;

UPDATE public.tags
SET name = jsonb_build_object('es', nombre, 'en', '', 'pt', '')
WHERE name IS NULL;

-- 3) Categorías de búsqueda: name como jsonb
ALTER TABLE public.tour_business_category
    DROP CONSTRAINT IF EXISTS tour_business_category_name_key;

ALTER TABLE public.tour_business_category
    ALTER COLUMN name TYPE jsonb
    USING jsonb_build_object('es', name, 'en', '', 'pt', '');

CREATE UNIQUE INDEX IF NOT EXISTS uq_tour_business_category_name_es
    ON public.tour_business_category ((name->>'es'));

-- 4) Subcategorías: columna name (jsonb)
ALTER TABLE public.tour_business_subcategory_mapping
    ADD COLUMN IF NOT EXISTS name jsonb NULL;

-- Traducciones completas: ejecutar 056_fix_filter_i18n_complete_translations.sql
