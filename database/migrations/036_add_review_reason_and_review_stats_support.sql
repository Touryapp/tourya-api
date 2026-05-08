-- Migration: Add review reason fields
-- Date: 2026-04-26

ALTER TABLE public.review
ADD COLUMN IF NOT EXISTS reason_type varchar(10) NULL;

ALTER TABLE public.review
ADD COLUMN IF NOT EXISTS reason_id int4 NULL;

CREATE INDEX IF NOT EXISTS idx_review_tour_status
ON public.review (tour_id, status);

CREATE INDEX IF NOT EXISTS idx_review_tour_status_reason
ON public.review (tour_id, status, reason_type, reason_id);

