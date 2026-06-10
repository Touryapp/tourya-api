-- Migration: Traducciones completas (es/en/pt) para filtros de búsqueda
-- Date: 2026-06-09
-- Complementa 055 con textos del documento "Filtros tours"

-- =============================================================================
-- CATEGORÍAS
-- =============================================================================
UPDATE public.tour_business_category SET name = '{"es":"Acuático","en":"Water Activities","pt":"Aquático"}'::jsonb WHERE code = 'ACUATICO';
UPDATE public.tour_business_category SET name = '{"es":"Deportes","en":"Sports & Diving","pt":"Esportes"}'::jsonb WHERE code = 'DEPORTES';
UPDATE public.tour_business_category SET name = '{"es":"Terrestre","en":"Land Tours","pt":"Terrestre"}'::jsonb WHERE code = 'TERRESTRE';
UPDATE public.tour_business_category SET name = '{"es":"Aventura","en":"Adventure","pt":"Aventura"}'::jsonb WHERE code = 'AVENTURA';
UPDATE public.tour_business_category SET name = '{"es":"Nocturno","en":"Nightlife","pt":"Noturno / Vida Noturna"}'::jsonb WHERE code = 'NOCTURNO';
UPDATE public.tour_business_category SET name = '{"es":"Cultural / Experiencias","en":"Culture & Experiences","pt":"Cultura e Experiências"}'::jsonb WHERE code = 'CULTURAL_EXPERIENCIAS';
UPDATE public.tour_business_category SET name = '{"es":"Alquiler de Transporte","en":"Transport Rental","pt":"Aluguel de Transporte"}'::jsonb WHERE code = 'ALQUILER_TRANSPORTE';

-- =============================================================================
-- SUBCATEGORÍAS
-- =============================================================================
ALTER TABLE public.tour_business_subcategory_mapping
    ADD COLUMN IF NOT EXISTS name jsonb NULL;

UPDATE public.tour_business_subcategory_mapping
SET name = jsonb_build_object('es', display_name, 'en', '', 'pt', '')
WHERE name IS NULL;

UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Paseo al cayo","en":"Cay / Islet Tour","pt":"Passeio ao Ilhéu (Cayo)"}'::jsonb WHERE subcategory_code = 'paseo_al_cayo';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Buceo","en":"Scuba Diving","pt":"Mergulho Cilindro"}'::jsonb WHERE subcategory_code = 'buceo';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Snuba","en":"Snuba Diving","pt":"Snuba"}'::jsonb WHERE subcategory_code = 'snuba';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Windsurf","en":"Windsurfing","pt":"Windsurf"}'::jsonb WHERE subcategory_code = 'windsurf';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Esquí acuático","en":"Water Skiing","pt":"Esqui Aquático"}'::jsonb WHERE subcategory_code = 'esqui_acuatico';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Wakeboard","en":"Wakeboarding","pt":"Wakeboard"}'::jsonb WHERE subcategory_code = 'wakeboard';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Fly board","en":"Flyboarding","pt":"Flyboard"}'::jsonb WHERE subcategory_code = 'fly_board';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Fiesta noche blanca","en":"White Night Party Cruise","pt":"Festa Noite Branca"}'::jsonb WHERE subcategory_code = 'fiesta_noche_blanca';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Parasail","en":"Parasailing","pt":"Parasail"}'::jsonb WHERE subcategory_code = 'parasail';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Paddle board","en":"Paddleboarding (SUP)","pt":"Stand Up Paddle"}'::jsonb WHERE subcategory_code = 'paddle_board';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Moto","en":"Motorbike / Scooter Rental","pt":"Aluguel de Moto / Scooter"}'::jsonb WHERE subcategory_code = 'moto';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Bicicleta","en":"Bicycle Rental","pt":"Aluguel de Bicicleta"}'::jsonb WHERE subcategory_code = 'bicicleta';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Carro playero","en":"Golf Cart / Buggy Rental","pt":"Aluguel de Carrinho de Golfe / Buggy"}'::jsonb WHERE subcategory_code = 'carro_playero';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Tour bahía diurno","en":"Daytime Bay Cruise","pt":"Tour Diurno pela Baía"}'::jsonb WHERE subcategory_code = 'tour_bahia_diurno';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Vuelta a la isla (City Tour)","en":"Island Tour (City Tour)","pt":"Volta à Ilha (City Tour)"}'::jsonb WHERE subcategory_code = 'vuelta_a_la_isla_city_tour';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Snorkeling","en":"Snorkeling","pt":"Snorkeling / Mergulho de Superfície"}'::jsonb WHERE subcategory_code = 'snorkeling';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Semi submarino","en":"Semi-Submarine Tour","pt":"Semi-Submarino"}'::jsonb WHERE subcategory_code = 'semi_submarino';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Pontón","en":"Pontoon Boat Charter","pt":"Passeio de Ponton"}'::jsonb WHERE subcategory_code = 'ponton';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Bar en el agua","en":"Floating Bar Experience","pt":"Bar Flutuante"}'::jsonb WHERE subcategory_code = 'bar_en_el_agua';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Yate de lujo","en":"Luxury Yacht Charter","pt":"Iate de Luxo"}'::jsonb WHERE subcategory_code = 'yate_de_lujo';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Jet Ski","en":"Jet Ski Rental","pt":"Jet Ski"}'::jsonb WHERE subcategory_code = 'jet_ski';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Aquanautas","en":"Helmet Diving (Aquanauts)","pt":"Caminhada Subaquática (Aquanautas)"}'::jsonb WHERE subcategory_code = 'aquanautas';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Kayak","en":"Kayak","pt":"Caiaque"}'::jsonb WHERE subcategory_code = 'kayak';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Cocina local","en":"Local Cuisine Experience","pt":"Culinária Local"}'::jsonb WHERE subcategory_code = 'cocina_local';
UPDATE public.tour_business_subcategory_mapping SET name = '{"es":"Picnic","en":"Picnic Experience","pt":"Piquenique"}'::jsonb WHERE subcategory_code = 'picnic';

