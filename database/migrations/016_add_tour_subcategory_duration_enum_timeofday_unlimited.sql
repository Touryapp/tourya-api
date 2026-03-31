-- Migration: Add subcategory, duration enum, time_of_day and is_unlimited_capacity to tour
-- Date: 2026-03-31
-- Description: Supports new Tour detail fields for filtering and UI.

-- 1) Enums
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tour_subcategory_enum') THEN
        CREATE TYPE public.tour_subcategory_enum AS ENUM (
            'paseo_al_cayo',
            'tour_bahia_diurno',
            'ponton',
            'yate_de_lujo',
            'bar_en_el_agua',
            'semi_submarino',
            'snorkeling',
            'buceo',
            'snuba',
            'windsurf',
            'esqui_acuatico',
            'wakeboard',
            'fly_board',
            'kayak',
            'vuelta_a_la_isla_city_tour',
            'parasail',
            'jet_ski',
            'fiesta_noche_blanca',
            'paddle_board',
            'aquanautas',
            'picnic',
            'cocina_local',
            'moto',
            'bicicleta',
            'carro_playero'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tour_duration_enum') THEN
        CREATE TYPE public.tour_duration_enum AS ENUM (
            '1_a_2_horas',
            '2_a_4_horas',
            '4_a_6_horas',
            'hasta_1_dia',
            'hasta_3_dias',
            'hasta_5_dias'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tour_time_of_day_enum') THEN
        CREATE TYPE public.tour_time_of_day_enum AS ENUM (
            'manana',
            'tarde',
            'noche'
        );
    END IF;
END $$;

-- 2) Columns on tour
ALTER TABLE public.tour
    ADD COLUMN IF NOT EXISTS sub_category public.tour_subcategory_enum NULL,
    ADD COLUMN IF NOT EXISTS duration_enum public.tour_duration_enum NULL,
    ADD COLUMN IF NOT EXISTS time_of_day public.tour_time_of_day_enum[] NULL,
    ADD COLUMN IF NOT EXISTS is_unlimited_capacity bool NOT NULL DEFAULT false;

COMMENT ON COLUMN public.tour.sub_category IS 'Subcategoría del tour (enum) relacionada lógicamente a la categoría';
COMMENT ON COLUMN public.tour.duration_enum IS 'Duración del tour en rangos predefinidos (enum)';
COMMENT ON COLUMN public.tour.time_of_day IS 'Horarios del tour (manana/tarde/noche). Mínimo 1 valor';
COMMENT ON COLUMN public.tour.is_unlimited_capacity IS 'Indica si el tour es ilimitado (true) o limitado (false)';

-- 3) Backfill defaults for existing tours
UPDATE public.tour
SET
    time_of_day = COALESCE(time_of_day, ARRAY['manana']::public.tour_time_of_day_enum[]),
    duration_enum = COALESCE(duration_enum, '1_a_2_horas'::public.tour_duration_enum),
    sub_category = COALESCE(sub_category, 'tour_bahia_diurno'::public.tour_subcategory_enum)
WHERE time_of_day IS NULL OR duration_enum IS NULL OR sub_category IS NULL;

