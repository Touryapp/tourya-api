-- Migration: Remove min_capacity and max_capacity from tour_schedule_config_slot
-- Date: 2026-01-29
-- Description: Removes min_capacity and max_capacity columns from tour_schedule_config_slot table

-- Step 1: Remove min_capacity column
ALTER TABLE public.tour_schedule_config_slot 
DROP COLUMN IF EXISTS min_capacity;

-- Step 2: Remove max_capacity column
ALTER TABLE public.tour_schedule_config_slot 
DROP COLUMN IF EXISTS max_capacity;

-- Verification query (optional - comment out if not needed)
-- SELECT column_name, data_type 
-- FROM information_schema.columns 
-- WHERE table_name = 'tour_schedule_config_slot' 
-- ORDER BY ordinal_position;
