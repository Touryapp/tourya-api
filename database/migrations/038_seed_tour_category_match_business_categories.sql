-- Migration 038: Poblar tour_category para que coincida con catálogo "business"
-- Motivo:
-- - El API usa tour_category (FK tour.category_id) y devuelve categoryName desde esa tabla (tc.name)
-- - El catálogo real existente está en public.tour_business_category (migración 024)
-- - Esta migración alinea tour_category con tour_business_category, usando los mismos IDs.
-- Date: 2026-05-09

-- Algunos entornos tienen name/description como varchar(30); tour_business_category.name es varchar(120).
-- Sin ampliar columnas, el UPSERT falla al asignar EXCLUDED.name o una descripción larga.

ALTER TABLE public.tour_category
  ALTER COLUMN "name" TYPE varchar(120) USING LEFT("name"::text, 120);

ALTER TABLE public.tour_category
  ALTER COLUMN description TYPE varchar(255) USING LEFT(description::text, 255);

-- Auditoría: tour_category hereda NOT NULL en created_by (JPA BaseEntity).
-- Usamos el primer usuario existente, o 1 si la tabla está vacía (mismo criterio que seeds antiguos).

-- 1) Upsert: crear/actualizar categorías con ids/nombres reales
INSERT INTO public.tour_category (id, name, description, created_date, created_by)
SELECT
  bc.id,
  bc.name,
  LEFT(CONCAT(bc.code, ' — ', bc.name), 255),
  CURRENT_TIMESTAMP,
  COALESCE((SELECT MIN(u.id) FROM public._user u), 1)
FROM public.tour_business_category bc
ON CONFLICT (id) DO UPDATE
SET
  name = EXCLUDED.name,
  description = EXCLUDED.description;

-- 2) Si existía data dummy en tour_category con nombres que ya existen, mantener el id "real"
-- (tour_category.name es UNIQUE; este bloque evita errores si ya existía por name).
UPDATE public.tour_category tc
SET
  description = LEFT(CONCAT(bc.code, ' — ', bc.name), 255)
FROM public.tour_business_category bc
WHERE tc.name = bc.name;

-- 3) Ajustar secuencia del serial para futuros inserts
SELECT setval(
  pg_get_serial_sequence('public.tour_category', 'id'),
  COALESCE((SELECT MAX(id) FROM public.tour_category), 1),
  true
);

