-- Migration: Add expires_at to reservation and allow TEMPORAL in delivery_status
-- Date: 2026-03-31
-- Description: Supports 15-minute hold reservations (TEMPORAL) that can be confirmed by payment.

-- 1) expires_at column
ALTER TABLE public.reservation
ADD COLUMN IF NOT EXISTS expires_at timestamp NULL;

COMMENT ON COLUMN public.reservation.expires_at IS 'Fecha/hora de expiración para reservas TEMPORAL (hold de 15 min)';

-- 2) Ensure delivery_status accepts TEMPORAL
-- delivery_status is varchar(50) without DB CHECK in ddl.sql, so no constraint change needed here.
-- If your DB has a constraint/enumeration, add TEMPORAL accordingly.

