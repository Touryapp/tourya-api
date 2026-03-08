-- Migration: Add user_id column to credit table
-- Date: 2026-03-05
-- Description: Adds user_id column to credit table to directly link credits to users.
--              This simplifies credit queries and ensures credits are always associated with the correct user.

-- Step 1: Add user_id column
ALTER TABLE public.credit 
ADD COLUMN user_id int4 NULL;

-- Step 2: Update existing credits with user_id = created_by
-- Para la migración de datos existentes, usamos created_by como user_id
UPDATE public.credit 
SET user_id = created_by;

-- Step 3: Verify that all credits have user_id before making it NOT NULL
-- If there are credits without user_id, this will show an error
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.credit WHERE user_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot set user_id to NOT NULL: there are credits without user_id. Please check the UPDATE query.';
    END IF;
END $$;

-- Step 4: Make user_id NOT NULL after populating existing data
ALTER TABLE public.credit 
ALTER COLUMN user_id SET NOT NULL;

-- Step 5: Add foreign key constraint
ALTER TABLE public.credit 
ADD CONSTRAINT fk_credit_user 
FOREIGN KEY (user_id) 
REFERENCES public._user(id) 
ON DELETE CASCADE;

-- Step 6: Add index for better query performance
CREATE INDEX idx_credit_user_id ON public.credit USING btree (user_id);

-- Step 7: Add comment
COMMENT ON COLUMN public.credit.user_id IS 'ID del usuario propietario del crédito';
