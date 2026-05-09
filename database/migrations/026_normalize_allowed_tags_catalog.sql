-- Migration: Normalize allowed tags catalog with dimensions
-- Date: 2026-04-01

CREATE TABLE IF NOT EXISTS public.tag_dimensions (
    id serial4 PRIMARY KEY,
    nombre varchar(80) NOT NULL UNIQUE,
    slug varchar(100) NOT NULL UNIQUE,
    display_order int4 NOT NULL UNIQUE
);

ALTER TABLE public.tags
    ADD COLUMN IF NOT EXISTS dimension_id int4 NULL REFERENCES public.tag_dimensions(id);

DELETE FROM public.tour_tags;
DELETE FROM public.tags;

INSERT INTO public.tag_dimensions (nombre, slug, display_order)
VALUES
    ('Audiencia', 'audiencia', 1),
    ('Experiencia', 'experiencia', 2),
    ('Habilidad', 'habilidad', 3),
    ('Logística', 'logistica', 4),
    ('Ubicación', 'ubicacion', 5),
    ('Duración', 'duracion', 6)
ON CONFLICT (nombre) DO UPDATE
SET slug = EXCLUDED.slug,
    display_order = EXCLUDED.display_order;

INSERT INTO public.tags (nombre, slug, dimension_id)
SELECT v.nombre, v.slug, d.id
FROM (
    VALUES
        ('Familiar', 'familiar', 'Audiencia'),
        ('Solo Adultos', 'solo-adultos', 'Audiencia'),
        ('Romántico / Parejas', 'romantico-parejas', 'Audiencia'),
        ('Pet Friendly', 'pet-friendly', 'Audiencia'),
        ('Ideal Niños / Bebés', 'ideal-ninos-bebes', 'Audiencia'),
        ('Adulto Mayor', 'adulto-mayor', 'Audiencia'),

        ('Aguas Cristalinas', 'aguas-cristalinas', 'Experiencia'),
        ('Instagrammable', 'instagrammable', 'Experiencia'),
        ('Adrenalina', 'adrenalina', 'Experiencia'),
        ('Relax / Chill Out', 'relax-chill-out', 'Experiencia'),
        ('Vida Marina', 'vida-marina', 'Experiencia'),
        ('Naturaleza / Eco', 'naturaleza-eco', 'Experiencia'),
        ('Cultura Raizal', 'cultura-raizal', 'Experiencia'),

        ('No requiere saber nadar', 'no-requiere-saber-nadar', 'Habilidad'),
        ('Aventura Extrema', 'aventura-extrema', 'Habilidad'),
        ('Apto para principiantes', 'apto-para-principiantes', 'Habilidad'),
        ('Certificación requerida', 'certificacion-requerida', 'Habilidad'),

        ('Reserva Instantánea', 'reserva-instantanea', 'Logística'),
        ('Incluye Almuerzo', 'incluye-almuerzo', 'Logística'),
        ('Barra Libre / Open Bar', 'barra-libre-open-bar', 'Logística'),
        ('Recogida en el Hotel', 'recogida-en-el-hotel', 'Logística'),
        ('Guía Bilingüe', 'guia-bilingue', 'Logística'),
        ('Sombra a Bordo', 'sombra-a-bordo', 'Logística'),
        ('Equipamiento Incluido', 'equipamiento-incluido', 'Logística'),

        ('Sector North End', 'sector-north-end', 'Ubicación'),
        ('Sector San Luis', 'sector-san-luis', 'Ubicación'),
        ('West View / Piscinita', 'west-view-piscinita', 'Ubicación'),

        ('Express (1-2 horas)', 'express-1-2-horas', 'Duración'),
        ('Medio Día', 'medio-dia', 'Duración'),
        ('Día Completo', 'dia-completo', 'Duración'),
        ('Atardecer / Sunset', 'atardecer-sunset', 'Duración')
) AS v(nombre, slug, dimension_nombre)
JOIN public.tag_dimensions d ON d.nombre = v.dimension_nombre;
