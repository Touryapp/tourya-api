-- Migration 073: Account lockout tras N intentos fallidos + seeds config (SEC-10)
-- Date: 2026-07-09
-- Description:
--   Agrega 2 columnas a _user para soportar el lockout automatico por intentos
--   fallidos de login, y siembra 3 keys en app_config para controlar el
--   comportamiento:
--     - failed_login_attempts (int, default 0): contador desde el ultimo login
--       exitoso. Se resetea a 0 en cada login exitoso.
--     - locked_until (timestamptz, nullable): timestamp hasta el cual la cuenta
--       queda bloqueada por lockout automatico. NULL = no bloqueada por
--       lockout. La columna existente accountLocked (boolean) sigue siendo la
--       forma de bloqueo permanente por ADMIN.
--
--   Config keys (feature flag OFF por default para no bloquear QA):
--     - AUTH_LOCKOUT_ENABLED = 0 (OFF)
--     - AUTH_LOCKOUT_MAX_ATTEMPTS = 5 (bloquea al 5to intento fallido)
--     - AUTH_LOCKOUT_BASE_BACKOFF_SECONDS = 60 (base del backoff exponencial)
--
--   Backoff exponencial: al superar max_attempts, cada nuevo fallo duplica el
--   tiempo de bloqueo. Cap = 24h (86400s).
--     attempts=5 -> 60s
--     attempts=6 -> 120s
--     attempts=7 -> 240s
--     attempts=8 -> 480s (~8 min)
--     attempts=13 -> ~4h
--     attempts=15+ -> cap 24h
--
--   Activacion sin re-deploy cuando Luis diga:
--     PUT /api/v1/config/AUTH_LOCKOUT_ENABLED {"value":{"value":1}}

ALTER TABLE public._user
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0;

ALTER TABLE public._user
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_user_locked_until
    ON public._user (locked_until)
    WHERE locked_until IS NOT NULL;

COMMENT ON COLUMN public._user.failed_login_attempts IS
    'Contador de intentos de login fallidos desde el ultimo login exitoso. Se resetea a 0 en cada success.';
COMMENT ON COLUMN public._user.locked_until IS
    'Timestamp hasta el cual la cuenta queda bloqueada por lockout automatico. NULL = no bloqueada. Distinto de accountLocked (bloqueo permanente por ADMIN).';

INSERT INTO public.app_config (config_key, config_value, description, created_date, created_by)
VALUES
    ('AUTH_LOCKOUT_ENABLED',
     '{"value": 0}'::jsonb,
     'Feature flag: 1 activa el lockout automatico tras N intentos fallidos. Default 0 para no bloquear QA in-flight.',
     now(), 1),
    ('AUTH_LOCKOUT_MAX_ATTEMPTS',
     '{"value": 5}'::jsonb,
     'Numero de intentos fallidos consecutivos antes de bloquear la cuenta. Default 5.',
     now(), 1),
    ('AUTH_LOCKOUT_BASE_BACKOFF_SECONDS',
     '{"value": 60}'::jsonb,
     'Segundos base del backoff exponencial. Cada intento fallido despues del max duplica el tiempo (cap 24h).',
     now(), 1)
ON CONFLICT (config_key) DO NOTHING;
