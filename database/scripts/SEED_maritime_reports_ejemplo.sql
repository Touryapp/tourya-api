-- =============================================================================
-- CARGA DE EJEMPLO — Reportes DIMAR (después de limpiar legacy)
-- =============================================================================
-- Inserta reportes de prueba usando IDs reales de country/state/city en tu BD.
-- Ajusta los ILIKE si tus nombres difieren (ej. Bolivar sin tilde).
--
--   psql -h HOST -U USER -d DB -f database/scripts/SEED_maritime_reports_ejemplo.sql
-- =============================================================================

DO $$
DECLARE
    v_country_id int;
    v_state_id int;
    v_city_id int;
    v_category_id int;
    v_user_id int;
    v_today date := CURRENT_DATE;
BEGIN
    SELECT id INTO v_country_id FROM public.country
    WHERE name ILIKE '%colombia%' ORDER BY id LIMIT 1;

    SELECT s.id INTO v_state_id
    FROM public.state s
    JOIN public.country c ON c.id = s.country_id
    WHERE c.id = v_country_id
      AND (s.name ILIKE '%bol%var%' OR s.name ILIKE '%bolivar%')
    ORDER BY s.id LIMIT 1;

    SELECT ci.id INTO v_city_id
    FROM public.city ci
    JOIN public.state s ON s.id = ci.state_id
    WHERE s.id = v_state_id
      AND ci.name ILIKE '%cartagena%'
    ORDER BY ci.id LIMIT 1;

    SELECT id INTO v_category_id FROM public.tour_business_category
    WHERE code = 'ACUATICO' LIMIT 1;

    SELECT id INTO v_user_id FROM public._user ORDER BY id LIMIT 1;

    IF v_country_id IS NULL OR v_state_id IS NULL OR v_city_id IS NULL THEN
        RAISE EXCEPTION 'No se encontró Colombia/Bolívar/Cartagena. Ejecuta primero: '
            'SELECT id, name FROM country; SELECT id, name, country_id FROM state; SELECT id, name, state_id FROM city;';
    END IF;

    IF v_category_id IS NULL THEN
        RAISE EXCEPTION 'No existe categoría ACUATICO. Ejecuta migración 024.';
    END IF;

    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'No hay usuarios en _user para created_by.';
    END IF;

    INSERT INTO public.maritim_activity_report (
        country_id, state_id, city_id,
        business_category_id, subcategory_code,
        flag, report_start_date, report_end_date,
        created_date, created_by
    ) VALUES
    (v_country_id, v_state_id, v_city_id, v_category_id, 'tour_bahia_diurno', 'GREEN',  v_today, v_today + 7, NOW(), v_user_id),
    (v_country_id, v_state_id, v_city_id, v_category_id, 'paseo_al_cayo',     'YELLOW', v_today, v_today + 3, NOW(), v_user_id),
    (v_country_id, v_state_id, v_city_id, v_category_id, 'snorkeling',        'RED',    v_today, v_today + 1, NOW(), v_user_id);

    RAISE NOTICE 'Insertados 3 reportes DIMAR de ejemplo (country=%, state=%, city=%).',
        v_country_id, v_state_id, v_city_id;
END $$;

SELECT id, country_id, state_id, city_id, subcategory_code, flag,
       report_start_date, report_end_date
FROM public.maritim_activity_report
ORDER BY id;
