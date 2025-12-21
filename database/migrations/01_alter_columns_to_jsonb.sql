-- =====================================================
-- PostgreSQL Migration Script: Add i18n Support with JSONB
-- =====================================================
-- This script converts text fields to JSONB format for multi-language support
-- Supports: Spanish (es), English (en), Portuguese (pt)
-- 
-- IMPORTANT: Backup your database before running this script!
-- 
-- Usage:
--   psql -U your_user -d your_database -f 01_alter_columns_to_jsonb.sql
-- =====================================================

-- Start transaction
BEGIN;

-- =====================================================
-- 1. ALTER COLUMNS TO JSONB TYPE
-- =====================================================

-- Table: tour_address
-- Column: location
ALTER TABLE tour_address 
    ALTER COLUMN location TYPE jsonb USING 
    CASE 
        WHEN location IS NULL THEN NULL
        ELSE jsonb_build_object('es', location, 'en', '', 'pt', '')
    END;

-- Table: tour_main_attractions
-- Column: description
ALTER TABLE tour_main_attractions 
    ALTER COLUMN description TYPE jsonb USING 
    CASE 
        WHEN description IS NULL THEN NULL
        ELSE jsonb_build_object('es', description, 'en', '', 'pt', '')
    END;

-- Table: tour_includes_excludes
-- Column: description
ALTER TABLE tour_includes_excludes 
    ALTER COLUMN description TYPE jsonb USING 
    CASE 
        WHEN description IS NULL THEN NULL
        ELSE jsonb_build_object('es', description, 'en', '', 'pt', '')
    END;

-- Table: tour_faq
-- Columns: question, answer
ALTER TABLE tour_faq 
    ALTER COLUMN question TYPE jsonb USING 
    CASE 
        WHEN question IS NULL THEN NULL
        ELSE jsonb_build_object('es', question, 'en', '', 'pt', '')
    END;

ALTER TABLE tour_faq 
    ALTER COLUMN answer TYPE jsonb USING 
    CASE 
        WHEN answer IS NULL THEN NULL
        ELSE jsonb_build_object('es', answer, 'en', '', 'pt', '')
    END;

-- Table: tour_itinerary
-- Columns: title, description
ALTER TABLE tour_itinerary 
    ALTER COLUMN title TYPE jsonb USING 
    CASE 
        WHEN title IS NULL THEN NULL
        ELSE jsonb_build_object('es', title, 'en', '', 'pt', '')
    END;

ALTER TABLE tour_itinerary 
    ALTER COLUMN description TYPE jsonb USING 
    CASE 
        WHEN description IS NULL THEN NULL
        ELSE jsonb_build_object('es', description, 'en', '', 'pt', '')
    END;

-- Table: tour_cancellation_policy
-- Column: observations
ALTER TABLE tour_cancellation_policy 
    ALTER COLUMN observations TYPE jsonb USING 
    CASE 
        WHEN observations IS NULL OR observations = '' THEN NULL
        ELSE jsonb_build_object('es', observations, 'en', '', 'pt', '')
    END;

-- Table: tour_gallery
-- Column: description
ALTER TABLE tour_gallery 
    ALTER COLUMN description TYPE jsonb USING 
    CASE 
        WHEN description IS NULL THEN NULL
        ELSE jsonb_build_object('es', description, 'en', '', 'pt', '')
    END;

-- Table: tour
-- Column: description
ALTER TABLE tour 
    ALTER COLUMN description TYPE jsonb USING 
    CASE 
        WHEN description IS NULL OR description = '' THEN NULL
        ELSE jsonb_build_object('es', description, 'en', '', 'pt', '')
    END;

-- =====================================================
-- 2. ADD VALIDATION CONSTRAINTS (Optional but recommended)
-- =====================================================

-- Ensure Spanish field is always present and not empty
ALTER TABLE tour_address 
    ADD CONSTRAINT tour_address_location_es_required 
    CHECK (location IS NULL OR (location->>'es' IS NOT NULL AND location->>'es' != ''));

ALTER TABLE tour_main_attractions 
    ADD CONSTRAINT tour_main_attractions_description_es_required 
    CHECK (description IS NULL OR (description->>'es' IS NOT NULL AND description->>'es' != ''));

