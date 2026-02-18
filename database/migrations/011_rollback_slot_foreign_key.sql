-- Rollback: Restore foreign key constraint for slot_id in shopping_cart_item
-- Date: 2026-02-18
-- Description: Restores the foreign key constraint fk_shopping_cart_item_slot
--              (default behavior: RESTRICT - blocks deletion if referenced)

-- Recreate the constraint without ON DELETE SET NULL (default: RESTRICT)
ALTER TABLE public.shopping_cart_item 
ADD CONSTRAINT fk_shopping_cart_item_slot 
FOREIGN KEY (slot_id) 
REFERENCES public.tour_schedule_config_slot(id);
