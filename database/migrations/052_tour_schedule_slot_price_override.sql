-- Porcentaje y precio de venta por instancia de schedule (día), sin pisar el slot compartido del config.

CREATE TABLE IF NOT EXISTS public.tour_schedule_slot_override (
    id SERIAL PRIMARY KEY,
    schedule_id INTEGER NOT NULL REFERENCES public.tour_schedule (id) ON DELETE CASCADE,
    slot_id INTEGER NOT NULL REFERENCES public.tour_schedule_config_slot (id) ON DELETE CASCADE,
    slot_porcentaje_tourya NUMERIC(8, 4) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NULL,
    created_by INTEGER NOT NULL DEFAULT 1,
    last_modified_by INTEGER NULL,
    CONSTRAINT uq_tour_schedule_slot_override UNIQUE (schedule_id, slot_id)
);

CREATE INDEX IF NOT EXISTS idx_tour_schedule_slot_override_schedule
    ON public.tour_schedule_slot_override (schedule_id);

COMMENT ON TABLE public.tour_schedule_slot_override IS
    'Porcentaje Tourya efectivo por schedule (día) y slot; usado por PUT /tour-schedules/tours/{tourId}/percentage.';

CREATE TABLE IF NOT EXISTS public.tour_schedule_price_override (
    id SERIAL PRIMARY KEY,
    schedule_id INTEGER NOT NULL REFERENCES public.tour_schedule (id) ON DELETE CASCADE,
    price_id INTEGER NOT NULL REFERENCES public.tour_schedule_config_price (id) ON DELETE CASCADE,
    price NUMERIC(12, 2) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NULL,
    created_by INTEGER NOT NULL DEFAULT 1,
    last_modified_by INTEGER NULL,
    CONSTRAINT uq_tour_schedule_price_override UNIQUE (schedule_id, price_id)
);

CREATE INDEX IF NOT EXISTS idx_tour_schedule_price_override_schedule
    ON public.tour_schedule_price_override (schedule_id);

COMMENT ON TABLE public.tour_schedule_price_override IS
    'Precio de venta recalculado por schedule y price_id tras actualizar % Tourya en un rango de fechas.';
