-- Migration 039: payout_status NOT NULL aunque Hibernate envíe NULL en INSERT
-- PostgreSQL NO aplica DEFAULT de columna cuando el INSERT lista la columna con NULL explícito.
-- Solución: DEFAULT en columna + trigger BEFORE INSERT como red de seguridad.

ALTER TABLE public.reservation
  ALTER COLUMN payout_status SET DEFAULT 'PENDING';

UPDATE public.reservation
SET payout_status = 'PENDING'
WHERE payout_status IS NULL;

-- Trigger: si llega NULL (bug cliente/Hibernate antiguo), forzar PENDING antes del INSERT
CREATE OR REPLACE FUNCTION public.trg_reservation_payout_status_bi()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.payout_status IS NULL THEN
    NEW.payout_status := 'PENDING';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_reservation_payout_status_bi ON public.reservation;

CREATE TRIGGER trg_reservation_payout_status_bi
  BEFORE INSERT ON public.reservation
  FOR EACH ROW
  EXECUTE PROCEDURE public.trg_reservation_payout_status_bi();
