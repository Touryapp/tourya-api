-- Migration: Add business categories and subcategory mappings for tour search
-- Date: 2026-04-01

CREATE TABLE IF NOT EXISTS public.tour_business_category (
    id serial4 PRIMARY KEY,
    code varchar(60) NOT NULL UNIQUE,
    name varchar(120) NOT NULL UNIQUE,
    display_order int4 NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS public.tour_business_subcategory_mapping (
    subcategory_code varchar(120) PRIMARY KEY,
    display_name varchar(150) NOT NULL,
    business_category_id int4 NOT NULL REFERENCES public.tour_business_category(id)
);

INSERT INTO public.tour_business_category (code, name, display_order)
VALUES
    ('ACUATICO', 'Acuático', 1),
    ('DEPORTES', 'Deportes', 2),
    ('TERRESTRE', 'Terrestre', 3),
    ('AVENTURA', 'Aventura', 4),
    ('NOCTURNO', 'Nocturno', 5),
    ('CULTURAL_EXPERIENCIAS', 'Cultural / Experiencias', 6),
    ('ALQUILER_TRANSPORTE', 'Alquiler de Transporte', 7)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    display_order = EXCLUDED.display_order;

INSERT INTO public.tour_business_subcategory_mapping (subcategory_code, display_name, business_category_id)
SELECT v.subcategory_code, v.display_name, c.id
FROM (
    VALUES
        ('paseo_al_cayo', 'Paseo al cayo', 'ACUATICO'),
        ('tour_bahia_diurno', 'Tour bahía diurno', 'ACUATICO'),
        ('ponton', 'Pontón', 'ACUATICO'),
        ('yate_de_lujo', 'Yate de lujo', 'ACUATICO'),
        ('bar_en_el_agua', 'Bar en el agua', 'ACUATICO'),
        ('semi_submarino', 'Semi submarino', 'ACUATICO'),
        ('snorkeling', 'Snorkeling', 'ACUATICO'),
        ('buceo', 'Buceo', 'DEPORTES'),
        ('snuba', 'Snuba', 'DEPORTES'),
        ('windsurf', 'Windsurf', 'DEPORTES'),
        ('esqui_acuatico', 'Esquí acuático', 'DEPORTES'),
        ('wakeboard', 'Wakeboard', 'DEPORTES'),
        ('fly_board', 'Fly board', 'DEPORTES'),
        ('kayak', 'Kayak', 'DEPORTES'),
        ('vuelta_a_la_isla_city_tour', 'Vuelta a la isla (City Tour)', 'TERRESTRE'),
        ('parasail', 'Parasail', 'AVENTURA'),
        ('jet_ski', 'Jet Ski', 'AVENTURA'),
        ('fiesta_noche_blanca', 'Fiesta noche blanca', 'NOCTURNO'),
        ('paddle_board', 'Paddle board', 'CULTURAL_EXPERIENCIAS'),
        ('aquanautas', 'Aquanautas', 'CULTURAL_EXPERIENCIAS'),
        ('picnic', 'Picnic', 'CULTURAL_EXPERIENCIAS'),
        ('cocina_local', 'Cocina local', 'CULTURAL_EXPERIENCIAS'),
        ('moto', 'Moto', 'ALQUILER_TRANSPORTE'),
        ('bicicleta', 'Bicicleta', 'ALQUILER_TRANSPORTE'),
        ('carro_playero', 'Carro playero', 'ALQUILER_TRANSPORTE')
) AS v(subcategory_code, display_name, category_code)
JOIN public.tour_business_category c ON c.code = v.category_code
ON CONFLICT (subcategory_code) DO UPDATE
SET display_name = EXCLUDED.display_name,
    business_category_id = EXCLUDED.business_category_id;
