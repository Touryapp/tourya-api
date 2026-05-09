-- Rollback Migration: Add back min_capacity and max_capacity to tour_schedule_config_slot
-- Date: 2026-01-29
-- Description: Restores min_capacity and max_capacity columns to tour_schedule_config_slot table

-- Step 1: Add back min_capacity column
ALTER TABLE public.tour_schedule_config_slot 
ADD COLUMN IF NOT EXISTS min_capacity int4 NULL;

-- Step 2: Add back max_capacity column
ALTER TABLE public.tour_schedule_config_slot 
ADD COLUMN IF NOT EXISTS max_capacity int4 NULL;

-- Step 3: Add comments to the columns for documentation
COMMENT ON COLUMN public.tour_schedule_config_slot.min_capacity IS 'Minimum capacity for the slot (restored)';
COMMENT ON COLUMN public.tour_schedule_config_slot.max_capacity IS 'Maximum capacity for the slot (restored)';
