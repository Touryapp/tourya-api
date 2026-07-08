-- Migration 066: Wompi webhook event log
-- Date: 2026-07-08
-- Description:
--   Tabla para registrar todos los eventos webhook recibidos de Wompi.
--   Uso: (a) auditoria de eventos recibidos, (b) reconciliacion posterior de pagos huerfanos.
--   Endpoint que persiste aqui: POST /public/wompi/webhook

CREATE TABLE IF NOT EXISTS public.wompi_webhook_event (
    id BIGSERIAL PRIMARY KEY,
    wompi_event_type VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(120) NOT NULL,
    transaction_reference VARCHAR(255),
    transaction_status VARCHAR(30) NOT NULL,
    amount_in_cents BIGINT,
    currency VARCHAR(10),
    wompi_sent_at TIMESTAMPTZ,
    wompi_timestamp BIGINT,
    raw_payload JSONB NOT NULL,
    signature_valid BOOLEAN NOT NULL DEFAULT FALSE,
    signature_checksum VARCHAR(128),
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    linked_payment_id BIGINT REFERENCES public.payment(payment_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_wompi_event_transaction_id
    ON public.wompi_webhook_event (transaction_id);

CREATE INDEX IF NOT EXISTS idx_wompi_event_unprocessed
    ON public.wompi_webhook_event (received_at)
    WHERE processed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_wompi_event_received_at
    ON public.wompi_webhook_event (received_at);

COMMENT ON TABLE public.wompi_webhook_event IS
    'Log de eventos webhook recibidos de Wompi. Usado para reconciliacion + auditoria.';
COMMENT ON COLUMN public.wompi_webhook_event.signature_valid IS
    'TRUE si el checksum SHA-256 de Wompi verifico correctamente contra el events secret.';
COMMENT ON COLUMN public.wompi_webhook_event.processed_at IS
    'Marca temporal de cuando el job de reconciliacion proceso este evento (NULL = pendiente).';
COMMENT ON COLUMN public.wompi_webhook_event.linked_payment_id IS
    'Payment de Tourya al que se pudo asociar este evento tras reconciliacion.';
