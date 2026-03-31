-- Migration: Change tour.time_of_day to text[] to avoid Hibernate array cast issues
-- Date: 2026-03-31

-- If column already exists as enum[], convert it to text[]
ALTER TABLE public.tour
    ALTER COLUMN time_of_day TYPE text[]
    USING (time_of_day::text[]);

COMMENT ON COLUMN public.tour.time_of_day IS 'Horarios del tour (manana/tarde/noche) almacenado como text[] para compatibilidad con Hibernate';