ALTER TABLE tour_includes_excludes 
    ADD CONSTRAINT tour_includes_excludes_description_es_required 
    CHECK (description IS NULL OR (description->>'es' IS NOT NULL AND description->>'es' != ''));

ALTER TABLE tour_faq 
    ADD CONSTRAINT tour_faq_question_es_required 
    CHECK (question IS NULL OR (question->>'es' IS NOT NULL AND question->>'es' != ''));

ALTER TABLE tour_faq 
    ADD CONSTRAINT tour_faq_answer_es_required 
    CHECK (answer IS NULL OR (answer->>'es' IS NOT NULL AND answer->>'es' != ''));

ALTER TABLE tour_itinerary 
    ADD CONSTRAINT tour_itinerary_title_es_required 
    CHECK (title IS NULL OR (title->>'es' IS NOT NULL AND title->>'es' != ''));

ALTER TABLE tour_itinerary 
    ADD CONSTRAINT tour_itinerary_description_es_required 
    CHECK (description IS NULL OR (description->>'es' IS NOT NULL AND description->>'es' != ''));

ALTER TABLE tour_cancellation_policy 
    ADD CONSTRAINT tour_cancellation_policy_observations_es_required 
    CHECK (observations IS NULL OR (observations->>'es' IS NOT NULL AND observations->>'es' != ''));

ALTER TABLE tour_gallery 
    ADD CONSTRAINT tour_gallery_description_es_required 
    CHECK (description IS NULL OR (description->>'es' IS NOT NULL AND description->>'es' != ''));

ALTER TABLE tour 
    ADD CONSTRAINT tour_description_es_required 
    CHECK (description IS NULL OR (description->>'es' IS NOT NULL AND description->>'es' != ''));

-- =====================================================
-- 3. CREATE INDEXES FOR PERFORMANCE (Optional but recommended)
-- =====================================================

-- Create GIN indexes for efficient JSONB queries
CREATE INDEX IF NOT EXISTS idx_tour_address_location_gin ON tour_address USING GIN (location);
CREATE INDEX IF NOT EXISTS idx_tour_main_attractions_description_gin ON tour_main_attractions USING GIN (description);
CREATE INDEX IF NOT EXISTS idx_tour_includes_excludes_description_gin ON tour_includes_excludes USING GIN (description);
CREATE INDEX IF NOT EXISTS idx_tour_faq_question_gin ON tour_faq USING GIN (question);
CREATE INDEX IF NOT EXISTS idx_tour_faq_answer_gin ON tour_faq USING GIN (answer);
CREATE INDEX IF NOT EXISTS idx_tour_itinerary_title_gin ON tour_itinerary USING GIN (title);
CREATE INDEX IF NOT EXISTS idx_tour_itinerary_description_gin ON tour_itinerary USING GIN (description);
CREATE INDEX IF NOT EXISTS idx_tour_cancellation_policy_observations_gin ON tour_cancellation_policy USING GIN (observations);
CREATE INDEX IF NOT EXISTS idx_tour_gallery_description_gin ON tour_gallery USING GIN (description);
CREATE INDEX IF NOT EXISTS idx_tour_description_gin ON tour USING GIN (description);

-- Create indexes for specific language searches (Spanish)
CREATE INDEX IF NOT EXISTS idx_tour_address_location_es ON tour_address ((location->>'es'));
CREATE INDEX IF NOT EXISTS idx_tour_main_attractions_description_es ON tour_main_attractions ((description->>'es'));
CREATE INDEX IF NOT EXISTS idx_tour_includes_excludes_description_es ON tour_includes_excludes ((description->>'es'));
CREATE INDEX IF NOT EXISTS idx_tour_description_es ON tour ((description->>'es'));

-- Commit transaction
COMMIT;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Uncomment to verify the migration
/*
-- Check column types
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

-- Sample data check
SELECT id, location FROM tour_address LIMIT 5;
SELECT id, description FROM tour_main_attractions LIMIT 5;
SELECT id, question, answer FROM tour_faq LIMIT 5;
*/