-- =============================================================================
-- ETIQUETAS (TAGS)
-- =============================================================================
UPDATE public.tags SET name = '{"es":"Familiar","en":"Family-Friendly","pt":"Familiar / Para Famílias"}'::jsonb WHERE slug = 'familiar';
UPDATE public.tags SET name = '{"es":"Solo Adultos","en":"Adults Only","pt":"Apenas Adultos"}'::jsonb WHERE slug = 'solo-adultos';
UPDATE public.tags SET name = '{"es":"Romántico / Parejas","en":"Romantic / Couples","pt":"Romântico / Casais"}'::jsonb WHERE slug = 'romantico-parejas';
UPDATE public.tags SET name = '{"es":"Pet Friendly","en":"Pet Friendly","pt":"Pet Friendly / Aceita Pets"}'::jsonb WHERE slug = 'pet-friendly';
UPDATE public.tags SET name = '{"es":"Ideal Niños / Bebés","en":"Great for Kids & Babies","pt":"Ideal para Crianças e Bebês"}'::jsonb WHERE slug = 'ideal-ninos-bebes';
UPDATE public.tags SET name = '{"es":"Adulto Mayor","en":"Senior-Friendly","pt":"Adequado para Idosos"}'::jsonb WHERE slug = 'adulto-mayor';
UPDATE public.tags SET name = '{"es":"Aguas Cristalinas","en":"Crystal Clear Waters","pt":"Águas Cristalinas"}'::jsonb WHERE slug = 'aguas-cristalinas';
UPDATE public.tags SET name = '{"es":"Instagrammable","en":"Instagrammable / Scenic","pt":"Instagramável / Fotos Lindas"}'::jsonb WHERE slug = 'instagrammable';
UPDATE public.tags SET name = '{"es":"Adrenalina","en":"Adrenaline / Thrill","pt":"Adrenalina / Pura Emoção"}'::jsonb WHERE slug = 'adrenalina';
UPDATE public.tags SET name = '{"es":"Relax / Chill Out","en":"Relax / Chill Out","pt":"Relaxar / Calmo"}'::jsonb WHERE slug = 'relax-chill-out';
UPDATE public.tags SET name = '{"es":"Vida Marina","en":"Marine Life / Wildlife","pt":"Vida Marinha"}'::jsonb WHERE slug = 'vida-marina';
UPDATE public.tags SET name = '{"es":"Naturaleza / Eco","en":"Nature / Eco-Tour","pt":"Natureza / Eco-Tour"}'::jsonb WHERE slug = 'naturaleza-eco';
UPDATE public.tags SET name = '{"es":"Cultura Raizal","en":"Local Culture & Heritage","pt":"Cultura Local / Raizal"}'::jsonb WHERE slug = 'cultura-raizal';
UPDATE public.tags SET name = '{"es":"No requiere saber nadar","en":"No Swimming Skills Required","pt":"Não Precisa Saber Nadar"}'::jsonb WHERE slug = 'no-requiere-saber-nadar';
UPDATE public.tags SET name = '{"es":"Aventura Extrema","en":"Extreme Adventure","pt":"Aventura Extrema"}'::jsonb WHERE slug = 'aventura-extrema';
UPDATE public.tags SET name = '{"es":"Apto para principiantes","en":"Beginner-Friendly","pt":"Ideal para Iniciantes"}'::jsonb WHERE slug = 'apto-para-principiantes';
UPDATE public.tags SET name = '{"es":"Certificación requerida","en":"Certification Required","pt":"Certificação Necessária"}'::jsonb WHERE slug = 'certificacion-requerida';
UPDATE public.tags SET name = '{"es":"Reserva Instantánea","en":"Instant Booking","pt":"Reserva Instantânea"}'::jsonb WHERE slug = 'reserva-instantanea';
UPDATE public.tags SET name = '{"es":"Incluye Almuerzo","en":"Lunch Included","pt":"Almoço Incluso"}'::jsonb WHERE slug = 'incluye-almuerzo';
UPDATE public.tags SET name = '{"es":"Barra Libre / Open Bar","en":"Open Bar","pt":"Open Bar / Bar Aberto"}'::jsonb WHERE slug = 'barra-libre-open-bar';
UPDATE public.tags SET name = '{"es":"Recogida en el Hotel","en":"Hotel Pickup","pt":"Trânsfer do Hotel / Busco no Hotel"}'::jsonb WHERE slug = 'recogida-en-el-hotel';
UPDATE public.tags SET name = '{"es":"Guía Bilingüe","en":"Bilingual Guide","pt":"Guia Bilíngue"}'::jsonb WHERE slug = 'guia-bilingue';
UPDATE public.tags SET name = '{"es":"Sombra a Bordo","en":"Shaded Boat / Shade Available","pt":"Sombra a Bordo"}'::jsonb WHERE slug = 'sombra-a-bordo';
UPDATE public.tags SET name = '{"es":"Equipamiento Incluido","en":"Gear Included","pt":"Equipamento Incluso"}'::jsonb WHERE slug = 'equipamiento-incluido';
UPDATE public.tags SET name = '{"es":"Sector North End","en":"North End Area (Downtown)","pt":"Setor North End (Centro)"}'::jsonb WHERE slug = 'sector-north-end';
UPDATE public.tags SET name = '{"es":"Sector San Luis","en":"San Luis Area","pt":"Setor San Luis"}'::jsonb WHERE slug = 'sector-san-luis';
UPDATE public.tags SET name = '{"es":"West View / Piscinita","en":"West View / La Piscinita","pt":"West View / La Piscinita"}'::jsonb WHERE slug = 'west-view-piscinita';
UPDATE public.tags SET name = '{"es":"Express (1-2 horas)","en":"Express Tour (1-2 hours)","pt":"Tour Expresso (1-2 horas)"}'::jsonb WHERE slug = 'express-1-2-horas';
UPDATE public.tags SET name = '{"es":"Medio Día","en":"Half-Day Tour","pt":"Meio Dia"}'::jsonb WHERE slug = 'medio-dia';
UPDATE public.tags SET name = '{"es":"Día Completo","en":"Full-Day Tour","pt":"Dia Inteiro"}'::jsonb WHERE slug = 'dia-completo';
UPDATE public.tags SET name = '{"es":"Atardecer / Sunset","en":"Sunset Tour","pt":"Pôr do Sol / Sunset"}'::jsonb WHERE slug = 'atardecer-sunset';
