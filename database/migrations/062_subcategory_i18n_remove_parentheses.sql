-- Migration: Quitar textos entre paréntesis en subcategorías (ajustes front Luis - naranja)
-- Date: 2026-06-25
-- Requiere: 055 y 056 ya ejecutadas (columna tour_business_subcategory_mapping.name jsonb)

-- Paseo al cayo: PT sin "(Cayo)"
UPDATE public.tour_business_subcategory_mapping
SET name = '{"es":"Paseo al cayo","en":"Cay / Islet Tour","pt":"Passeio ao Ilhéu"}'::jsonb
WHERE subcategory_code = 'paseo_al_cayo';

-- Vuelta a la isla: sin paréntesis en ningún idioma; EN solo "City Tour"
UPDATE public.tour_business_subcategory_mapping
SET
    display_name = 'Vuelta a la isla',
    name = '{"es":"Vuelta a la isla","en":"City Tour","pt":"Volta à Ilha"}'::jsonb
WHERE subcategory_code = 'vuelta_a_la_isla_city_tour';

-- Snorkeling: PT solo "Snorkeling"
UPDATE public.tour_business_subcategory_mapping
SET name = '{"es":"Snorkeling","en":"Snorkeling","pt":"Snorkeling"}'::jsonb
WHERE subcategory_code = 'snorkeling';

-- Aquanautas: sin paréntesis ni sufijos "/ Sea Trek"
UPDATE public.tour_business_subcategory_mapping
SET name = '{"es":"Aquanautas","en":"Helmet Diving","pt":"Caminhada Subaquática"}'::jsonb
WHERE subcategory_code = 'aquanautas';

-- Otros enumerados con paréntesis innecesarios en EN/PT
UPDATE public.tour_business_subcategory_mapping
SET name = '{"es":"Paddle board","en":"Paddleboarding","pt":"Stand Up Paddle"}'::jsonb
WHERE subcategory_code = 'paddle_board';

UPDATE public.tour_business_subcategory_mapping
SET name = '{"es":"Moto","en":"Motorbike Rental","pt":"Aluguel de Moto"}'::jsonb
WHERE subcategory_code = 'moto';

UPDATE public.tour_business_subcategory_mapping
SET name = '{"es":"Carro playero","en":"Golf Cart Rental","pt":"Aluguel de Carrinho de Golfe"}'::jsonb
WHERE subcategory_code = 'carro_playero';
