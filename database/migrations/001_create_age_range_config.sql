-- =====================================================
-- Migration: 001_create_age_range_config.sql
-- Description: Crear tabla de configuración centralizada de rangos de edad
-- Author: System
-- Date: 2026-02-13
-- =====================================================

-- =====================================================
-- PASO 1: Crear tabla age_range_config
-- =====================================================

CREATE TABLE IF NOT EXISTS public.age_range_config (
    id serial4 NOT NULL,
    age_type varchar(20) NOT NULL,
    min_age int4 NOT NULL,
    max_age int4 NOT NULL,
    description varchar(255) NULL,
    is_active bool NOT NULL DEFAULT true,
    created_date timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date timestamptz NULL,
    created_by int4 NOT NULL,
    last_modified_by int4 NULL,
    
    CONSTRAINT age_range_config_pkey PRIMARY KEY (id),
    CONSTRAINT age_range_config_age_type_unique UNIQUE (age_type),
    CONSTRAINT age_range_config_age_check CHECK (min_age >= 0 AND max_age >= min_age)
);

-- Índices para mejorar performance
CREATE INDEX IF NOT EXISTS age_range_config_age_type_idx ON public.age_range_config USING btree (age_type);
CREATE INDEX IF NOT EXISTS age_range_config_is_active_idx ON public.age_range_config USING btree (is_active);

COMMENT ON TABLE public.age_range_config IS 'Configuración centralizada de rangos de edad para precios de tours';
COMMENT ON COLUMN public.age_range_config.age_type IS 'Tipo de edad: ADULT, CHILD, INFANT';
COMMENT ON COLUMN public.age_range_config.min_age IS 'Edad mínima del rango (inclusive)';
COMMENT ON COLUMN public.age_range_config.max_age IS 'Edad máxima del rango (inclusive)';
COMMENT ON COLUMN public.age_range_config.is_active IS 'Indica si la configuración está activa';

-- =====================================================
-- PASO 2: Insertar datos iniciales
-- =====================================================

INSERT INTO public.age_range_config 
    (age_type, min_age, max_age, description, is_active, created_by, created_date) 
VALUES 
    ('ADULT', 18, 99, 'Adultos mayores de 18 años', true, 1, CURRENT_TIMESTAMP),
    ('CHILD', 3, 17, 'Niños de 3 a 17 años', true, 1, CURRENT_TIMESTAMP),
    ('INFANT', 0, 2, 'Infantes de 0 a 2 años', true, 1, CURRENT_TIMESTAMP)
ON CONFLICT (age_type) DO NOTHING;

-- =====================================================
-- VERIFICACIÓN
-- =====================================================

-- Verificar que se creó la tabla
SELECT 
    table_name, 
    table_type 
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_name = 'age_range_config';

-- Verificar datos insertados
SELECT 
    id,
    age_type,
    min_age,
    max_age,
    description,
    is_active
FROM public.age_range_config
ORDER BY min_age;

-- =====================================================
-- ROLLBACK (si es necesario)
-- =====================================================

-- DROP TABLE IF EXISTS public.age_range_config CASCADE;
