-- Migration: Add provider_unit_price to shopping_cart_item_detail
-- Description: Adds provider_unit_price column to track the provider's unit price for each detail item
-- Date: 2024

-- Step 1: Add provider_unit_price column
ALTER TABLE public.shopping_cart_item_detail
ADD COLUMN IF NOT EXISTS provider_unit_price numeric(10, 2) NULL;

-- Step 2: Add comment
COMMENT ON COLUMN public.shopping_cart_item_detail.provider_unit_price IS 'Precio unitario que recibe el proveedor por este detalle';

-- Step 3: Update existing records (optional - set to 0 or NULL if provider_price was not tracked before)
-- UPDATE public.shopping_cart_item_detail
-- SET provider_unit_price = 0
-- WHERE provider_unit_price IS NULL;


