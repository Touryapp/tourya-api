ALTER TABLE public.credit
    ADD COLUMN IF NOT EXISTS transferred_from_user_id INT4 REFERENCES public._user(id),
    ADD COLUMN IF NOT EXISTS transferred_at TIMESTAMP;

COMMENT ON COLUMN public.credit.transferred_from_user_id IS 'Usuario que cedió el crédito (trazabilidad).';
COMMENT ON COLUMN public.credit.transferred_at IS 'Fecha de transferencia; si no es NULL el crédito ya fue transferido.';
