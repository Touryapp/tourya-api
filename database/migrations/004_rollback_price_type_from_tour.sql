-- Rollback Migration: Remove price_type field from tour table
-- Date: 2026-01-17
-- Description: Removes the price_type column and enum type

-- Step 1: Remove the price_type column from the tour table
ALTER TABLE public.tour 
DROP COLUMN IF EXISTS price_type;

-- Step 2: Drop the price_type enum type
DROP TYPE IF EXISTS price_type_enum;
