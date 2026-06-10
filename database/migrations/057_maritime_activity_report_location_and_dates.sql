-- Migration: Reportes DIMAR v2 — ubicación por ID, categoría/subcategoría, rango de fechas
-- Date: 2026-06-09

ALTER TABLE public.maritim_activity_report
    ADD COLUMN IF NOT EXISTS country_id int4 NULL REFERENCES public.country(id),
    ADD COLUMN IF NOT EXISTS state_id int4 NULL REFERENCES public.state(id),
    ADD COLUMN IF NOT EXISTS city_id int4 NULL REFERENCES public.city(id),
    ADD COLUMN IF NOT EXISTS business_category_id int4 NULL REFERENCES public.tour_business_category(id),
    ADD COLUMN IF NOT EXISTS subcategory_code varchar(120) NULL,
    ADD COLUMN IF NOT EXISTS report_start_date date NULL,
    ADD COLUMN IF NOT EXISTS report_end_date date NULL;

UPDATE public.maritim_activity_report
SET report_start_date = report_date,
    report_end_date = report_date
WHERE report_start_date IS NULL
  AND report_date IS NOT NULL;

ALTER TABLE public.maritim_activity_report DROP COLUMN IF EXISTS country;
ALTER TABLE public.maritim_activity_report DROP COLUMN IF EXISTS city;
ALTER TABLE public.maritim_activity_report DROP COLUMN IF EXISTS department;
ALTER TABLE public.maritim_activity_report DROP COLUMN IF EXISTS activity;
ALTER TABLE public.maritim_activity_report DROP COLUMN IF EXISTS report_date;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_maritim_report_subcategory'
    ) THEN
        ALTER TABLE public.maritim_activity_report
            ADD CONSTRAINT fk_maritim_report_subcategory
            FOREIGN KEY (subcategory_code)
            REFERENCES public.tour_business_subcategory_mapping(subcategory_code);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_maritim_activity_report_dates
    ON public.maritim_activity_report (report_start_date, report_end_date);

CREATE INDEX IF NOT EXISTS idx_maritim_activity_report_location
    ON public.maritim_activity_report (country_id, state_id, city_id);

CREATE INDEX IF NOT EXISTS idx_maritim_activity_report_subcategory
    ON public.maritim_activity_report (subcategory_code);

COMMENT ON COLUMN public.maritim_activity_report.country_id IS 'ID del país';
COMMENT ON COLUMN public.maritim_activity_report.state_id IS 'ID del departamento (state)';
COMMENT ON COLUMN public.maritim_activity_report.city_id IS 'ID de la ciudad';
COMMENT ON COLUMN public.maritim_activity_report.business_category_id IS 'ID de tour_business_category';
COMMENT ON COLUMN public.maritim_activity_report.subcategory_code IS 'Código de subcategoría del tour';
COMMENT ON COLUMN public.maritim_activity_report.report_start_date IS 'Fecha inicio de vigencia del reporte';
COMMENT ON COLUMN public.maritim_activity_report.report_end_date IS 'Fecha fin de vigencia del reporte';
