-- Rollback Migration: Remove provider_unit_price from shopping_cart_item_detail
-- Description: Removes provider_unit_price column from shopping_cart_item_detail table
-- Date: 2024

-- Step 1: Remove provider_unit_price column
ALTER TABLE public.shopping_cart_item_detail
DROP COLUMN IF EXISTS provider_unit_price;


