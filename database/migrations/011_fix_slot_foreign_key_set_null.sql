-- Migration: Remove foreign key constraint for slot_id in shopping_cart_item
-- Date: 2026-02-18
-- Description: Removes the foreign key constraint fk_shopping_cart_item_slot because:
--              1. Prices are already stored in shopping_cart_item_detail
--              2. Rescheduling uses prices from the new schedule, not the original slot
--              3. slot_id is only used for display purposes in responses
--              4. The constraint causes errors when slots are deleted/updated in configs
--              slot_id will remain as a simple integer column without referential integrity.

-- Step 1: Drop the existing constraint
ALTER TABLE public.shopping_cart_item 
DROP CONSTRAINT IF EXISTS fk_shopping_cart_item_slot;

-- Verification query (optional - comment out if not needed)
-- SELECT 
--     tc.constraint_name, 
--     tc.table_name, 
--     kcu.column_name,
--     ccu.table_name AS foreign_table_name,
--     ccu.column_name AS foreign_column_name,
--     rc.delete_rule
-- FROM information_schema.table_constraints AS tc 
-- JOIN information_schema.key_column_usage AS kcu
--   ON tc.constraint_name = kcu.constraint_name
-- JOIN information_schema.constraint_column_usage AS ccu
--   ON ccu.constraint_name = tc.constraint_name
-- JOIN information_schema.referential_constraints AS rc
--   ON rc.constraint_name = tc.constraint_name
-- WHERE tc.constraint_name = 'fk_shopping_cart_item_slot';
