-- =====================================================
-- Migration: 002_drop_age_columns_from_price.sql
-- Description: Eliminar columnas min_age y max_age de tour_schedule_config_price
-- Author: System
-- Date: 2026-02-13
-- IMPORTANTE: Ejecutar DESPUÉS de desplegar el código Java actualizado
-- =====================================================

-- =====================================================
-- VERIFICACIÓN PREVIA
-- =====================================================

-- Verificar que la tabla age_range_config existe
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
          AND table_name = 'age_range_config'
    ) THEN
        RAISE EXCEPTION 'La tabla age_range_config no existe. Ejecutar primero 001_create_age_range_config.sql';
    END IF;
END $$;

-- Verificar que hay datos en age_range_config
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM public.age_range_config) = 0 THEN
        RAISE EXCEPTION 'La tabla age_range_config está vacía. Insertar datos primero.';
    END IF;
END $$;

-- =====================================================
-- BACKUP DE DATOS (OPCIONAL - DESCOMENTAR SI SE DESEA)
-- =====================================================

-- CREATE TABLE IF NOT EXISTS public.tour_schedule_config_price_backup AS
-- SELECT * FROM public.tour_schedule_config_price;

-- =====================================================
-- PASO 1: Eliminar columnas min_age y max_age
-- =====================================================

-- Verificar que las columnas existen antes de eliminarlas
DO $$
BEGIN
    -- Eliminar min_age si existe
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'tour_schedule_config_price' 
          AND column_name = 'min_age'
    ) THEN
        ALTER TABLE public.tour_schedule_config_price DROP COLUMN min_age;
        RAISE NOTICE 'Columna min_age eliminada exitosamente';
    ELSE
        RAISE NOTICE 'Columna min_age ya no existe';
    END IF;

    -- Eliminar max_age si existe
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'tour_schedule_config_price' 
          AND column_name = 'max_age'
    ) THEN
        ALTER TABLE public.tour_schedule_config_price DROP COLUMN max_age;
        RAISE NOTICE 'Columna max_age eliminada exitosamente';
    ELSE
        RAISE NOTICE 'Columna max_age ya no existe';
    END IF;
END $$;

-- =====================================================
-- VERIFICACIÓN POST-MIGRACIÓN
-- =====================================================

-- Verificar que las columnas fueron eliminadas
SELECT 
    column_name,
    data_type
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'tour_schedule_config_price'
ORDER BY ordinal_position;

-- Verificar que age_type sigue existiendo
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'tour_schedule_config_price' 
          AND column_name = 'age_type'
    ) THEN
        RAISE EXCEPTION 'ERROR: La columna age_type no existe en tour_schedule_config_price';
    END IF;
    
    RAISE NOTICE 'Verificación exitosa: age_type existe, min_age y max_age eliminadas';
END $$;

-- =====================================================
-- ROLLBACK (si es necesario)
-- =====================================================

/*
-- IMPORTANTE: Solo usar si se necesita revertir la migración
-- Esto requiere que exista el backup

ALTER TABLE public.tour_schedule_config_price 
ADD COLUMN min_age int4 NOT NULL DEFAULT 0;

ALTER TABLE public.tour_schedule_config_price 
ADD COLUMN max_age int4 NOT NULL DEFAULT 99;

-- Restaurar datos desde backup si existe
-- UPDATE public.tour_schedule_config_price p
-- SET 
--     min_age = b.min_age,
--     max_age = b.max_age
-- FROM public.tour_schedule_config_price_backup b
-- WHERE p.id = b.id;

-- DROP TABLE IF EXISTS public.tour_schedule_config_price_backup;
*/
