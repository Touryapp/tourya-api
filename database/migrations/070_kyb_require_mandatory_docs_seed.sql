-- Migration 070: Feature flag KYB_REQUIRE_MANDATORY_DOCS (default OFF)
-- Date: 2026-07-09
-- Description:
--   Seed default en app_config para el feature flag que activa la validacion de
--   documentos KYB obligatorios (RN-045) al hacer PUT /requestProvider/user/send.
--
--   value=0 (OFF): comportamiento actual, sin validacion. Se puede hacer submit sin
--     todos los documentos obligatorios (util para QA in-flight).
--   value=1 (ON): se rechaza el submit con HTTP 400 si falta cualquier
--     RequestProviderDocumentType con mandatory=true. Retorna la lista de nombres
--     de docs faltantes en el body para que el frontend guie al usuario.
--
--   Se puede activar sin re-deploy cuando Luis confirme que dev esta listo:
--     PUT /api/v1/config/KYB_REQUIRE_MANDATORY_DOCS
--     Body: {"value": {"value": 1}, "description": "Politica RN-045 activa"}
--
--   Idempotente: ON CONFLICT DO NOTHING.

INSERT INTO public.app_config (config_key, config_value, description, created_date, created_by)
VALUES
    ('KYB_REQUIRE_MANDATORY_DOCS',
     '{"value": 0}'::jsonb,
     'Feature flag: si es 1, PUT /requestProvider/user/send rechaza el submit cuando faltan documentos con mandatory=true. Default OFF para no bloquear QA in-flight.',
     now(), 1)
ON CONFLICT (config_key) DO NOTHING;
