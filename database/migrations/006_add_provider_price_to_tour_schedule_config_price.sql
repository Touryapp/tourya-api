-- Migration: Add provider_price column to tour_schedule_config_price
-- Date: 2026-02-05
-- Description: Adds provider_price column to store the price that the provider receives for each age type

-- Step 1: Add provider_price column
ALTER TABLE public.tour_schedule_config_price 
ADD COLUMN provider_price NUMERIC NULL;

-- Step 2: Add comment to document the column purpose
COMMENT ON COLUMN public.tour_schedule_config_price.provider_price 
IS 'Precio que el proveedor recibe por este tipo de edad (puede ser diferente al precio de venta)';

-- Verification query (optional - uncomment to verify)
-- SELECT column_name, data_type, is_nullable, column_default
-- FROM information_schema.columns 
-- WHERE table_name = 'tour_schedule_config_price' 
-- ORDER BY ordinal_position;
