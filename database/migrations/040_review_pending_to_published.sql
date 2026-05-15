-- Reseñas: el producto trata las reseñas visibles como PUBLISHED.
-- Histórico en PENDING impedía listados/resúmenes alineados con el detalle del tour.
UPDATE public.review
SET status = 'PUBLISHED'
WHERE status = 'PENDING';
