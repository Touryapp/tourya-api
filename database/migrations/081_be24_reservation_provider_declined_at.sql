-- Migration 081: BE-24 Fase 1 — reservation.provider_declined_at
-- Date: 2026-07-23
-- Description:
--   Nuevo campo para marcar cuándo el provider declinó una reserva
--   (RN-055 rediseñada, Luis 2026-07-23 en issue #193).
--
--   El endpoint PUT /reservations/{id}/decline lo setea y dispara auto-cancel
--   con crédito + email al turista con tours alternativos + anula el
--   AccountPayable para evitar doble pago en el próximo payout del provider.

ALTER TABLE public.reservation
    ADD COLUMN IF NOT EXISTS provider_declined_at TIMESTAMPTZ NULL;

COMMENT ON COLUMN public.reservation.provider_declined_at IS
    'BE-24 (RN-055): timestamp cuando el provider marcó "no puedo atender" la reserva. NULL = no ha sido declinada. Al setearse se dispara cancelación automática + crédito al turista + anula AccountPayable.';
