-- Migration: Update get_templates_by_provider function to include provider_price
-- Date: 2026-02-05
-- Description: Updates the get_templates_by_provider function to return provider_price in the prices JSON

-- Drop and recreate the function with the new field
DROP FUNCTION IF EXISTS public.get_templates_by_provider(integer);

CREATE OR REPLACE FUNCTION public.get_templates_by_provider(p_provider_id integer)
 RETURNS SETOF jsonb
 LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN QUERY
    SELECT
        jsonb_build_object(
            'id', c.id,
            'tourId', c.tour_id,
            'label', c.label,
            'startDate', c.start_date,
            'endDate', c.end_date,
            'daysOfWeek', c.days_of_week,
            'isUnlimitedCapacity', c.is_unlimited_capacity,
            'createdBy', c.created_by,
            'lastModifiedBy', c.last_modified_by,
            'createdDate', c.created_date,
            'lastModifiedDate', c.last_modified_date,
            'providerId', c.provider_id,
            'isTemplate', c.is_template,
            'slots', (
                SELECT jsonb_agg(
                    jsonb_build_object(
                        'id', s.id,
                        'startTime', s.start_time,
                        'endTime', s.end_time,
                        'minCapacity', s.min_capacity,
                        'maxCapacity', s.max_capacity,
                        'prices', (
                            SELECT jsonb_agg(
                                jsonb_build_object(
                                    'id', p.id,
                                    'ageType', p.age_type,
                                    'minAge', p.min_age,
                                    'maxAge', p.max_age,
                                    'price', p.price,
                                    'providerPrice', p.provider_price  -- NUEVO CAMPO
                                )
                            )
                            FROM public.tour_schedule_config_price p
                            WHERE p.slot_id = s.id
                        )
                    )
                )
                FROM public.tour_schedule_config_slot s
                WHERE s.config_id = c.id
            )
        )
    FROM public.tour_schedule_config c
    WHERE c.provider_id = p_provider_id
      AND c.is_template = true;
END;
$function$;
