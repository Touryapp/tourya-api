-- Migration 030: get_templates_by_provider ya no puede usar tour_schedule_config.is_unlimited_capacity
-- (eliminada en 029). La ilimitada vive en tour.is_unlimited_capacity.
-- Alinea slots con capacity/bookings/availability y precios sin min_age/max_age (age_range_config en app).

DROP FUNCTION IF EXISTS public.get_templates_by_provider(integer);

CREATE OR REPLACE FUNCTION public.get_templates_by_provider(p_provider_id integer)
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
      AND COALESCE(c.is_template, false) = true;
END;
$function$;
