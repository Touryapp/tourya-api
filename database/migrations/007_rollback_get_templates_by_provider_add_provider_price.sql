
CREATE OR REPLACE FUNCTION public.get_templates_by_provider(p_provider_id integer)
 RETURNS SETOF jsonb
 LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN QUERY
    SELECT
        jsonb_build_object(
            'id', c.id,  -- antes config_id
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
                        'id', s.id, -- antes slot_id
                        'startTime', s.start_time,
                        'endTime', s.end_time,
                        'minCapacity', s.min_capacity,
                        'maxCapacity', s.max_capacity,
                        'prices', (
                            SELECT jsonb_agg(
                                jsonb_build_object(
                                    'id', p.id, -- 🔑 corregido
                                    'ageType', p.age_type,
                                    'minAge', p.min_age,
                                    'maxAge', p.max_age,
                                    'price', p.price
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
$function$
;