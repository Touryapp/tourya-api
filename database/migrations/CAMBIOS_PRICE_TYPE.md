# Cambios Realizados: Campo price_type en Tour

## Resumen
Se ha agregado un nuevo campo `price_type` (tipo de precio) a la tabla `tour` con un enumerado que contiene los valores `individual` y `grupo`.

## Archivos Creados/Modificados

### 1. Enum Java - PriceTypeEnum.java
**Ubicación:** `src/main/java/com/tourya/api/constans/enums/PriceTypeEnum.java`

Enumerado Java con dos valores:
- `INDIVIDUAL` ("individual")
- `GRUPO` ("grupo")

### 2. Converter Java - PriceTypeEnumConverter.java
**Ubicación:** `src/main/java/com/tourya/api/constans/enums/PriceTypeEnumConverter.java`

Convertidor JPA para mapear el enum Java a la base de datos y viceversa.

### 3. Entidad Tour - Tour.java
**Ubicación:** `src/main/java/com/tourya/api/models/Tour.java`

Se agregó el campo:
```java
@Convert(converter = PriceTypeEnumConverter.class)
@Column(name = "price_type")
private PriceTypeEnum priceType;
```

### 4. Script de Migración - 004_add_price_type_to_tour.sql
**Ubicación:** `database/migrations/004_add_price_type_to_tour.sql`

Script para aplicar los cambios en la base de datos:
- Crea el tipo enum `price_type_enum` con valores 'individual' y 'grupo'
- Agrega la columna `price_type` a la tabla `tour`
- Incluye comentarios y pasos opcionales para establecer valores por defecto

### 5. Script de Rollback - 004_rollback_price_type_from_tour.sql
**Ubicación:** `database/migrations/004_rollback_price_type_from_tour.sql`

Script para revertir los cambios si es necesario:
- Elimina la columna `price_type`
- Elimina el tipo enum `price_type_enum`

### 6. DDL Principal - ddl.sql
**Ubicación:** `database/ddl.sql`

Se actualizó el archivo DDL principal con:
- Definición del tipo enum `price_type_enum`
- Columna `price_type` en la definición de la tabla `tour`

## Cómo Aplicar los Cambios

### Opción 1: Ejecutar el script de migración
```bash
psql -U tu_usuario -d tu_base_de_datos -f database/migrations/004_add_price_type_to_tour.sql
```

### Opción 2: Ejecutar manualmente
```sql
-- Crear el enum
CREATE TYPE price_type_enum AS ENUM ('individual', 'grupo');

-- Agregar la columna
ALTER TABLE public.tour 
ADD COLUMN price_type price_type_enum NULL;
```

## Uso en la Aplicación

Ahora puedes usar el campo `priceType` en tu entidad Tour:

```java
Tour tour = new Tour();
tour.setPriceType(PriceTypeEnum.INDIVIDUAL);
// o
tour.setPriceType(PriceTypeEnum.GRUPO);
```

## Valores Posibles
- `individual`: Para tours con precio individual
- `grupo`: Para tours con precio grupal

## Notas Importantes
1. El campo es **nullable** (puede ser NULL)
2. Si deseas hacerlo obligatorio, descomenta las líneas opcionales en el script de migración
3. Si tienes registros existentes, considera establecer un valor por defecto antes de hacer el campo NOT NULL
