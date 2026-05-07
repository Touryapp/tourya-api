-- Migration: Provider payout orders + reservation payout available date
-- Date: 2026-04-26
-- Description:
-- 1) Add payout_available_date to reservation (reservation_date + 2 days).
-- 2) Create provider payout order tables to group DELIVERED reservations for payment.

-- 1) Reservation: fecha disponible pago
ALTER TABLE public.reservation
ADD COLUMN IF NOT EXISTS payout_available_date date NULL;

-- Backfill (si ya hay reservas existentes)
UPDATE public.reservation
SET payout_available_date = (reservation_date AT TIME ZONE 'UTC')::date + INTERVAL '2 days'
WHERE payout_available_date IS NULL
  AND reservation_date IS NOT NULL;

-- 2) Provider payout order
CREATE TABLE IF NOT EXISTS public.provider_payout_order (
    id bigserial PRIMARY KEY,
    provider_id int4 NOT NULL REFERENCES public.provider(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    pay_date date NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    amount_total numeric(10,2) NOT NULL DEFAULT 0
);

-- Link payout order <-> reservations (one reservation can belong to only one order)
CREATE TABLE IF NOT EXISTS public.provider_payout_order_reservation (
    payout_order_id bigint NOT NULL REFERENCES public.provider_payout_order(id) ON DELETE CASCADE,
    reservation_id bigint NOT NULL REFERENCES public.reservation(reservation_id) ON DELETE CASCADE,
    account_payable_id bigint NULL REFERENCES public.account_payable(id) ON DELETE SET NULL,
    amount numeric(10,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (payout_order_id, reservation_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_provider_payout_order_reservation_reservation_id
ON public.provider_payout_order_reservation (reservation_id);

CREATE INDEX IF NOT EXISTS idx_provider_payout_order_provider_pay_date
ON public.provider_payout_order (provider_id, pay_date);

CREATE INDEX IF NOT EXISTS idx_provider_payout_order_reservation_order
ON public.provider_payout_order_reservation (payout_order_id);

-- Attachments (payment proof files)
CREATE TABLE IF NOT EXISTS public.provider_payout_attachment (
    id bigserial PRIMARY KEY,
    payout_order_id bigint NOT NULL REFERENCES public.provider_payout_order(id) ON DELETE CASCADE,
    file_url text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

