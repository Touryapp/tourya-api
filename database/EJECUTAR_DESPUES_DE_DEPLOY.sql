-- =====================================================
-- SCRIPT DE MIGRACIÓN: Constraint Único en tour_schedule
-- EJECUTAR ESTE SCRIPT DESPUÉS DE DESPLEGAR EL CÓDIGO
-- =====================================================
-- Fecha: 2025-12-21
-- Propósito: Garantizar que solo exista un schedule por (tour_id, schedule_date)
-- =====================================================

-- PASO 1: Verificar duplicados existentes
-- Ejecuta esto primero para ver qué duplicados tienes
SELECT 
    tour_id, 
    schedule_date, 
    COUNT(*) as cantidad_duplicados,
    STRING_AGG(id::text, ', ' ORDER BY id) as ids_schedules
FROM tour_schedule
GROUP BY tour_id, schedule_date
HAVING COUNT(*) > 1
ORDER BY tour_id, schedule_date;

-- =====================================================
-- PASO 2: BACKUP (RECOMENDADO)
-- =====================================================
-- Descomenta y ejecuta si quieres hacer un backup
-- CREATE TABLE tour_schedule_backup_20251221 AS 
-- SELECT * FROM tour_schedule;

-- =====================================================
-- PASO 3: Actualizar referencias en shopping_cart_item
-- =====================================================
-- Esto actualiza los items del carrito para que apunten al schedule que vamos a mantener
-- (el de ID más alto para cada combinación tour_id + schedule_date)

UPDATE shopping_cart_item sci
SET tour_schedule_id = keeper.max_id
FROM (
    SELECT 
        ts.tour_id,
        ts.schedule_date,
        MAX(ts.id) as max_id
    FROM tour_schedule ts
    GROUP BY ts.tour_id, ts.schedule_date
    HAVING COUNT(*) > 1  -- Solo los que tienen duplicados
) keeper
INNER JOIN tour_schedule ts_old 
    ON ts_old.tour_id = keeper.tour_id 
    AND ts_old.schedule_date = keeper.schedule_date
    AND ts_old.id != keeper.max_id  -- Los que vamos a eliminar
WHERE sci.tour_schedule_id = ts_old.id;

-- Verificar cuántos registros se actualizaron
-- SELECT COUNT(*) as registros_actualizados FROM shopping_cart_item 
-- WHERE tour_schedule_id IN (
--     SELECT id FROM tour_schedule ts
--     WHERE id NOT IN (
--         SELECT MAX(id) FROM tour_schedule GROUP BY tour_id, schedule_date
--     )
-- );

-- =====================================================
-- PASO 4: Actualizar referencias en tour_reservation
-- =====================================================
-- Actualizar las reservas para que apunten al schedule que vamos a mantener

UPDATE tour_reservation tr
SET schedule_id = keeper.max_id
FROM (
    SELECT 
        ts.tour_id,
        ts.schedule_date,
        MAX(ts.id) as max_id
    FROM tour_schedule ts
    GROUP BY ts.tour_id, ts.schedule_date
    HAVING COUNT(*) > 1  -- Solo los que tienen duplicados
) keeper
INNER JOIN tour_schedule ts_old 
    ON ts_old.tour_id = keeper.tour_id 
    AND ts_old.schedule_date = keeper.schedule_date
    AND ts_old.id != keeper.max_id  -- Los que vamos a eliminar
WHERE tr.schedule_id = ts_old.id;

-- Verificar cuántas reservas se actualizaron
SELECT COUNT(*) as reservas_actualizadas 
FROM tour_reservation 
WHERE schedule_id IN (
    SELECT id FROM tour_schedule ts
    WHERE id NOT IN (
        SELECT MAX(id) FROM tour_schedule GROUP BY tour_id, schedule_date
    )
);

-- =====================================================
-- PASO 5: Actualizar otras referencias si existen
-- =====================================================
-- Si tienes otras tablas que referencian tour_schedule, agrégalas aquí
-- Ejemplo para reservation (tabla diferente):

