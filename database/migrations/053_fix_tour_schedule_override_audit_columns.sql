-- Patch 052 si ya se ejecutó sin columnas de auditoría JPA (BaseEntity).

ALTER TABLE public.tour_schedule_slot_override
    ADD COLUMN IF NOT EXISTS last_modified_by INTEGER NULL;

ALTER TABLE public.tour_schedule_price_override
    ADD COLUMN IF NOT EXISTS last_modified_by INTEGER NULL;

-- Alinear NOT NULL / defaults con BaseEntity si la tabla se creó con 052 anterior
ALTER TABLE public.tour_schedule_slot_override
    ALTER COLUMN created_by SET DEFAULT 1;

ALTER TABLE public.tour_schedule_price_override
    ALTER COLUMN created_by SET DEFAULT 1;

UPDATE public.tour_schedule_slot_override SET created_by = 1 WHERE created_by IS NULL;
UPDATE public.tour_schedule_price_override SET created_by = 1 WHERE created_by IS NULL;

ALTER TABLE public.tour_schedule_slot_override
    ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE public.tour_schedule_price_override
    ALTER COLUMN created_by SET NOT NULL;
