-- Migration: Add price_type field to tour table
-- Date: 2026-01-17
-- Description: Adds a new price_type column with enum type (individual, grupo)

-- Step 1: Create the price_type enum type
CREATE TYPE price_type_enum AS ENUM ('individual', 'grupo');

-- Step 2: Add the price_type column to the tour table
ALTER TABLE public.tour 
ADD COLUMN price_type price_type_enum NULL;

-- Step 3: Add a comment to the column for documentation
COMMENT ON COLUMN public.tour.price_type IS 'Type of pricing for the tour: individual or grupo (group)';

-- Optional: Set a default value for existing records if needed
-- UPDATE public.tour SET price_type = 'individual' WHERE price_type IS NULL;

-- Optional: Make the column NOT NULL after setting default values
-- ALTER TABLE public.tour ALTER COLUMN price_type SET NOT NULL;
