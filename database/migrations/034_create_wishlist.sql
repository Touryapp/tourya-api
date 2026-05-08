-- Migration: Create wishlist (tour likes) per user
-- Date: 2026-04-26

CREATE TABLE IF NOT EXISTS public.user_wishlist (
    user_id int4 NOT NULL REFERENCES public._user(id) ON DELETE CASCADE,
    tour_id int4 NOT NULL REFERENCES public.tour(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, tour_id)
);

CREATE INDEX IF NOT EXISTS idx_user_wishlist_user_id
ON public.user_wishlist (user_id);

