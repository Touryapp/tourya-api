# Cambios Realizados: Eliminación de min_capacity y max_capacity

## Resumen
Se han eliminado los campos `min_capacity` y `max_capacity` de la tabla `tour_schedule_config_slot` y de todas las entidades, DTOs y servicios relacionados.

## Archivos Modificados

### 1. Entidad Java - TourScheduleConfigSlot.java
**Ubicación:** `src/main/java/com/tourya/api/models/TourScheduleConfigSlot.java`

**Cambios:**
- ❌ Eliminado campo `minCapacity`
- ❌ Eliminado campo `maxCapacity`

### 2. DTO Request - TourScheduleConfigSlotDto.java
**Ubicación:** `src/main/java/com/tourya/api/models/request/TourScheduleConfigSlotDto.java`

**Cambios:**
- ❌ Eliminado campo `minCapacity`
- ❌ Eliminado campo `maxCapacity`
- ❌ Eliminada validación `@NotNull` para `maxCapacity`

### 3. DTO Response - TourScheduleSlotResponse.java
**Ubicación:** `src/main/java/com/tourya/api/models/responses/TourScheduleSlotResponse.java`

**Cambios:**
- ❌ Eliminado campo `minCapacity`
- ❌ Eliminado campo `maxCapacity`

### 4. Servicio - TourScheduleConfigGeneralService.java
**Ubicación:** `src/main/java/com/tourya/api/services/TourScheduleConfigGeneralService.java`

**Cambios:**
- ❌ Eliminadas todas las llamadas a `setMinCapacity()` y `setMaxCapacity()`
- ❌ Eliminadas todas las llamadas a `getMinCapacity()` y `getMaxCapacity()`
- Actualizado en los métodos:
  - `buildSlotsAndPricesFromRequest()` (líneas 102-107)
  - `manageSlotsUpdate()` (líneas 209-212)
  - `mapToTourScheduleConfigResponse()` (líneas 406-411)
  - `convertToTourScheduleConfigResponse()` (líneas 485-490)

### 5. DDL Principal - ddl.sql
**Ubicación:** `database/ddl.sql`

**Cambios:**
- ❌ Eliminada columna `min_capacity` de la definición de `tour_schedule_config_slot`
- ❌ Eliminada columna `max_capacity` de la definición de `tour_schedule_config_slot`

## Archivos Creados

### 1. Script de Migración - 005_remove_capacity_fields_from_slot.sql
**Ubicación:** `database/migrations/005_remove_capacity_fields_from_slot.sql`

Script para eliminar las columnas de la base de datos:
```sql
ALTER TABLE public.tour_schedule_config_slot DROP COLUMN IF EXISTS min_capacity;
ALTER TABLE public.tour_schedule_config_slot DROP COLUMN IF EXISTS max_capacity;
```

### 2. Script de Rollback - 005_rollback_capacity_fields_to_slot.sql
**Ubicación:** `database/migrations/005_rollback_capacity_fields_to_slot.sql`

Script para restaurar las columnas si es necesario.

## Cómo Aplicar los Cambios

### Opción 1: Ejecutar el script de migración
```bash
psql -U tu_usuario -d tu_base_de_datos -f database/migrations/005_remove_capacity_fields_from_slot.sql
```

### Opción 2: Ejecutar manualmente
```sql
ALTER TABLE public.tour_schedule_config_slot DROP COLUMN IF EXISTS min_capacity;
ALTER TABLE public.tour_schedule_config_slot DROP COLUMN IF EXISTS max_capacity;
```

## Impacto de los Cambios

### ✅ Cambios Completados
1. **Modelo de Datos**: Los campos ya no existen en la entidad JPA
2. **API**: Los DTOs de request y response ya no incluyen estos campos
3. **Lógica de Negocio**: El servicio ya no procesa ni asigna estos valores
4. **Base de Datos**: El DDL actualizado refleja la nueva estructura

### ⚠️ Consideraciones Importantes
1. **Datos Existentes**: Si tienes datos en estas columnas, se perderán al ejecutar la migración
2. **Backup**: Se recomienda hacer un backup antes de ejecutar la migración
3. **Rollback**: Existe un script de rollback, pero los datos no se recuperarán automáticamente

### 📋 Verificación Post-Migración

Después de aplicar la migración, verifica que todo funcione correctamente:

```sql
-- Verificar estructura de la tabla
SELECT column_name, data_type, is_nullable
FROM information_schema.columns 
WHERE table_name = 'tour_schedule_config_slot' 
ORDER BY ordinal_position;
```

Deberías ver solo estas columnas:
- `id`
- `config_id`
- `start_time`
- `end_time`
- `created_by`
- `last_modified_by`
- `created_date`
- `last_modified_date`

## Notas Adicionales

- Los campos `min_capacity` y `max_capacity` ya no son necesarios en el modelo de slots
- La capacidad ahora se maneja a nivel de `tour_schedule` o `tour_schedule_config`
- No se requieren cambios adicionales en otros servicios o controladores
