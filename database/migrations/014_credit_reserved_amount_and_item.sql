-- Migration: Add reserved amount, shopping_cart_item_id and RESERVED/CONSUMED status to credit
-- Date: 2026-03-14
-- Description: Supports credit reservation flow: reserve credits for cart items (reserved_amount, item link),
--              status RESERVED; on payment consume reserved and set CONSUMED or back to CREATED if remainder.

-- Step 1: Add reserved_amount column (amount reserved for a cart item)
ALTER TABLE public.credit
ADD COLUMN reserved_amount numeric(10, 2) NOT NULL DEFAULT 0;

-- Step 2: Add shopping_cart_item_id to associate reserved credit with cart item
ALTER TABLE public.credit
ADD COLUMN shopping_cart_item_id int8 NULL;

-- Step 3: Drop existing status check constraint to add new values
ALTER TABLE public.credit
DROP CONSTRAINT IF EXISTS credit_status_check;

-- Step 4: Add new status check including RESERVED and CONSUMED
ALTER TABLE public.credit
ADD CONSTRAINT credit_status_check CHECK (
    (status)::text = ANY (
        (ARRAY[
            'CREATED'::character varying,
            'RESERVED'::character varying,
            'CONSUMED'::character varying,
            'CANCELED'::character varying,
            'DELETED'::character varying
        ])::text[]
    )
);

-- Step 5: Add check reserved_amount >= 0 and reserved_amount <= amount when status = 'RESERVED'
ALTER TABLE public.credit
ADD CONSTRAINT chk_credit_reserved_amount CHECK (reserved_amount >= (0)::numeric);

-- Step 6: Foreign key to shopping_cart_item (optional; only set when credit is reserved for an item)
ALTER TABLE public.credit
ADD CONSTRAINT fk_credit_shopping_cart_item
FOREIGN KEY (shopping_cart_item_id)
REFERENCES public.shopping_cart_item(id)
ON DELETE SET NULL;

-- Step 7: Index for queries by shopping_cart_item_id
CREATE INDEX idx_credit_shopping_cart_item_id ON public.credit USING btree (shopping_cart_item_id);

-- Step 8: Comments
COMMENT ON COLUMN public.credit.reserved_amount IS 'Monto reservado para un item del carrito; al consumir se deduce de amount';
COMMENT ON COLUMN public.credit.shopping_cart_item_id IS 'Item del carrito al que está asociada la reserva; NULL cuando no está reservado';
