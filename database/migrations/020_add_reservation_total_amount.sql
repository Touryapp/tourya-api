-- Migration: Add total_amount to reservation for holds/payment validation
-- Date: 2026-03-31

ALTER TABLE public.reservation
ADD COLUMN IF NOT EXISTS total_amount numeric(10, 2) NULL;

COMMENT ON COLUMN public.reservation.total_amount IS 'Monto total asociado a la reserva (calculado al crear TEMPORAL). Se valida contra el pago.';

