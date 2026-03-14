-- Migration: Add amount_credit to payment and create payment_credit for credit usage detail
-- Date: 2026-03-14
-- Description: Persist total amount paid with credits and which credits were used per payment.

-- Step 1: Add amount_credit to payment (monto total pagado con créditos en este pago)
ALTER TABLE public.payment
ADD COLUMN amount_credit numeric(10, 2) NULL;

COMMENT ON COLUMN public.payment.amount_credit IS 'Monto total pagado con créditos en este pago; NULL si no se usaron créditos';

-- Step 2: Create payment_credit (detalle: qué crédito se usó y por cuánto)
CREATE TABLE public.payment_credit (
    id bigserial NOT NULL,
    payment_id int8 NOT NULL,
    credit_id int8 NOT NULL,
    amount_used numeric(10, 2) NOT NULL,
    CONSTRAINT payment_credit_pkey PRIMARY KEY (id),
    CONSTRAINT fk_payment_credit_payment FOREIGN KEY (payment_id) REFERENCES public.payment(payment_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_credit_credit FOREIGN KEY (credit_id) REFERENCES public.credit(id) ON DELETE RESTRICT
);

CREATE INDEX idx_payment_credit_payment_id ON public.payment_credit USING btree (payment_id);
CREATE INDEX idx_payment_credit_credit_id ON public.payment_credit USING btree (credit_id);

COMMENT ON TABLE public.payment_credit IS 'Créditos utilizados en cada pago (detalle por crédito)';
COMMENT ON COLUMN public.payment_credit.amount_used IS 'Monto consumido de este crédito en este pago';
