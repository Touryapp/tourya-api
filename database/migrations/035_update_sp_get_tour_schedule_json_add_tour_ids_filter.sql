-- Migration: Add tourIds filter to sp_get_tour_schedule_json
-- Date: 2026-04-26

DROP FUNCTION IF EXISTS public.sp_get_tour_schedule_json(jsonb);

CREATE OR REPLACE FUNCTION public.sp_get_tour_schedule_json(filters jsonb)
 RETURNS TABLE(result jsonb)
 LANGUAGE plpgsql
AS $function$
DECLARE
  v_page int := COALESCE((filters->>'page')::int, 0);
  v_size int := COALESCE((filters->>'size')::int, 10);

  v_start_date date := COALESCE((filters->>'start_date')::date, (filters->>'startDate')::date);
  v_end_date   date := COALESCE((filters->>'end_date')::date,   (filters->>'endDate')::date);

  v_state_txt text := NULLIF(COALESCE(filters->>'state', filters->>'stateId'), '');
  v_city_txt  text := NULLIF(COALESCE(filters->>'city',  filters->>'cityId'),  '');

  v_provider_state_txt text := NULLIF(filters->>'providerStateId', '');
  v_provider_city_txt  text := NULLIF(filters->>'providerCityId',  '');

  v_duration_txt      text := NULLIF(filters->>'duration','');
  v_category_txt      text := NULLIF(COALESCE(filters->>'category', filters->>'categoryId'), '');
  v_duration_type_txt text := NULLIF(COALESCE(filters->>'duration_type', filters->>'durationType'), '');
  v_age_type_txt      text := UPPER(NULLIF(COALESCE(filters->>'age_type', filters->>'ageType'), ''));
  v_category_ids int[] := CASE
    WHEN filters ? 'categoryIds' AND jsonb_typeof(filters->'categoryIds') = 'array' THEN
      ARRAY(SELECT jsonb_array_elements_text(filters->'categoryIds')::int)
    WHEN NULLIF(COALESCE(filters->>'category', filters->>'categoryId'), '') IS NOT NULL THEN
      ARRAY[COALESCE(filters->>'category', filters->>'categoryId')::int]
    ELSE NULL
  END;

  v_sub_category_txt  text := NULLIF(COALESCE(filters->>'subCategory', filters->>'sub_category'), '');
  v_duration_enum_txt text := NULLIF(COALESCE(filters->>'durationEnum', filters->>'duration_enum'), '');
  v_time_of_day_txt   text := NULLIF(COALESCE(filters->>'timeOfDay', filters->>'time_of_day'), '');
  v_sub_categories text[] := CASE
    WHEN filters ? 'subCategories' AND jsonb_typeof(filters->'subCategories') = 'array' THEN
      ARRAY(SELECT jsonb_array_elements_text(filters->'subCategories'))
    WHEN NULLIF(COALESCE(filters->>'subCategory', filters->>'sub_category'), '') IS NOT NULL THEN
      ARRAY[COALESCE(filters->>'subCategory', filters->>'sub_category')]
    ELSE NULL
  END;
  v_duration_enums text[] := CASE
    WHEN filters ? 'durationEnums' AND jsonb_typeof(filters->'durationEnums') = 'array' THEN
      ARRAY(SELECT jsonb_array_elements_text(filters->'durationEnums'))
    WHEN NULLIF(COALESCE(filters->>'durationEnum', filters->>'duration_enum'), '') IS NOT NULL THEN
      ARRAY[COALESCE(filters->>'durationEnum', filters->>'duration_enum')]
    ELSE NULL
  END;
  v_time_of_day_arr text[] := CASE
    WHEN filters ? 'timeOfDay' AND jsonb_typeof(filters->'timeOfDay') = 'array' THEN
      ARRAY(SELECT jsonb_array_elements_text(filters->'timeOfDay'))
    WHEN filters ? 'time_of_day' AND jsonb_typeof(filters->'time_of_day') = 'array' THEN
      ARRAY(SELECT jsonb_array_elements_text(filters->'time_of_day'))
    WHEN NULLIF(COALESCE(filters->>'timeOfDay', filters->>'time_of_day'), '') IS NOT NULL THEN
      ARRAY[COALESCE(filters->>'timeOfDay', filters->>'time_of_day')]
    ELSE NULL
  END;

  v_min_price numeric := COALESCE(NULLIF(filters->>'min_price','')::numeric,
                                   NULLIF(filters->>'minPrice','')::numeric);
  v_max_price numeric := COALESCE(NULLIF(filters->>'max_price','')::numeric,
                                   NULLIF(filters->>'maxPrice','')::numeric);

  v_tag text := NULLIF(COALESCE(filters->>'tag', filters->>'tags'), '');
  v_tag_ids int[] := CASE
    WHEN filters ? 'tagIds' AND jsonb_typeof(filters->'tagIds') = 'array' THEN
      ARRAY(SELECT jsonb_array_elements_text(filters->'tagIds')::int)
    ELSE NULL
  END;
  v_tag_names text[] := CASE
    WHEN filters ? 'tags' AND jsonb_typeof(filters->'tags') = 'array' THEN
      ARRAY(SELECT jsonb_array_elements_text(filters->'tags'))
    WHEN NULLIF(COALESCE(filters->>'tag', filters->>'tags'), '') IS NOT NULL THEN
      ARRAY[COALESCE(filters->>'tag', filters->>'tags')]
    ELSE NULL
  END;
  v_text_search text := NULLIF(filters->>'textSearch','');

  v_tour_id int := (filters->>'tourId')::int;
  v_tour_ids int[] := CASE
    WHEN filters ? 'tourIds' AND jsonb_typeof(filters->'tourIds') = 'array' THEN
      ARRAY(SELECT jsonb_array_elements_text(filters->'tourIds')::int)
    ELSE NULL
  END;

  v_requested_units int := COALESCE(
    NULLIF(COALESCE(filters->>'requestedUnits', filters->>'participants', filters->>'quantity'), '')::int,
    NULL
  );
  v_language text := COALESCE(NULLIF(filters->>'language', ''), 'es');
