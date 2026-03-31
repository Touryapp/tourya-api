-- Migration: Fix sp_get_tour_schedule_json ELSE branch (missing FROM sc)
-- Date: 2026-03-31
--
-- Root cause:
-- The previous definition referenced alias `sc` inside the CASE ELSE branch without a FROM clause,
-- causing: ERROR: missing FROM-clause entry for table "sc".

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

  v_sub_category_txt  text := NULLIF(COALESCE(filters->>'subCategory', filters->>'sub_category'), '');
  v_duration_enum_txt text := NULLIF(COALESCE(filters->>'durationEnum', filters->>'duration_enum'), '');
  v_time_of_day_txt   text := NULLIF(COALESCE(filters->>'timeOfDay', filters->>'time_of_day'), '');

  v_min_price numeric := COALESCE(NULLIF(filters->>'min_price','')::numeric,
                                   NULLIF(filters->>'minPrice','')::numeric);
  v_max_price numeric := COALESCE(NULLIF(filters->>'max_price','')::numeric,
                                   NULLIF(filters->>'maxPrice','')::numeric);

  v_tag text := NULLIF(COALESCE(filters->>'tag', filters->>'tags'), '');
  v_text_search text := NULLIF(filters->>'textSearch','');

  v_tour_id int := (filters->>'tourId')::int;
  v_language text := COALESCE(NULLIF(filters->>'language', ''), 'es');
