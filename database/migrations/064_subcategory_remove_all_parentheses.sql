-- Migration: Eliminar TODOS los paréntesis en subcategorías (name jsonb + display_name)
-- Date: 2026-06-27
-- Corrige 062 que dejó "(City Tour)" en ES y no actualizó display_name

-- =============================================================================
-- Correcciones explícitas (tabla Luis / doc front)
-- =============================================================================

UPDATE public.tour_business_subcategory_mapping
SET
    display_name = 'Vuelta a la isla',
    name = '{"es":"Vuelta a la isla","en":"City Tour","pt":"Volta à Ilha"}'::jsonb
WHERE subcategory_code = 'vuelta_a_la_isla_city_tour';

UPDATE public.tour_business_subcategory_mapping
SET
    name = '{"es":"Aquanautas","en":"Helmet Diving","pt":"Caminhada Subaquática"}'::jsonb
WHERE subcategory_code = 'aquanautas';

UPDATE public.tour_business_subcategory_mapping
SET
    name = '{"es":"Paseo al cayo","en":"Cay / Islet Tour","pt":"Passeio ao Ilhéu"}'::jsonb
WHERE subcategory_code = 'paseo_al_cayo';

UPDATE public.tour_business_subcategory_mapping
SET
    name = '{"es":"Snorkeling","en":"Snorkeling","pt":"Snorkeling"}'::jsonb
WHERE subcategory_code = 'snorkeling';

UPDATE public.tour_business_subcategory_mapping
SET
    name = '{"es":"Paddle board","en":"Paddleboarding","pt":"Stand Up Paddle"}'::jsonb
WHERE subcategory_code = 'paddle_board';

UPDATE public.tour_business_subcategory_mapping
SET
    name = '{"es":"Moto","en":"Motorbike Rental","pt":"Aluguel de Moto"}'::jsonb
WHERE subcategory_code = 'moto';

UPDATE public.tour_business_subcategory_mapping
SET
    name = '{"es":"Carro playero","en":"Golf Cart Rental","pt":"Aluguel de Carrinho de Golfe"}'::jsonb
WHERE subcategory_code = 'carro_playero';

-- =============================================================================
-- Limpieza general: quitar " (texto)" en cualquier subcategoría restante
-- =============================================================================

UPDATE public.tour_business_subcategory_mapping
SET
    display_name = trim(regexp_replace(display_name, ' \([^)]*\)', '', 'g')),
    name = jsonb_build_object(
        'es', trim(regexp_replace(COALESCE(name->>'es', ''), ' \([^)]*\)', '', 'g')),
        'en', trim(regexp_replace(COALESCE(name->>'en', ''), ' \([^)]*\)', '', 'g')),
        'pt', trim(regexp_replace(COALESCE(name->>'pt', ''), ' \([^)]*\)', '', 'g'))
    )
WHERE display_name ~ '\([^)]*\)'
   OR COALESCE(name->>'es', '') ~ '\([^)]*\)'
   OR COALESCE(name->>'en', '') ~ '\([^)]*\)'
   OR COALESCE(name->>'pt', '') ~ '\([^)]*\)';
