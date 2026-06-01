-- Migration: Usuarios por proveedor, roles operador/backoffice, asignación tour-operador

INSERT INTO public."role" (name, created_date)
SELECT 'PROVIDER_OPERATOR', NOW()
WHERE NOT EXISTS (SELECT 1 FROM public."role" WHERE name = 'PROVIDER_OPERATOR');

INSERT INTO public."role" (name, created_date)
SELECT 'BACKOFFICE_OPERATION', NOW()
WHERE NOT EXISTS (SELECT 1 FROM public."role" WHERE name = 'BACKOFFICE_OPERATION');

CREATE TABLE IF NOT EXISTS public.provider_user (
    id SERIAL PRIMARY KEY,
    provider_id INT4 NOT NULL REFERENCES public.provider(id) ON DELETE CASCADE,
    user_id INT4 NOT NULL UNIQUE REFERENCES public._user(id) ON DELETE CASCADE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP NULL,
    created_by INT4 NULL,
    last_modified_by INT4 NULL,
    CONSTRAINT provider_user_provider_user_unique UNIQUE (provider_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_provider_user_provider_id ON public.provider_user(provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_user_user_id ON public.provider_user(user_id);

COMMENT ON TABLE public.provider_user IS
    'Relación 1 proveedor : N usuarios. Cada usuario pertenece a un solo proveedor.';

-- Usuario principal del proveedor (owner registrado)
INSERT INTO public.provider_user (provider_id, user_id, is_primary, created_date)
SELECT p.id, p.user_id, TRUE, COALESCE(p.created_date, NOW())
FROM public.provider p
WHERE p.user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM public.provider_user pu
      WHERE pu.user_id = p.user_id
  );

CREATE TABLE IF NOT EXISTS public.provider_user_tour (
    id SERIAL PRIMARY KEY,
    provider_user_id INT4 NOT NULL REFERENCES public.provider_user(id) ON DELETE CASCADE,
    tour_id INT4 NOT NULL REFERENCES public.tour(id) ON DELETE CASCADE,
    is_principal BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT provider_user_tour_unique UNIQUE (provider_user_id, tour_id)
);

CREATE INDEX IF NOT EXISTS idx_provider_user_tour_tour_id ON public.provider_user_tour(tour_id);

COMMENT ON TABLE public.provider_user_tour IS
    'Tours asignados a un operador del proveedor. Un operador puede ser principal en un tour.';
