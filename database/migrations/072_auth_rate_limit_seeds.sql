-- Migration 072: Seeds default para rate limiting en /auth/** (SEC-09)
-- Date: 2026-07-09
-- Description:
--   Inserta 2 configuraciones que gobiernan el rate limiter aplicado en
--   AuthRateLimitFilter (todas las rutas bajo /auth/**):
--     - AUTH_RATE_LIMIT_ENABLED: feature flag. 0 = OFF (default), 1 = ON.
--     - AUTH_RATE_LIMIT_PER_MINUTE: requests permitidos por IP por minuto (default 60).
--
--   El default OFF evita bloquear QA in-flight. Se activa sin re-deploy con:
--     PUT /api/v1/config/AUTH_RATE_LIMIT_ENABLED {"value":{"value":1}}
--
--   Umbral configurable si Luis define un valor oficial diferente:
--     PUT /api/v1/config/AUTH_RATE_LIMIT_PER_MINUTE {"value":{"value":120}}
--
--   Nota sobre coordinacion entre instancias: el contador es in-memory por instancia
--   de Cloud Run. Con max-instances=2, el limite efectivo puede ser hasta 2x. Aceptable
--   como defensa en profundidad; migrar a Redis si se necesita coordinacion estricta.
--
--   Idempotente: ON CONFLICT DO NOTHING.

INSERT INTO public.app_config (config_key, config_value, description, created_date, created_by)
VALUES
    ('AUTH_RATE_LIMIT_ENABLED',
     '{"value": 0}'::jsonb,
     'Feature flag: 1 activa el rate limiter en /auth/**. Default 0 para no bloquear QA in-flight.',
     now(), 1),
    ('AUTH_RATE_LIMIT_PER_MINUTE',
     '{"value": 60}'::jsonb,
     'Cuenta maxima de requests por IP por minuto en /auth/**. Cuando se excede se devuelve 429.',
     now(), 1)
ON CONFLICT (config_key) DO NOTHING;
