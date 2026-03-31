-- Migration: Allow reservation.payment_id to be NULL for TEMPORAL holds
-- Date: 2026-03-31
--
-- TEMPORAL reservations are created before payment exists, so payment_id must be nullable.

ALTER TABLE public.reservation
    DROP CONSTRAINT IF EXISTS fk_reservation_payment;

ALTER TABLE public.reservation
    ALTER COLUMN payment_id DROP NOT NULL;

ALTER TABLE public.reservation
    ADD CONSTRAINT fk_reservation_payment
        FOREIGN KEY (payment_id)
        REFERENCES public.payment(payment_id)
        ON DELETE CASCADE;

