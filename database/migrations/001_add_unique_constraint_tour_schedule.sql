-- =====================================================
-- Migration Script: Add Unique Constraint to tour_schedule
-- Purpose: Ensure only one schedule per (tour_id, schedule_date)
-- Author: TourYa Development Team
-- Date: 2025-12-21
-- =====================================================

-- STEP 1: Identify and review duplicate records
-- Run this first to see what duplicates exist
SELECT 
    tour_id, 
    schedule_date, 
    COUNT(*) as duplicate_count,
    STRING_AGG(id::text, ', ' ORDER BY id) as schedule_ids
FROM tour_schedule
GROUP BY tour_id, schedule_date
HAVING COUNT(*) > 1
ORDER BY tour_id, schedule_date;

-- STEP 2: Backup the tour_schedule table (RECOMMENDED)
-- Uncomment and run if you want a backup
-- CREATE TABLE tour_schedule_backup_20251221 AS 
-- SELECT * FROM tour_schedule;

-- STEP 3: Clean duplicate records
-- This keeps the MOST RECENT record (highest ID) for each (tour_id, schedule_date)
-- and deletes the older duplicates
DELETE FROM tour_schedule
WHERE id NOT IN (
    SELECT MAX(id)
    FROM tour_schedule
    GROUP BY tour_id, schedule_date
);

-- STEP 4: Verify no duplicates remain
-- This should return 0 rows
SELECT 
    tour_id, 
    schedule_date, 
    COUNT(*) as count
FROM tour_schedule
GROUP BY tour_id, schedule_date
HAVING COUNT(*) > 1;

-- STEP 5: Add unique constraint
-- This prevents future duplicates
ALTER TABLE public.tour_schedule 
ADD CONSTRAINT uq_tour_schedule_tour_date 
UNIQUE (tour_id, schedule_date);

-- STEP 6: Create index for better query performance
-- This improves performance when searching by tour_id and schedule_date
CREATE INDEX IF NOT EXISTS idx_tour_schedule_tour_date 
ON public.tour_schedule (tour_id, schedule_date);

-- STEP 7: Verify the constraint was created
SELECT 
    conname as constraint_name,
    contype as constraint_type,
    pg_get_constraintdef(oid) as constraint_definition
FROM pg_constraint
WHERE conrelid = 'tour_schedule'::regclass
    AND conname = 'uq_tour_schedule_tour_date';

-- =====================================================
-- ROLLBACK SCRIPT (if needed)
-- =====================================================
-- Uncomment these lines if you need to rollback the changes

-- -- Remove the unique constraint
-- ALTER TABLE public.tour_schedule 
-- DROP CONSTRAINT IF EXISTS uq_tour_schedule_tour_date;

-- -- Remove the index
-- DROP INDEX IF EXISTS idx_tour_schedule_tour_date;

-- -- Restore from backup (if you created one)
-- -- TRUNCATE tour_schedule;
-- -- INSERT INTO tour_schedule SELECT * FROM tour_schedule_backup_20251221;
