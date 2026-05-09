-- Migration: Map legacy tour tags to normalized allowed catalog
-- Date: 2026-04-01

-- Repoblar la relación tour_tags a partir del catálogo legacy (tour_tag / tour_tag_mapping)
-- usando reglas de equivalencia hacia el nuevo catálogo permitido.

DELETE FROM public.tour_tags;

WITH legacy_map AS (
    SELECT DISTINCT
        ttm.tour_id,
        unnest(
            CASE
                -- Audiencia
                WHEN tt.name IN ('Tour para parejas', 'Cena romántica', 'Propuestas de matrimonio', 'Atardecer en velero', 'Cena en la playa')
                    THEN ARRAY['romantico-parejas']
                WHEN tt.name IN ('Actividades infantiles', 'Animales marinos', 'Seguro para niños', 'Tour educativo')
                    THEN ARRAY['familiar', 'ideal-ninos-bebes']
                WHEN tt.name IN ('Fiesta blanca', 'Fiesta en yate', 'Tour nocturno con linternas')
                    THEN ARRAY['solo-adultos']
                WHEN tt.name IN ('Mulitas', 'Carro playero')
                    THEN ARRAY['pet-friendly']
                WHEN tt.name IN ('Acuario', 'Tour de día completo', 'Paseo en catamarán', 'Stand up paddle', 'Día de playa', 'Hamacas', 'Tumbonas')
                    THEN ARRAY['familiar', 'adulto-mayor']

                -- Experiencia
                WHEN tt.name IN ('Johnny Cay', 'Haynes Cay', 'Acuario', 'Cayos (Johnny Cay, Acuario, Haynes Cay)', 'Atardecer en velero', 'Atardecer')
                    THEN ARRAY['aguas-cristalinas']
                WHEN tt.name IN ('Buceo', 'Snorkel', 'Submarinismo', 'Mantarrayas', 'Nadar con mantarrayas', 'Fauna marina', 'Animales marinos')
                    THEN ARRAY['vida-marina']
                WHEN tt.name IN ('Manglares', 'Eco-tour', 'Tour ecológico', 'Senderismo')
                    THEN ARRAY['naturaleza-eco']
                WHEN tt.name IN ('Raizales y cultura local', 'Historia de la isla', 'Música y baile tradicional', 'Gastronomía típica')
                    THEN ARRAY['cultura-raizal']
                WHEN tt.name IN ('Parasailing', 'Motos acuáticas', 'Cuatrimoto', 'Caminatas extremas')
                    THEN ARRAY['adrenalina']
                WHEN tt.name IN ('Hamacas', 'Tumbonas', 'Cócteles', 'Día de playa', 'Atardecer', 'Stand up paddle')
                    THEN ARRAY['relax-chill-out']

                -- Habilidad
                WHEN tt.name IN ('Mantarrayas', 'Nadar con mantarrayas', 'Submarinismo', 'Buceo', 'Snorkel', 'Kayak', 'Stand up paddle')
                    THEN ARRAY['apto-para-principiantes']
                WHEN tt.name IN ('Motos acuáticas', 'Parasailing', 'Cuatrimoto', 'Caminatas extremas')
                    THEN ARRAY['aventura-extrema']
                WHEN tt.name IN ('Acuario', 'Semi submarino', 'Nadar con mantarrayas')
                    THEN ARRAY['no-requiere-saber-nadar']
                WHEN tt.name IN ('Buceo', 'Submarinismo')
                    THEN ARRAY['certificacion-requerida']

                -- Logística
                WHEN tt.name = 'Incluye almuerzo'
                    THEN ARRAY['incluye-almuerzo']
                WHEN tt.name = 'Guía bilingüe'
                    THEN ARRAY['guia-bilingue']
                WHEN tt.name = 'Recolección en hotel'
                    THEN ARRAY['recogida-en-el-hotel']
                WHEN tt.name IN ('Todo incluido', 'Tour privado')
                    THEN ARRAY['equipamiento-incluido']
                WHEN tt.name IN ('Paseo en catamarán', 'Tour en lancha', 'Fiesta en yate', 'Atardecer en velero')
                    THEN ARRAY['sombra-a-bordo']
                WHEN tt.name IN ('Fiesta blanca', 'Fiesta en yate', 'Cócteles', 'Degustación de ron o cócteles')
                    THEN ARRAY['barra-libre-open-bar']

                -- Ubicación
                WHEN tt.name IN ('Johnny Cay', 'Haynes Cay', 'Acuario', 'Cayos (Johnny Cay, Acuario, Haynes Cay)')
                    THEN ARRAY['west-view-piscinita']

                -- Duración
                WHEN tt.name = 'Medio día'
                    THEN ARRAY['medio-dia']
                WHEN tt.name = 'Tour de día completo'
                    THEN ARRAY['dia-completo']
                WHEN tt.name IN ('Atardecer', 'Tour al atardecer', 'Atardecer en velero')
                    THEN ARRAY['atardecer-sunset']
                WHEN tt.name IN ('Parasailing', 'Motos acuáticas')
                    THEN ARRAY['express-1-2-horas']

                ELSE ARRAY[]::text[]
            END
        ) AS new_slug
    FROM public.tour_tag_mapping ttm
    JOIN public.tour_tag tt ON tt.id = ttm.tag_id
),
extra_map AS (
    -- Reglas adicionales por contenido del tour para no depender solo del legacy tag.
    SELECT DISTINCT
        t.id AS tour_id,
        unnest(
            CASE
                WHEN lower(coalesce(t.name->>'es', '')) LIKE '%bahia%' THEN ARRAY['aguas-cristalinas']
                ELSE ARRAY[]::text[]
            END
        ) AS new_slug
    FROM public.tour t
)
INSERT INTO public.tour_tags (tour_id, tag_id)
SELECT DISTINCT
    src.tour_id,
    tg.id
FROM (
    SELECT * FROM legacy_map
    UNION
    SELECT * FROM extra_map
) src
JOIN public.tags tg ON tg.slug = src.new_slug
WHERE src.new_slug IS NOT NULL
  AND src.new_slug <> ''
ON CONFLICT (tour_id, tag_id) DO NOTHING;
