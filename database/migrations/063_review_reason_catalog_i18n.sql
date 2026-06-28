-- Migration: Catálogo de motivos de reseña en 3 idiomas (es/en/pt)
-- Date: 2026-06-25
-- Endpoint: GET /api/v1/public/review/reasons

CREATE TABLE IF NOT EXISTS public.review_reason_catalog (
    id          SERIAL PRIMARY KEY,
    reason_type VARCHAR(20) NOT NULL,
    reason_id   INTEGER     NOT NULL,
    label       jsonb       NOT NULL,
    CONSTRAINT uq_review_reason_catalog_type_id UNIQUE (reason_type, reason_id)
);

COMMENT ON TABLE public.review_reason_catalog IS 'Motivos de reseña positivos/negativos con traducciones es/en/pt';

-- POSITIVE (reason_id 1..7)
INSERT INTO public.review_reason_catalog (reason_type, reason_id, label) VALUES
('POSITIVE', 1, '{"es":"Servicio excepcional del guía","en":"Exceptional service from the guide","pt":"Serviço excepcional do guia"}'),
('POSITIVE', 2, '{"es":"Puntualidad del proveedor","en":"Provider''s punctuality","pt":"Pontualidade do prestador de serviços"}'),
('POSITIVE', 3, '{"es":"Buena organización del tour","en":"Good tour organization","pt":"Boa organização do passeio"}'),
('POSITIVE', 4, '{"es":"Excelente relación calidad-precio","en":"Excellent value for money","pt":"Excelente custo-benefício"}'),
('POSITIVE', 5, '{"es":"Comodidad del transporte","en":"Comfort of transportation","pt":"Conforto do transporte"}'),
('POSITIVE', 6, '{"es":"Buena atención al cliente","en":"Good customer service","pt":"Bom atendimento ao cliente"}'),
('POSITIVE', 7, '{"es":"Recomendable para otros viajeros","en":"Would recommend to other travelers","pt":"Recomendável para outros viajantes"}')
ON CONFLICT (reason_type, reason_id) DO UPDATE SET label = EXCLUDED.label;

-- NEGATIVE (reason_id 1..7)
INSERT INTO public.review_reason_catalog (reason_type, reason_id, label) VALUES
('NEGATIVE', 1, '{"es":"Retraso o impuntualidad","en":"Delays or lack of punctuality","pt":"Atraso ou falta de pontualidade"}'),
('NEGATIVE', 2, '{"es":"Guía poco amable o desinformado","en":"Unfriendly or uninformed guide","pt":"Guia pouco simpático ou mal informado"}'),
('NEGATIVE', 3, '{"es":"Mala comunicación con el operador turístico","en":"Poor communication with the tour operator","pt":"Má comunicação com a operadora de turismo"}'),
('NEGATIVE', 4, '{"es":"El tour no correspondía a la descripción","en":"Tour did not match the description","pt":"O passeio não correspondia à descrição"}'),
('NEGATIVE', 5, '{"es":"Problemas con el transporte","en":"Problems with transportation","pt":"Problemas com o transporte"}'),
('NEGATIVE', 6, '{"es":"Mala relación calidad-precio","en":"Poor value for money","pt":"Má relação custo-benefício"}'),
('NEGATIVE', 7, '{"es":"No cumplieron con lo que incluye el tour","en":"Did not deliver what the tour included","pt":"Não cumpriram o que estava incluído no passeio"}')
ON CONFLICT (reason_type, reason_id) DO UPDATE SET label = EXCLUDED.label;
