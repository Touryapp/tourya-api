-- Migration: Create tourist_profile (user tourist data)
-- Date: 2026-04-26
-- Description: Stores tourist personal/contact/address data and photo for each user (1-1).

CREATE TABLE IF NOT EXISTS public.tourist_profile (
    id bigserial PRIMARY KEY,
    user_id int4 NOT NULL UNIQUE REFERENCES public._user(id) ON DELETE CASCADE,
    first_name varchar(120) NULL,
    last_name varchar(120) NULL,
    document_number varchar(64) NULL,
    phone varchar(32) NULL,
    email varchar(320) NULL,
    city varchar(120) NULL,
    state varchar(120) NULL,
    country varchar(120) NULL,
    photo_url text NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tourist_profile_user_id
ON public.tourist_profile (user_id);

