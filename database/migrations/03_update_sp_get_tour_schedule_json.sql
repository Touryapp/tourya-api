-- Drop existing function if exists
DROP FUNCTION IF EXISTS sp_get_tour_schedule_json(jsonb);

-- Create the updated function with language parameter support
CREATE OR REPLACE FUNCTION sp_get_tour_schedule_json(filters jsonb)
RETURNS TABLE(result jsonb)
LANGUAGE plpgsql
AS $$
DECLARE
  -- Paginación
  v_page int := COALESCE((filters->>'page')::int, 0);
  v_size int := COALESCE((filters->>'size')::int, 10);

  -- Fechas
  v_start_date date := COALESCE((filters->>'start_date')::date, (filters->>'startDate')::date);
  v_end_date   date := COALESCE((filters->>'end_date')::date,   (filters->>'endDate')::date);

  -- Filtros por ubicación (address del tour)
  v_state_txt text := NULLIF(COALESCE(filters->>'state', filters->>'stateId'), '');
  v_city_txt  text := NULLIF(COALESCE(filters->>'city',  filters->>'cityId'),  '');

  -- Filtros por ubicación del provider (opcionales)
  v_provider_state_txt text := NULLIF(filters->>'providerStateId', '');
  v_provider_city_txt  text := NULLIF(filters->>'providerCityId',  '');

  -- Otros filtros
  v_duration_txt      text := NULLIF(filters->>'duration','');
  v_category_txt      text := NULLIF(COALESCE(filters->>'category', filters->>'categoryId'), '');
  v_duration_type_txt text := NULLIF(COALESCE(filters->>'duration_type', filters->>'durationType'), '');
  v_age_type_txt      text := NULLIF(COALESCE(filters->>'age_type',      filters->>'ageType'),      '');

  -- Precios numéricos
  v_min_price numeric := COALESCE(NULLIF(filters->>'min_price','')::numeric,
                                   NULLIF(filters->>'minPrice','')::numeric);
  v_max_price numeric := COALESCE(NULLIF(filters->>'max_price','')::numeric,
                                   NULLIF(filters->>'maxPrice','')::numeric);

  -- Otros
  v_tag text := NULLIF(COALESCE(filters->>'tag', filters->>'tags'), '');
  v_text_search text := NULLIF(filters->>'textSearch','');

  -- FILTRO DE TOUR_ID
  v_tour_id int := (filters->>'tourId')::int;

  -- PARÁMETRO DE IDIOMA (opcional, por defecto 'es')
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
    'schedules', CASE
      WHEN v_tour_id IS NOT NULL THEN
        COALESCE((
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
                      'minCapacity', sl.min_capacity,
                      'maxCapacity', sl.max_capacity,
                      'prices', COALESCE((
                        SELECT jsonb_agg(
                          jsonb_build_object(
                            'ageType', p.age_type,
                            'minAge', p.min_age,
                            'maxAge', p.max_age,
                            'price', p.price
                          )
                        )
                        FROM tour_schedule_config_price p
                        WHERE p.slot_id = sl.id
                        AND (v_age_type_txt IS NULL OR p.age_type::text = v_age_type_txt)
                        AND (v_min_price IS NULL OR p.price >= v_min_price)
                        AND (v_max_price IS NULL OR p.price <= v_max_price)
                      ), '[]'::jsonb),
                      'highestPrice', COALESCE((
                        SELECT jsonb_build_object(
                          'ageType', p.age_type,
                          'price', p.price
                        )
                        FROM tour_schedule_config_price p
                        WHERE p.slot_id = sl.id
                        AND (v_age_type_txt IS NULL OR p.age_type::text = v_age_type_txt)
                        AND (v_min_price IS NULL OR p.price >= v_min_price)
                        AND (v_max_price IS NULL OR p.price <= v_max_price)
                        ORDER BY p.price DESC
                        LIMIT 1
                      ), '{}'::jsonb)
                    )
                    ORDER BY sl.start_time
                  )
                  FROM tour_schedule_config_slot sl
                  WHERE sl.config_id = sc.id
                  AND (v_age_type_txt IS NULL OR EXISTS (SELECT 1 FROM tour_schedule_config_price px WHERE px.slot_id = sl.id AND px.age_type::text = v_age_type_txt))
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
      ELSE
        (
          SELECT jsonb_agg(
            jsonb_build_object(
              'config', jsonb_build_object(
                'slots', jsonb_build_array(
                  jsonb_build_object(
                    'highestPrice', COALESCE((
                      SELECT jsonb_build_object(
                        'ageType', p.age_type,
                        'price', p.price
                      )
                      FROM tour_schedule s
                      JOIN tour_schedule_config sc ON sc.id = s.config_id
                      JOIN tour_schedule_config_slot sl ON sl.config_id = sc.id
                      JOIN tour_schedule_config_price p ON p.slot_id = sl.id
                      WHERE s.tour_id = t.id
                      AND (v_start_date IS NULL OR s.schedule_date >= v_start_date)
                      AND (v_end_date IS NULL OR s.schedule_date <= v_end_date)
                      ORDER BY p.price DESC
                      LIMIT 1
                    ), '{}'::jsonb)
                  )
                )
              )
            )
          )
        )
    END
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
$$;
