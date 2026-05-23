-- Registro Nacional de Turismo (RNT) del proveedor
ALTER TABLE public.provider
    ADD COLUMN IF NOT EXISTS rnt varchar(50) NULL;

COMMENT ON COLUMN public.provider.rnt IS 'Número de Registro Nacional de Turismo (RNT)';