BEGIN
  RETURN QUERY
  SELECT jsonb_build_object(
    'tour', jsonb_build_object(
      'id', t.id,
      'name', t.name,
      'description', t.description,
      'categoryId', t.category_id,
      'categoryName', tc.name,
      'duration', t.duration,
      'durationType', t.duration_type,
      'rating', t.rating,
      'status', t.status,
      'priceType', t.price_type,
      'maxPeople', t.max_people,
      'isUnlimitedCapacity', t.is_unlimited_capacity,
      'subCategory', t.sub_category,
      'subCategoryName', tscm.display_name,
      'durationEnum', t.duration_enum,
      'timeOfDay', t.time_of_day,
      'tags', COALESCE((
        SELECT jsonb_agg(
          jsonb_build_object(
            'id', tg.id,
            'name', tg.nombre,
            'category', td.nombre
          )
        )
        FROM tour_tags tt
        JOIN tags tg ON tg.id = tt.tag_id
        JOIN tag_dimensions td ON td.id = tg.dimension_id
        WHERE tt.tour_id = t.id
      ), '[]'::jsonb),
      'address', jsonb_build_object(
        'country', a.country_id,
        'state', a.state_id,
        'city', a.city_id,
        'address', a.address,
        'latitude', a.latitude,
        'longitude', a.longitude
      ),
      'gallery', COALESCE((
        SELECT jsonb_agg(
          jsonb_build_object(
            'id', g.id,
            'imageUrl', g.image_url,
            'description', g.description,
            'order', g.order_index
          ) ORDER BY g.order_index
        )
        FROM tour_gallery g
        WHERE g.tour_id = t.id
      ), '[]'::jsonb)
    ),
    'schedules', COALESCE((
      SELECT jsonb_agg(sch_obj)
      FROM (
        SELECT jsonb_build_object(
          'id', ts.id,
          'scheduleDate', ts.schedule_date,
          'startTime', ts.start_time,
          'endTime', ts.end_time,
          'status', ts.status,
          'config', jsonb_build_object(
            'id', tsc.id,
            'slots', COALESCE((
              SELECT jsonb_agg(slot_obj)
              FROM (
                SELECT jsonb_build_object(
                  'slotId', sl.id,
                  'startTime', sl.start_time,
                  'endTime', sl.end_time,
                  'capacity', sl.capacity,
                  'bookings', sl.bookings,
                  'availability', sl.availability,
                  'minCapacityCalc', sl.min_capacity_calc,
                  'checkAvailability', sl.check_availability,
                  'prices', COALESCE((
                    SELECT jsonb_agg(
                      jsonb_build_object(
                        'ageType', p.age_type,
                        'minAge', arc.min_age,
                        'maxAge', arc.max_age,
                        'price', p.price,
                        'providerPrice', p.provider_price
                      )
                    )
                    FROM tour_schedule_config_price p
                    LEFT JOIN age_range_config arc ON arc.age_type = p.age_type
                    WHERE p.slot_id = sl.id
                  ), '[]'::jsonb),
                  'highestPrice', (
                    SELECT jsonb_build_object(
                      'ageType', hp.age_type,
                      'price', hp.price
                    )
                    FROM tour_schedule_config_price hp
                    WHERE hp.slot_id = sl.id
                    ORDER BY hp.price DESC
                    LIMIT 1
                  )
                ) AS slot_obj
                FROM tour_schedule_config_slot sl
                WHERE sl.config_id = tsc.id
                  AND (v_requested_units IS NULL OR sl.check_availability IS FALSE OR sl.availability >= v_requested_units)
              ) x
            ), '[]'::jsonb)
          )
        ) AS sch_obj
        FROM tour_schedule ts
        LEFT JOIN tour_schedule_config tsc ON tsc.schedule_id = ts.id
        WHERE ts.tour_id = t.id
          AND (v_start_date IS NULL OR ts.schedule_date >= v_start_date)
          AND (v_end_date IS NULL OR ts.schedule_date <= v_end_date)
      ) y
    ), '[]'::jsonb)
  )
  FROM tour t
  JOIN tour_category tc ON tc.id = t.category_id
  LEFT JOIN tour_subcategory_map tscm ON tscm.code = t.sub_category
  LEFT JOIN tour_address a ON a.tour_id = t.id
  WHERE 1=1
    AND (v_tour_id IS NULL OR t.id = v_tour_id)
    AND (v_tour_ids IS NULL OR t.id = ANY(v_tour_ids))
    AND (v_category_ids IS NULL OR t.category_id = ANY(v_category_ids))
    AND (v_sub_categories IS NULL OR t.sub_category = ANY(v_sub_categories))
    AND (v_duration_enums IS NULL OR t.duration_enum = ANY(v_duration_enums))
    AND (v_time_of_day_arr IS NULL OR EXISTS (
      SELECT 1 FROM unnest(t.time_of_day) tod WHERE tod = ANY(v_time_of_day_arr)
    ))
    AND (v_text_search IS NULL OR (t.name->>v_language ILIKE '%'||v_text_search||'%' OR t.description->>v_language ILIKE '%'||v_text_search||'%'))
    AND (v_state_txt IS NULL OR a.state_id = v_state_txt::int)
    AND (v_city_txt IS NULL OR a.city_id = v_city_txt::int)
  ORDER BY t.created_date DESC
  OFFSET v_page LIMIT v_size;
END;
$function$;