BEGIN
  RETURN QUERY
  SELECT jsonb_build_object(
    'tour', jsonb_build_object(
      'id', t.id,
      'name', t.name,
      'description', t.description,
      'duration', t.duration,
      'durationType', t.duration_type,
      'rating', t.rating,
      'status', t.status,
      'priceType', t.price_type,
      'maxPeople', t.max_people,
      'isUnlimitedCapacity', t.is_unlimited_capacity,
      'subCategory', t.sub_category,
      'durationEnum', t.duration_enum,
      'timeOfDay', t.time_of_day,
      'tags', COALESCE((
        SELECT jsonb_agg(
          jsonb_build_object(
            'id', tg.id,
            'name', tg.name,
            'category', tg.category
          )
        )
        FROM tour_tag_mapping tm
        JOIN tour_tag tg ON tg.id = tm.tag_id
        WHERE tm.tour_id = t.id
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
          )
        )
        FROM tour_gallery g
        WHERE g.tour_id = t.id
      ), '[]'::jsonb)
    ),
    -- Always return schedules for search (fixes missing sc alias)
    'schedules', COALESCE((
      SELECT jsonb_agg(
        jsonb_build_object(
          'id', s.id,
          'scheduleDate', s.schedule_date,
          'maxCapacity', s.max_capacity,
          'reservedCapacity', s.reserved_capacity,
          'isUnlimitedCapacity', s.is_unlimited_capacity,
          'status', s.status,
          'config', jsonb_build_object(
            'id', sc.id,
            'slots', COALESCE((
              SELECT jsonb_agg(
                jsonb_build_object(
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
                    LEFT JOIN public.age_range_config arc ON arc.age_type = CAST(p.age_type AS character varying)
                    WHERE p.slot_id = sl.id
                      AND (v_age_type_txt IS NULL OR CAST(p.age_type AS character varying) = v_age_type_txt)
                      AND (v_min_price IS NULL OR p.price >= v_min_price)
                      AND (v_max_price IS NULL OR p.price <= v_max_price)
                  ), '[]'::jsonb)
                )
                ORDER BY sl.start_time
              )
              FROM tour_schedule_config_slot sl
              WHERE sl.config_id = sc.id
                AND (v_age_type_txt IS NULL OR EXISTS (SELECT 1 FROM tour_schedule_config_price px WHERE px.slot_id = sl.id AND CAST(px.age_type AS character varying) = v_age_type_txt))
                AND (v_min_price IS NULL OR EXISTS (SELECT 1 FROM tour_schedule_config_price px WHERE px.slot_id = sl.id AND px.price >= v_min_price))
                AND (v_max_price IS NULL OR EXISTS (SELECT 1 FROM tour_schedule_config_price px WHERE px.slot_id = sl.id AND px.price <= v_max_price))
            ), '[]'::jsonb)
          )
        )
        ORDER BY s.schedule_date ASC
      )
      FROM tour_schedule s
      LEFT JOIN tour_schedule_config sc ON sc.id = s.config_id
      WHERE s.tour_id = t.id
        AND (v_start_date IS NULL OR s.schedule_date >= v_start_date)
        AND (v_end_date IS NULL OR s.schedule_date <= v_end_date)
    ), '[]'::jsonb)
  ) AS result
  FROM tour t
  JOIN tour_address a ON a.tour_id = t.id
  JOIN provider pr ON pr.id = t.provider_id
  WHERE
    t.status = 'accepted'
    AND (v_tour_id IS NULL OR t.id = v_tour_id)
    AND (v_state_txt IS NULL OR a.state_id::text = v_state_txt)
    AND (v_city_txt IS NULL OR a.city_id::text = v_city_txt)
    AND (v_provider_state_txt IS NULL OR pr.state_id::text = v_provider_state_txt)
    AND (v_provider_city_txt IS NULL OR pr.city_id::text = v_provider_city_txt)
    AND (v_duration_txt IS NULL OR t.duration::text = v_duration_txt)
    AND (v_duration_type_txt IS NULL OR t.duration_type::text = v_duration_type_txt)
    AND (v_category_txt IS NULL OR t.category_id::text = v_category_txt)
    AND (v_sub_category_txt IS NULL OR t.sub_category::text = v_sub_category_txt)
    AND (v_duration_enum_txt IS NULL OR t.duration_enum::text = v_duration_enum_txt)
    AND (v_time_of_day_txt IS NULL OR (t.time_of_day::text ILIKE '%' || v_time_of_day_txt || '%'))
    AND (
      v_tag IS NULL OR EXISTS (
        SELECT 1
        FROM tour_tag_mapping tm2
        JOIN tour_tag tg2 ON tg2.id = tm2.tag_id
        WHERE tm2.tour_id = t.id
          AND tg2.name ILIKE '%' || v_tag || '%'
      )
    )
    AND (
      v_text_search IS NULL OR
      (t.name->>v_language) ILIKE '%' || v_text_search || '%' OR
      (t.description->>v_language) ILIKE '%' || v_text_search || '%'
    )
    AND (
      v_min_price IS NULL
      OR EXISTS (
        SELECT 1
        FROM tour_schedule s2
        JOIN tour_schedule_config sc2 ON sc2.id = s2.config_id
        JOIN tour_schedule_config_slot sl2 ON sl2.config_id = sc2.id
        JOIN tour_schedule_config_price p2 ON p2.slot_id = sl2.id
        WHERE s2.tour_id = t.id
          AND (v_start_date IS NULL OR s2.schedule_date >= v_start_date)
          AND (v_end_date IS NULL OR s2.schedule_date <= v_end_date)
          AND p2.price >= v_min_price
      )
    )
    AND (
      v_max_price IS NULL
      OR EXISTS (
        SELECT 1
        FROM tour_schedule s3
        JOIN tour_schedule_config sc3 ON sc3.id = s3.config_id
        JOIN tour_schedule_config_slot sl3 ON sl3.config_id = sc3.id
        JOIN tour_schedule_config_price p3 ON p3.slot_id = sl3.id
        WHERE s3.tour_id = t.id
          AND (v_start_date IS NULL OR s3.schedule_date >= v_start_date)
          AND (v_end_date IS NULL OR s3.schedule_date <= v_end_date)
          AND p3.price <= v_max_price
      )
    )
  GROUP BY t.id, a.id, pr.id
  ORDER BY t.id ASC
  LIMIT v_size
  OFFSET v_page * v_size;
END;
$function$;

