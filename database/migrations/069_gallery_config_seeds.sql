-- Migration 069: Seeds default en app_config para BE-10 (validador galeria)
-- Date: 2026-07-09
-- Description:
--   Inserta 3 configuraciones que gobiernan la validacion de imagenes en
--   POST /tours/{tourId}/gallery/sync y /syncWithUpdate:
--     - GALLERY_MAX_SIZE_MB: tamano maximo por imagen en MB (default 5)
--     - GALLERY_MIN_WIDTH_PX: ancho minimo en pixeles para calidad aceptable (default 800)
--     - GALLERY_MAX_IMAGES_PER_TOUR: cuenta maxima de imagenes por tour (default 7)
--
--   Valores propuestos por Luis (RN-013). Al estar en app_config, ADMIN los
--   ajusta sin re-deploy si cambia la politica oficial.
--
--   Idempotente: ON CONFLICT DO NOTHING para poder re-ejecutar en otros ambientes.

INSERT INTO public.app_config (config_key, config_value, description, created_date, created_by)
VALUES
    ('GALLERY_MAX_SIZE_MB',
     '{"value": 5}'::jsonb,
     'Tamano maximo en MB por imagen en la galeria de un tour. Rechazado si excede.',
     now(), 1),
    ('GALLERY_MIN_WIDTH_PX',
     '{"value": 800}'::jsonb,
     'Ancho minimo en pixeles para asegurar calidad aceptable. Rechazado si menor.',
     now(), 1),
    ('GALLERY_MAX_IMAGES_PER_TOUR',
     '{"value": 7}'::jsonb,
     'Cuenta maxima de imagenes por tour. El sync se rechaza si el resultado excede este limite.',
     now(), 1)
ON CONFLICT (config_key) DO NOTHING;
