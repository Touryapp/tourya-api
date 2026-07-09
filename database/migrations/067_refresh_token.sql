-- Migration 067: Refresh token store con rotacion + deteccion de reuso
-- Date: 2026-07-08
-- Description:
--   Almacena los refresh tokens emitidos por el sistema de auth. Necesario para:
--   1) Rotacion: cada uso de un refresh token lo revoca y emite uno nuevo (menor
--      exposicion si se filtra).
--   2) Deteccion de reuso: si un refresh token ya revocado se intenta usar de
--      nuevo, se revoca toda su familia (sesion completa).
--   3) Logout server-side: al cerrar sesion se revoca la familia entera.
--
--   El "family_id" agrupa todos los refresh tokens de una misma sesion (encadenados
--   por previous_jti). Al login se genera un family_id nuevo; al rotar se hereda.

CREATE TABLE IF NOT EXISTS public.refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id INT4 NOT NULL REFERENCES public._user(id) ON DELETE CASCADE,
    jti VARCHAR(64) NOT NULL UNIQUE,
    family_id VARCHAR(64) NOT NULL,
    previous_jti VARCHAR(64),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revoked_reason VARCHAR(50),
    user_agent VARCHAR(500),
    ip_address VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_jti
    ON public.refresh_token (jti);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id
    ON public.refresh_token (user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_token_family_id
    ON public.refresh_token (family_id);

CREATE INDEX IF NOT EXISTS idx_refresh_token_previous_jti
    ON public.refresh_token (previous_jti);

COMMENT ON TABLE public.refresh_token IS
    'Refresh tokens emitidos por el sistema de auth. Rotacion + deteccion de reuso + logout server-side.';
COMMENT ON COLUMN public.refresh_token.jti IS
    'JWT ID unico del refresh token. Se busca aca al recibir el JWT crudo.';
COMMENT ON COLUMN public.refresh_token.family_id IS
    'Agrupa refresh tokens de una misma sesion. Al login = UUID nuevo. Al rotar = heredado.';
COMMENT ON COLUMN public.refresh_token.previous_jti IS
    'JTI del refresh anterior en la cadena de rotacion (NULL para el primero). Usado para detectar reuso.';
COMMENT ON COLUMN public.refresh_token.revoked_at IS
    'NULL = activo. Set al hacer logout, rotar (revocacion soft del anterior) o detectar reuso.';
COMMENT ON COLUMN public.refresh_token.revoked_reason IS
    'Motivo de revocacion: logout, rotated, reuse_detected, family_revoked.';
