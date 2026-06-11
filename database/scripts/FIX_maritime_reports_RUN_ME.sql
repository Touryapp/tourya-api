-- =============================================================================
-- SCRIPT ÚNICO — Arreglar GET /maritime-activity-reports (error 500)
-- =============================================================================
-- Qué hace:
--   1. Muestra cuántos reportes están rotos y cuáles son
--   2. Los elimina (no se pueden recuperar: la migración 057 borró country/city texto)
--   3. Deja las columnas obligatorias para que no vuelva a pasar
--
-- Cómo ejecutar (elige una opción):
--
--   psql -h TU_HOST -p 5432 -U TU_USER -d TU_DB -f database/scripts/FIX_maritime_reports_RUN_ME.sql
--
--   O en DBeaver/pgAdmin: abrir este archivo y ejecutar todo.
--
-- Después: volver a crear reportes con POST /maritime-activity-reports
-- =============================================================================

\echo '=== PASO 1: Reportes inválidos (preview) ==='
SELECT
    id,
    country_id,
    state_id,
    city_id,
    business_category_id,
    subcategory_code,
    flag,
    report_start_date,
    report_end_date,
    created_date
FROM public.maritim_activity_report
WHERE country_id IS NULL
   OR state_id IS NULL
   OR city_id IS NULL
   OR business_category_id IS NULL
   OR subcategory_code IS NULL
   OR report_start_date IS NULL
   OR report_end_date IS NULL
ORDER BY id;

\echo '=== PASO 2: Conteo ==='
SELECT COUNT(*) AS total_invalidos
FROM public.maritim_activity_report
WHERE country_id IS NULL
   OR state_id IS NULL
   OR city_id IS NULL
   OR business_category_id IS NULL
   OR subcategory_code IS NULL
   OR report_start_date IS NULL
   OR report_end_date IS NULL;

\echo '=== PASO 3: Eliminar inválidos ==='
DELETE FROM public.maritim_activity_report
WHERE country_id IS NULL
   OR state_id IS NULL
   OR city_id IS NULL
   OR business_category_id IS NULL
   OR subcategory_code IS NULL
   OR report_start_date IS NULL
   OR report_end_date IS NULL;

\echo '=== PASO 4: NOT NULL en columnas requeridas ==='
ALTER TABLE public.maritim_activity_report
    ALTER COLUMN country_id SET NOT NULL,
    ALTER COLUMN state_id SET NOT NULL,
    ALTER COLUMN city_id SET NOT NULL,
    ALTER COLUMN business_category_id SET NOT NULL,
    ALTER COLUMN subcategory_code SET NOT NULL,
    ALTER COLUMN report_start_date SET NOT NULL,
    ALTER COLUMN report_end_date SET NOT NULL;

\echo '=== PASO 5: Verificación final ==='
SELECT COUNT(*) AS restantes_invalidos
FROM public.maritim_activity_report
WHERE country_id IS NULL
   OR state_id IS NULL
   OR city_id IS NULL
   OR business_category_id IS NULL
   OR subcategory_code IS NULL
   OR report_start_date IS NULL
   OR report_end_date IS NULL;

SELECT COUNT(*) AS total_reportes_ok FROM public.maritim_activity_report;

\echo '=== LISTO. Probar: GET /api/v1/maritime-activity-reports?page=0&size=10 ==='
