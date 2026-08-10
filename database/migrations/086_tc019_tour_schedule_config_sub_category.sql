-- Migration 086: TC-019 (#231) — agrega sub_category a tour_schedule_config + filter en get_templates_by_provider
-- Date: 2026-08-10
-- Description:
--   Luis pidio en #231 que el combo de Template en Configuracion de Calendario del tour
--   filtre solo templates de la misma subcategoria que el tour, y que en la creacion
--   de Template haya un campo Subcategoria.
--
--   Cambios:
--   (a) Agrega columna sub_category (tour_subcategory_enum, nullable) a tour_schedule_config.
--       Templates existentes quedan con NULL (no filtrable por subcat hasta ser editados).
--   (b) Recrea get_templates_by_provider para aceptar un segundo parametro p_sub_category
--       opcional. Si viene, filtra por c.sub_category::text = p_sub_category. Tambien
--       incluye sub_category en el JSON output para el frontend.

ALTER TABLE public.tour_schedule_config
  ADD COLUMN IF NOT EXISTS sub_category tour_subcategory_enum NULL;

DROP FUNCTION IF EXISTS public.get_templates_by_provider(integer);
DROP FUNCTION IF EXISTS public.get_templates_by_provider(integer, text);

CREATE OR REPLACE FUNCTION public.get_templates_by_provider(
    p_provider_id integer,
    p_sub_category text DEFAULT NULL
)
RETURNS SETOF jsonb
LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN QUERY
    SELECT jsonb_build_object(
        'id', c.id,
        'providerId', c.provider_id,
        'label', c.label,
        'daysOfWeek', c.days_of_week,
        -- TC-019 (#231): expone la subcategoria del template para que el frontend pueda
        -- distinguir a que subcategorias aplica y mostrarla en la lista.
        'subCategory', c.sub_category,
        'slots', (
            SELECT COALESCE(
                jsonb_agg(
                    jsonb_build_object(
                        'id', s.id,
                        'startTime', s.start_time,
                        'endTime', s.end_time,
                        'capacity', s.capacity,
                        'bookings', COALESCE(s.bookings, 0),
                        'availability', COALESCE(s.availability, 0),
                        'minCapacityCalc', s.min_capacity_calc,
                        'checkAvailability', COALESCE(s.check_availability, false),
                        'prices', (
                            SELECT COALESCE(
                                jsonb_agg(
                                    jsonb_build_object(
                                        'id', p.id,
                                        'ageType', p.age_type,
                                        'price', p.price,
                                        'providerPrice', p.provider_price
                                    )
                                    ORDER BY p.id
                                ),
                                '[]'::jsonb
                            )
                            FROM public.tour_schedule_config_price p
                            WHERE p.slot_id = s.id
                        )
                    )
                    ORDER BY s.id
                ),
                '[]'::jsonb
            )
            FROM public.tour_schedule_config_slot s
            WHERE s.config_id = c.id
        )
    )
    FROM public.tour_schedule_config c
    WHERE c.provider_id = p_provider_id
      AND COALESCE(c.is_template, false) = true
      -- TC-019 (#231): filtrar por subcategoria si viene. Templates con sub_category=NULL
      -- se ocultan al pasar filtro (no aplican a ninguna subcat especifica).
      AND (p_sub_category IS NULL OR c.sub_category::text = p_sub_category);
END;
$function$;
