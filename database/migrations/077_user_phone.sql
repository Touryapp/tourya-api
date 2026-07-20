-- Migration 077 — BE-22d
-- Agrega columna phone al _user para permitir que sub-usuarios (PROVIDER_OPERATOR
-- y futuros roles) tengan su propio numero de contacto.
--
-- Hoy `TourPrincipalOperatorService.fromOperatorLink()` usa `provider.phone`
-- como fallback del phone del operador — con este campo, el operador principal
-- de un tour muestra su propio numero (RN-026 de Luis 2026-07-19).
--
-- Aditiva, nullable, sin backfill: usuarios existentes quedan NULL y el
-- servicio hace fallback al provider.phone (retrocompatible).

ALTER TABLE public._user
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20) NULL;

COMMENT ON COLUMN public._user.phone IS
    'BE-22d: telefono de contacto del usuario. Usado por PROVIDER_OPERATOR como contacto principal del tour (RN-026). NULL cuando no se ha cargado — el fallback en TourPrincipalOperatorService toma provider.phone.';
