-- Migration: Add simplified tags catalog and tour_tags relation
-- Date: 2026-04-01

CREATE TABLE IF NOT EXISTS public.tags (
    id serial4 PRIMARY KEY,
    nombre varchar(150) NOT NULL UNIQUE,
    slug varchar(180) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS public.tour_tags (
    tour_id int4 NOT NULL REFERENCES public.tour(id) ON DELETE CASCADE,
    tag_id int4 NOT NULL REFERENCES public.tags(id) ON DELETE CASCADE,
    PRIMARY KEY (tour_id, tag_id)
);

INSERT INTO public.tags (nombre, slug)
SELECT DISTINCT
    tt.name,
    regexp_replace(
        translate(lower(tt.name), 'áéíóúñ', 'aeioun'),
        '[^a-z0-9]+',
        '-',
        'g'
    ) AS slug
FROM public.tour_tag tt
ON CONFLICT (nombre) DO UPDATE
SET slug = EXCLUDED.slug;

INSERT INTO public.tour_tags (tour_id, tag_id)
SELECT DISTINCT
    ttm.tour_id,
    tg.id
FROM public.tour_tag_mapping ttm
JOIN public.tour_tag tt ON tt.id = ttm.tag_id
JOIN public.tags tg ON tg.nombre = tt.name
ON CONFLICT (tour_id, tag_id) DO NOTHING;
