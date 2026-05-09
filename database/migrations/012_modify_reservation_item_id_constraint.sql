-- Migration: Modify reservation item_id foreign key constraint to allow NULL
-- Date: 2026-02-28
-- Description: Changes the foreign key constraint to allow item_id to be NULL
--              so that canceled reservations can have item_id pointing to deleted items
--              (historical reference only)

-- Step 1: Drop the existing constraint
ALTER TABLE public.reservation 
DROP CONSTRAINT IF EXISTS fk_reservation_shopping_cart_item;

-- Step 2: Modify the column to allow NULL
ALTER TABLE public.reservation 
ALTER COLUMN item_id DROP NOT NULL;

-- Step 3: Recreate the constraint with ON DELETE SET NULL
ALTER TABLE public.reservation 
ADD CONSTRAINT fk_reservation_shopping_cart_item 
FOREIGN KEY (item_id) 
REFERENCES public.shopping_cart_item(id) 
ON DELETE SET NULL;
