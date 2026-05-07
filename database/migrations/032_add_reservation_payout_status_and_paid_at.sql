-- Migration: Add reservation payout status fields
-- Date: 2026-04-26
-- Description:
--  - Track when a DELIVERED reservation has been paid to provider via payout order.

ALTER TABLE public.reservation
ADD COLUMN IF NOT EXISTS payout_status varchar(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE public.reservation
ADD COLUMN IF NOT EXISTS payout_paid_at timestamptz NULL;

CREATE INDEX IF NOT EXISTS idx_reservation_payout_status
ON public.reservation (payout_status);

