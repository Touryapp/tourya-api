-- Migration 074: Notificaciones de expiración de créditos (BE-18 + BE-19)
-- Date: 2026-07-12
--
-- BE-18 (RN-036): correo automático "crédito por expirar" 30 y 7 días antes.
-- BE-19 (RN-036): correo automático "crédito expirado" al vencer.
--
-- Agrega 3 columnas para idempotencia (evitar reenvío del mismo correo si el
-- job corre 2 veces por accidente) y suma el valor 'EXPIRED' al catálogo de
-- estados de crédito (la columna credit.status es VARCHAR(20), no un ENUM
-- de PostgreSQL, por lo que no requiere cambio de tipo).
--
-- Backfill: marca EXPIRED los créditos que ya vencieron sin haber sido
-- consumidos. NO envía correo retroactivo (expired_notified_at = NOW() para
-- que el job los considere ya notificados).

ALTER TABLE public.credit
    ADD COLUMN IF NOT EXISTS reminder_30d_sent_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS reminder_7d_sent_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS expired_notified_at TIMESTAMPTZ NULL;

COMMENT ON COLUMN public.credit.reminder_30d_sent_at IS 'BE-18: timestamp del envío del recordatorio 30 días antes de expiration_date. NULL = no enviado. Usado como flag de idempotencia por CreditExpirationJob.';
COMMENT ON COLUMN public.credit.reminder_7d_sent_at IS 'BE-18: timestamp del envío del recordatorio 7 días antes de expiration_date.';
COMMENT ON COLUMN public.credit.expired_notified_at IS 'BE-19: timestamp del envío del correo de expiración. NULL = no notificado.';

-- Backfill de créditos ya expirados: marca EXPIRED sin reenviar correo
-- (llevan meses vencidos, notificarlos ahora seria molesto).
UPDATE public.credit
SET status = 'EXPIRED',
    expired_notified_at = NOW()
WHERE status = 'CREATED'
  AND expiration_date < CURRENT_DATE;

-- Indice para el job diario: acelera busqueda de candidatos por fecha exacta.
CREATE INDEX IF NOT EXISTS idx_credit_status_expiration_date
    ON public.credit (status, expiration_date);
