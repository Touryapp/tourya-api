-- =====================================================
-- PostgreSQL ROLLBACK Script: Revert i18n JSONB Changes
-- =====================================================
-- This script reverts the JSONB columns back to TEXT/VARCHAR
-- and extracts the Spanish (es) value as the default
-- 
-- IMPORTANT: This will LOSE English and Portuguese translations!
-- Only use this if you need to rollback the migration.
-- 
-- Usage:
--   psql -U your_user -d your_database -f 02_rollback_jsonb_to_text.sql
-- =====================================================

-- Start transaction
BEGIN;

-- =====================================================
-- 1. DROP CONSTRAINTS
-- =====================================================

ALTER TABLE tour_address DROP CONSTRAINT IF EXISTS tour_address_location_es_required;
ALTER TABLE tour_main_attractions DROP CONSTRAINT IF EXISTS tour_main_attractions_description_es_required;
ALTER TABLE tour_includes_excludes DROP CONSTRAINT IF EXISTS tour_includes_excludes_description_es_required;
ALTER TABLE tour_faq DROP CONSTRAINT IF EXISTS tour_faq_question_es_required;
ALTER TABLE tour_faq DROP CONSTRAINT IF EXISTS tour_faq_answer_es_required;
ALTER TABLE tour_itinerary DROP CONSTRAINT IF EXISTS tour_itinerary_title_es_required;
ALTER TABLE tour_itinerary DROP CONSTRAINT IF EXISTS tour_itinerary_description_es_required;
ALTER TABLE tour_cancellation_policy DROP CONSTRAINT IF EXISTS tour_cancellation_policy_observations_es_required;
ALTER TABLE tour_gallery DROP CONSTRAINT IF EXISTS tour_gallery_description_es_required;
ALTER TABLE tour DROP CONSTRAINT IF EXISTS tour_description_es_required;

-- =====================================================
-- 2. DROP INDEXES
-- =====================================================

DROP INDEX IF EXISTS idx_tour_address_location_gin;
DROP INDEX IF EXISTS idx_tour_main_attractions_description_gin;
DROP INDEX IF EXISTS idx_tour_includes_excludes_description_gin;
DROP INDEX IF EXISTS idx_tour_faq_question_gin;
DROP INDEX IF EXISTS idx_tour_faq_answer_gin;
DROP INDEX IF EXISTS idx_tour_itinerary_title_gin;
DROP INDEX IF EXISTS idx_tour_itinerary_description_gin;
DROP INDEX IF EXISTS idx_tour_cancellation_policy_observations_gin;
DROP INDEX IF EXISTS idx_tour_gallery_description_gin;
DROP INDEX IF EXISTS idx_tour_description_gin;
DROP INDEX IF EXISTS idx_tour_address_location_es;
DROP INDEX IF EXISTS idx_tour_main_attractions_description_es;
DROP INDEX IF EXISTS idx_tour_includes_excludes_description_es;
DROP INDEX IF EXISTS idx_tour_description_es;

-- =====================================================
-- 3. REVERT COLUMNS TO TEXT TYPE
-- =====================================================

-- Table: tour_address
-- Column: location
ALTER TABLE tour_address 
    ALTER COLUMN location TYPE TEXT USING 
    CASE 
        WHEN location IS NULL THEN NULL
        ELSE location->>'es'
    END;

-- Table: tour_main_attractions
-- Column: description
ALTER TABLE tour_main_attractions 
    ALTER COLUMN description TYPE TEXT USING 
    CASE 
        WHEN description IS NULL THEN NULL
        ELSE description->>'es'
    END;

-- Table: tour_includes_excludes
-- Column: description
ALTER TABLE tour_includes_excludes 
    ALTER COLUMN description TYPE TEXT USING 
    CASE 
        WHEN description IS NULL THEN NULL
        ELSE description->>'es'
    END;

-- Table: tour_faq
-- Columns: question, answer
ALTER TABLE tour_faq 
    ALTER COLUMN question TYPE TEXT USING 
    CASE 
        WHEN question IS NULL THEN NULL
        ELSE question->>'es'
    END;

ALTER TABLE tour_faq 
    ALTER COLUMN answer TYPE TEXT USING 
    CASE 
        WHEN answer IS NULL THEN NULL
        ELSE answer->>'es'
    END;

-- Table: tour_itinerary
-- Columns: title, description
ALTER TABLE tour_itinerary 
    ALTER COLUMN title TYPE TEXT USING 
    CASE 
        WHEN title IS NULL THEN NULL
        ELSE title->>'es'
    END;

ALTER TABLE tour_itinerary 
    ALTER COLUMN description TYPE TEXT USING 
    CASE 
        WHEN description IS NULL THEN NULL
        ELSE description->>'es'
    END;

-- Table: tour_cancellation_policy
-- Column: observations
ALTER TABLE tour_cancellation_policy 
    ALTER COLUMN observations TYPE TEXT USING 
    CASE 
        WHEN observations IS NULL THEN NULL
        ELSE observations->>'es'
    END;

-- Table: tour_gallery
-- Column: description
ALTER TABLE tour_gallery 
    ALTER COLUMN description TYPE TEXT USING 
    CASE 
        WHEN description IS NULL THEN NULL
        ELSE description->>'es'
    END;

-- Table: tour
-- Column: description
ALTER TABLE tour 
    ALTER COLUMN description TYPE TEXT USING 
    CASE 
        WHEN description IS NULL THEN NULL
        ELSE description->>'es'
    END;

-- Commit transaction
COMMIT;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Uncomment to verify the rollback
/*
-- Check column types (should be TEXT now)
SELECT 
    table_name, 
    column_name, 
    data_type 
FROM information_schema.columns 
WHERE table_name IN (
    'tour_address', 
    'tour_main_attractions', 
    'tour_includes_excludes',
    'tour_faq',
    'tour_itinerary',
    'tour_cancellation_policy',
    'tour_gallery'
)
AND column_name IN ('location', 'description', 'question', 'answer', 'title', 'observations')
ORDER BY table_name, column_name;

-- Sample data check (should be plain text now)
SELECT id, location FROM tour_address LIMIT 5;
SELECT id, description FROM tour_main_attractions LIMIT 5;
*/