-- UPDATE reservation r
-- SET item_id = keeper.max_id
-- FROM (
--     SELECT 
--         ts.tour_id,
--         ts.schedule_date,
--         MAX(ts.id) as max_id
--     FROM tour_schedule ts
--     GROUP BY ts.tour_id, ts.schedule_date
--     HAVING COUNT(*) > 1
-- ) keeper
-- INNER JOIN tour_schedule ts_old 
--     ON ts_old.tour_id = keeper.tour_id 
--     AND ts_old.schedule_date = keeper.schedule_date
--     AND ts_old.id != keeper.max_id
-- WHERE r.item_id = ts_old.id;

-- =====================================================
-- PASO 6: Limpiar duplicados
-- =====================================================
-- Ahora sí podemos eliminar los duplicados sin violar foreign keys
DELETE FROM tour_schedule ts
WHERE ts.id NOT IN (
    SELECT MAX(id)
    FROM tour_schedule
    GROUP BY tour_id, schedule_date
);

-- =====================================================
-- PASO 6: Verificar que no queden duplicados
-- =====================================================
-- Esto debe retornar 0 filas
SELECT 
    tour_id, 
    schedule_date, 
    COUNT(*) as count
FROM tour_schedule
GROUP BY tour_id, schedule_date
HAVING COUNT(*) > 1;

-- =====================================================
-- PASO 7: Agregar constraint único
-- =====================================================
ALTER TABLE public.tour_schedule 
ADD CONSTRAINT uq_tour_schedule_tour_date 
UNIQUE (tour_id, schedule_date);

-- =====================================================
-- PASO 8: Crear índice para mejor performance
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_tour_schedule_tour_date 
ON public.tour_schedule (tour_id, schedule_date);

-- =====================================================
-- PASO 9: Verificar que el constraint se creó
-- =====================================================
SELECT 
    conname as nombre_constraint,
    contype as tipo_constraint,
    pg_get_constraintdef(oid) as definicion
FROM pg_constraint
WHERE conrelid = 'tour_schedule'::regclass
    AND conname = 'uq_tour_schedule_tour_date';

-- =====================================================
-- RESULTADO ESPERADO:
-- =====================================================
-- nombre_constraint          | tipo_constraint | definicion
-- ---------------------------|-----------------|----------------------------------
-- uq_tour_schedule_tour_date | u               | UNIQUE (tour_id, schedule_date)

-- =====================================================
-- ROLLBACK (si necesitas revertir los cambios)
-- =====================================================
-- Descomenta estas líneas SOLO si necesitas hacer rollback

-- -- 1. Eliminar el constraint único
-- ALTER TABLE public.tour_schedule 
-- DROP CONSTRAINT IF EXISTS uq_tour_schedule_tour_date;

-- -- 2. Eliminar el índice
-- DROP INDEX IF EXISTS idx_tour_schedule_tour_date;

-- -- 3. Restaurar desde backup (si creaste uno)
-- -- TRUNCATE tour_schedule;
-- -- INSERT INTO tour_schedule SELECT * FROM tour_schedule_backup_20251221;

-- =====================================================
-- NOTAS IMPORTANTES:
-- =====================================================
-- 1. Este script debe ejecutarse DESPUÉS de desplegar el nuevo código
-- 2. El constraint garantiza que no se puedan crear duplicados en el futuro
-- 3. El PASO 3 actualiza las referencias en shopping_cart_item antes de eliminar
-- 4. Si tienes otras tablas que referencian tour_schedule, agrégalas en PASO 4
-- 5. El índice mejora el performance de búsquedas por tour_id y schedule_date

-- =====================================================
-- VERIFICACIÓN ADICIONAL: Ver qué tablas referencian tour_schedule
-- =====================================================
-- Ejecuta esto para ver todas las foreign keys que apuntan a tour_schedule:
-- SELECT
--     tc.table_name as tabla_que_referencia,
--     kcu.column_name as columna,
--     ccu.table_name AS tabla_referenciada
-- FROM information_schema.table_constraints AS tc
-- JOIN information_schema.key_column_usage AS kcu
--     ON tc.constraint_name = kcu.constraint_name
-- JOIN information_schema.constraint_column_usage AS ccu
--     ON ccu.constraint_name = tc.constraint_name
-- WHERE tc.constraint_type = 'FOREIGN KEY' 
--     AND ccu.table_name = 'tour_schedule';
