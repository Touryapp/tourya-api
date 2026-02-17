-- Rollback Migration: Remove provider_price column from tour_schedule_config_price
-- Date: 2026-02-05
-- Description: Removes provider_price column if rollback is needed

-- Step 1: Remove provider_price column
ALTER TABLE public.tour_schedule_config_price 
DROP COLUMN IF EXISTS provider_price;

-- Verification query (optional - uncomment to verify)
-- SELECT column_name, data_type 
-- FROM information_schema.columns 
-- WHERE table_name = 'tour_schedule_config_price' 
-- ORDER BY ordinal_position;
