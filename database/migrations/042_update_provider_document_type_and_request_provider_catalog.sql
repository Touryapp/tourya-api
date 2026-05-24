-- Migration 042: documentType del proveedor (NIT/CC) y catálogo de documentos de solicitud
-- Date: 2026-05-23

BEGIN;

-- 1) provider.document_type: reemplazar valor legacy RNT por CC (tipo de identificación)
UPDATE public.provider
SET document_type = 'CC'
WHERE document_type = 'RNT';

-- 2) Ampliar name para nuevos tipos de documento de solicitud
ALTER TABLE public.request_provider_document_type
    ALTER COLUMN "name" TYPE varchar(120) USING LEFT("name"::text, 120);

-- 3) Liberar el nombre 'Other' (unique) que hoy está en id=3, crear id=8 y mover referencias
UPDATE public.request_provider_document_type
SET name = '_mig042_other_legacy_'
WHERE id = 3
  AND name = 'Other';

INSERT INTO public.request_provider_document_type (id, "name", mandatory, created_date, created_by, description)
SELECT
    8,
    'Other',
    false,
    CURRENT_TIMESTAMP,
    COALESCE((SELECT MIN(u.id) FROM public._user u), 1),
    'Other documents'
WHERE NOT EXISTS (SELECT 1 FROM public.request_provider_document_type WHERE id = 8);

UPDATE public.request_provider_gallery
SET document_type_id = 8
WHERE document_type_id = 3;

-- 4) Catálogo completo de documentos para solicitud de proveedor
INSERT INTO public.request_provider_document_type (id, "name", mandatory, created_date, created_by, description)
VALUES
    (1, 'Cámara de comercio', true, CURRENT_TIMESTAMP, COALESCE((SELECT MIN(u.id) FROM public._user u), 1), 'Certificado de Cámara de comercio'),
    (2, 'RUT', true, CURRENT_TIMESTAMP, COALESCE((SELECT MIN(u.id) FROM public._user u), 1), 'Registro Único Tributario (RUT)'),
    (3, 'RNT', true, CURRENT_TIMESTAMP, COALESCE((SELECT MIN(u.id) FROM public._user u), 1), 'Registro Nacional de Turismo (RNT)'),
    (4, 'Cédula del representante legal', true, CURRENT_TIMESTAMP, COALESCE((SELECT MIN(u.id) FROM public._user u), 1), 'Documento de identidad del representante legal'),
    (5, 'Seguros de operación', true, CURRENT_TIMESTAMP, COALESCE((SELECT MIN(u.id) FROM public._user u), 1), 'Pólizas de seguros de operación vigentes'),
    (6, 'Contrato de mandato (firmado)', true, CURRENT_TIMESTAMP, COALESCE((SELECT MIN(u.id) FROM public._user u), 1), 'Contrato de mandato debidamente firmado'),
    (7, 'Contrato de vinculación (firmado)', true, CURRENT_TIMESTAMP, COALESCE((SELECT MIN(u.id) FROM public._user u), 1), 'Contrato de vinculación debidamente firmado'),
    (8, 'Other', false, CURRENT_TIMESTAMP, COALESCE((SELECT MIN(u.id) FROM public._user u), 1), 'Other documents')
ON CONFLICT (id) DO UPDATE
SET
    "name" = EXCLUDED."name",
    description = EXCLUDED.description,
    mandatory = EXCLUDED.mandatory,
    last_modified_date = CURRENT_TIMESTAMP;

SELECT setval(
    'request_provider_document_type_id_seq',
    (SELECT COALESCE(MAX(id), 1) FROM public.request_provider_document_type)
);

COMMIT;
