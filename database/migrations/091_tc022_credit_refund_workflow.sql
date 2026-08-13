-- Migration 091: TC-022 credit refund workflow (issue #253)
-- Date: 2026-08-13
--
-- Nuevo flujo: el turista puede solicitar la devolucion en efectivo de un
-- credito no reservado (CREATED con saldo libre). Un usuario ADMIN o
-- BACKOFFICE_OPERATION completa la devolucion subiendo un comprobante y el
-- credito pasa a REFUNDED.
--
-- Transiciones validas:
--   CREATED -> REFUND_REQUESTED  (endpoint turista)
--   REFUND_REQUESTED -> REFUNDED (endpoint admin, con comprobante)
--
-- Cambios:
--   1. credit_status_check: agrega REFUND_REQUESTED y REFUNDED.
--      Nota: la columna credit.status es VARCHAR(20) con CHECK constraint
--      (no un ENUM de PostgreSQL). En 074 se empezo a usar 'EXPIRED' sin
--      actualizar el CHECK; esta migracion normaliza el catalogo completo:
--      CREATED, RESERVED, CONSUMED, CANCELED, DELETED, EXPIRED,
--      REFUND_REQUESTED, REFUNDED.
--
--   2. 3 columnas nuevas en public.credit:
--      - refund_requested_at TIMESTAMPTZ NULL
--      - refunded_at         TIMESTAMPTZ NULL
--      - refund_proof_url    VARCHAR(500) NULL
--
-- Cero side effect sobre creditos existentes (usar/transferir/cancelar/expirar).

BEGIN;

-- 1. Actualizar CHECK constraint para admitir los nuevos valores.
ALTER TABLE public.credit
    DROP CONSTRAINT IF EXISTS credit_status_check;

ALTER TABLE public.credit
    ADD CONSTRAINT credit_status_check CHECK (
        (status)::text = ANY (
            (ARRAY[
                'CREATED'::character varying,
                'RESERVED'::character varying,
                'CONSUMED'::character varying,
                'CANCELED'::character varying,
                'DELETED'::character varying,
                'EXPIRED'::character varying,
                'REFUND_REQUESTED'::character varying,
                'REFUNDED'::character varying
            ])::text[]
        )
    );

-- 2. Columnas del flujo de devolucion.
ALTER TABLE public.credit
    ADD COLUMN IF NOT EXISTS refund_requested_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS refunded_at         TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS refund_proof_url    VARCHAR(500) NULL;

COMMENT ON COLUMN public.credit.refund_requested_at IS
    'TC-022: timestamp cuando el turista solicito la devolucion en efectivo (transicion CREATED -> REFUND_REQUESTED).';
COMMENT ON COLUMN public.credit.refunded_at IS
    'TC-022: timestamp cuando ADMIN/BACKOFFICE marco la devolucion como completada (transicion REFUND_REQUESTED -> REFUNDED).';
COMMENT ON COLUMN public.credit.refund_proof_url IS
    'TC-022: URL publica del comprobante de devolucion subido a S3/GCS (path credit-refund-proofs/{creditId}/...).';

COMMIT;

-- =============================================================================
-- Rollback (comentado):
-- BEGIN;
-- ALTER TABLE public.credit
--     DROP COLUMN IF EXISTS refund_proof_url,
--     DROP COLUMN IF EXISTS refunded_at,
--     DROP COLUMN IF EXISTS refund_requested_at;
--
-- ALTER TABLE public.credit
--     DROP CONSTRAINT IF EXISTS credit_status_check;
--
-- ALTER TABLE public.credit
--     ADD CONSTRAINT credit_status_check CHECK (
--         (status)::text = ANY (
--             (ARRAY[
--                 'CREATED'::character varying,
--                 'RESERVED'::character varying,
--                 'CONSUMED'::character varying,
--                 'CANCELED'::character varying,
--                 'DELETED'::character varying,
--                 'EXPIRED'::character varying
--             ])::text[]
--         )
--     );
-- COMMIT;
