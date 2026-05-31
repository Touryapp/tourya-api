-- Cuenta tours que coinciden con los mismos filtros que sp_get_tour_schedule_json (sin paginar)
CREATE OR REPLACE FUNCTION public.sp_count_tour_schedule_json(filters jsonb)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$
DECLARE
  v_start_date date := COALESCE((filters->>'start_date')::date, (filters->>'startDate')::date);
  v_end_date   date := COALESCE((filters->>'end_date')::date,   (filters->>'endDate')::date);
  v_state_txt text := NULLIF(COALESCE(filters->>'state', filters->>'stateId'), '');
  v_city_txt  text := NULLIF(COALESCE(filters->>'city',  filters->>'cityId'),  '');
  v_provider_state_txt text := NULLIF(filters->>'providerStateId', '');
  v_provider_city_txt  text := NULLIF(filters->>'providerCityId',  '');
  v_duration_txt      text := NULLIF(filters->>'duration','');
  v_category_ids int[] := CASE
    WHEN filters ? 'categoryIds' AND jsonb_typeof(filters->'categoryIds') = 'array' THEN
      ARRAY(SELECT jsonb_array_elements_text(filters->'categoryIds')::int)
    WHEN NULLIF(COALESCE(filters->>'category', filters->>'categoryId'), '') IS NOT NULL THEN
      ARRAY[COALESCE(filters->>'category', filters->>'categoryId')::int]
    ELSE NULL
  END;
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
  v_total bigint;
BEGIN
  SELECT COUNT(DISTINCT t.id) INTO v_total
  FROM tour t
  JOIN tour_address a ON a.tour_id = t.id
  JOIN provider pr ON pr.id = t.provider_id
  WHERE
    t.status = 'accepted'
    AND (v_tour_id IS NULL OR t.id = v_tour_id)
    AND (v_tour_ids IS NULL OR t.id = ANY(v_tour_ids))
    AND (v_state_txt IS NULL OR a.state_id::text = v_state_txt)
    AND (v_city_txt IS NULL OR a.city_id::text = v_city_txt)
    AND (v_provider_state_txt IS NULL OR pr.state_id::text = v_provider_state_txt)
    AND (v_provider_city_txt IS NULL OR pr.city_id::text = v_provider_city_txt)
    AND (v_duration_txt IS NULL OR t.duration::text = v_duration_txt)
    AND (v_category_ids IS NULL OR t.category_id = ANY(v_category_ids))
    AND (v_sub_categories IS NULL OR t.sub_category::text = ANY(v_sub_categories))
    AND (v_duration_enums IS NULL OR t.duration_enum::text = ANY(v_duration_enums))
    AND (
      v_time_of_day_arr IS NULL
      OR (
        t.time_of_day IS NOT NULL
        AND EXISTS (
          SELECT 1 FROM unnest(t.time_of_day::text[]) AS tod(value)
          WHERE tod.value = ANY(v_time_of_day_arr)
        )
      )
    )
    AND (
      (v_tag_ids IS NULL AND v_tag_names IS NULL) OR EXISTS (
        SELECT 1 FROM tour_tags tt2
        JOIN tags tg2 ON tg2.id = tt2.tag_id
        WHERE tt2.tour_id = t.id
          AND (
            (v_tag_ids IS NOT NULL AND tg2.id = ANY(v_tag_ids))
            OR (v_tag_names IS NOT NULL AND tg2.nombre = ANY(v_tag_names))
          )
      )
    )
    AND (
      v_text_search IS NULL OR
      (t.name->>v_language) ILIKE '%' || v_text_search || '%' OR
      (t.description->>v_language) ILIKE '%' || v_text_search || '%'
    )
    AND (
      (v_start_date IS NULL AND v_end_date IS NULL)
      OR EXISTS (
        SELECT 1 FROM tour_schedule sx
        LEFT JOIN tour_schedule_config scx ON scx.id = sx.config_id
        WHERE sx.tour_id = t.id
          AND (v_start_date IS NULL OR sx.schedule_date >= v_start_date)
          AND (v_end_date IS NULL OR sx.schedule_date <= v_end_date)
          AND (
            v_requested_units IS NULL
            OR COALESCE(t.is_unlimited_capacity, false) = true
            OR EXISTS (
              SELECT 1 FROM tour_schedule_config_slot slx
              WHERE slx.config_id = scx.id
                AND GREATEST(0, COALESCE(slx.capacity, 0) - COALESCE(slx.bookings, 0)) >= v_requested_units
            )
          )
      )
    )
    AND (
      v_min_price IS NULL
      OR EXISTS (
        SELECT 1 FROM tour_schedule s2
        JOIN tour_schedule_config sc2 ON sc2.id = s2.config_id
        JOIN tour_schedule_config_slot sl2 ON sl2.config_id = sc2.id
        JOIN tour_schedule_config_price p2 ON p2.slot_id = sl2.id
        WHERE s2.tour_id = t.id
          AND (v_start_date IS NULL OR s2.schedule_date >= v_start_date)
          AND (v_end_date IS NULL OR s2.schedule_date <= v_end_date)
          AND CAST(p2.age_type AS character varying) = 'ADULT'
          AND p2.price >= v_min_price
      )
    )
    AND (
      v_max_price IS NULL
      OR EXISTS (
        SELECT 1 FROM tour_schedule s3
        JOIN tour_schedule_config sc3 ON sc3.id = s3.config_id
        JOIN tour_schedule_config_slot sl3 ON sl3.config_id = sc3.id
        JOIN tour_schedule_config_price p3 ON p3.slot_id = sl3.id
        WHERE s3.tour_id = t.id
          AND (v_start_date IS NULL OR s3.schedule_date >= v_start_date)
          AND (v_end_date IS NULL OR s3.schedule_date <= v_end_date)
          AND CAST(p3.age_type AS character varying) = 'ADULT'
          AND p3.price <= v_max_price
      )
    );
  RETURN COALESCE(v_total, 0);
END;
$function$;
