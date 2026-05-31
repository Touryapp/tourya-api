-- La dirección completa (p. ej. desde Google Maps) supera varchar(50)
ALTER TABLE public.provider
    ALTER COLUMN address TYPE varchar(255);

COMMENT ON COLUMN public.provider.address IS 'Dirección del proveedor (hasta 255 caracteres).';
