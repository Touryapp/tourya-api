# Análisis de Impacto: Eliminación de min_age y max_age

## 📊 Resumen del Análisis

Se propone eliminar los campos `min_age` y `max_age` de la tabla `tour_schedule_config_price` y crear una tabla de configuración centralizada `age_range_config`.

## 🔍 Estado Actual

### Tabla Actual: `tour_schedule_config_price`
```sql
CREATE TABLE public.tour_schedule_config_price (
    id serial4 NOT NULL,
    slot_id int4 NOT NULL,
    age_type public."age_price_type_enum" NOT NULL,
    min_age int4 NOT NULL,  ← A ELIMINAR
    max_age int4 NOT NULL,  ← A ELIMINAR
    price numeric NOT NULL,
    created_by int4 NOT NULL,
    last_modified_by int4 NULL,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    last_modified_date timestamp NULL
);
```

### Enum Existente: `AgePriceType`
```java
public enum AgePriceType {
    ADULT("ADULT"),
    CHILD("CHILD"),
    INFANT("INFANT");
}
```

## 📁 Archivos Impactados

### 1. **Entidades y Modelos** (2 archivos)
- ✅ `TourScheduleConfigPrice.java` - Entidad JPA
  - Líneas 56, 59: campos `minAge` y `maxAge`
  
- ✅ `Tour.java` - Entidad Tour (NO AFECTADO - es diferente)
  - Línea 64: `minAge` del tour (edad mínima para el tour, no para precios)

### 2. **DTOs Request** (1 archivo)
- ✅ `TourScheduleConfigPriceDto.java`
  - Líneas 15-20: campos `minAge` y `maxAge` con validaciones

### 3. **DTOs Response** (3 archivos)
- ✅ `TourSchedulePriceResponse.java`
  - Líneas 12-13: campos `minAge` y `maxAge`
  
- ✅ `TourPriceOptionDto.java`
  - Líneas 12-13: campos `minAge` y `maxAge`
  
- ✅ `SearchTourScheduleFullResponse.java` (clase interna)
  - Líneas 94-95: campos `minAge` y `maxAge`

### 4. **Servicios** (1 archivo principal)
- ✅ `TourScheduleConfigGeneralService.java`
  - **6 referencias** a `setMinAge()` y `setMaxAge()`
  - **6 referencias** a `getMinAge()` y `getMaxAge()`
  - Métodos afectados:
    - `buildSlotsAndPricesFromRequest()` (líneas 112-113)
    - `updateSlotPrices()` (líneas 258-259)
    - `mapToTourScheduleConfigResponse()` (líneas 427-428)
    - `convertToTourScheduleConfigResponse()` (líneas 505-506)
    - `searchToursForReservation()` (líneas 612-613)

### 5. **Base de Datos**
- ✅ `ddl.sql`
  - Líneas 1260-1261: columnas `min_age` y `max_age`

## 🎯 Propuesta de Solución

### Nueva Tabla: `age_range_config`

```sql
CREATE TABLE public.age_range_config (
    id serial4 NOT NULL,
    age_type public.age_price_type_enum NOT NULL UNIQUE,
    min_age int4 NOT NULL,
    max_age int4 NOT NULL,
    description varchar(255) NULL,
    is_active bool DEFAULT true NOT NULL,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    last_modified_date timestamp NULL,
    created_by int4 NOT NULL,
    last_modified_by int4 NULL,
    CONSTRAINT age_range_config_pkey PRIMARY KEY (id),
    CONSTRAINT age_range_config_age_type_unique UNIQUE (age_type),
    CONSTRAINT age_range_config_age_check CHECK (min_age >= 0 AND max_age >= min_age)
);
```

### Datos Iniciales Sugeridos

```sql
INSERT INTO age_range_config (age_type, min_age, max_age, description, is_active, created_by) 
VALUES
('ADULT', 18, 99, 'Rango de edad para adultos', true, 1),
('CHILD', 3, 17, 'Rango de edad para niños', true, 1),
('INFANT', 0, 2, 'Rango de edad para infantes/bebés', true, 1);
```

## 📋 Cambios Necesarios

### 1. Base de Datos

#### Crear tabla de configuración
```sql
CREATE TABLE public.age_range_config (...);
INSERT INTO age_range_config VALUES (...);
```

#### Eliminar columnas de tour_schedule_config_price
```sql
ALTER TABLE public.tour_schedule_config_price 
DROP COLUMN min_age,
DROP COLUMN max_age;
```

### 2. Código Java

#### Nueva Entidad: `AgeRangeConfig.java`
```java
@Entity
@Table(name = "age_range_config")
public class AgeRangeConfig extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "age_type", unique = true, nullable = false)
    private AgePriceType ageType;
    
    @Column(name = "min_age", nullable = false)
    private Integer minAge;
    
    @Column(name = "max_age", nullable = false)
    private Integer maxAge;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
```

#### Nuevo Repositorio: `AgeRangeConfigRepository.java`
```java
public interface AgeRangeConfigRepository extends JpaRepository<AgeRangeConfig, Integer> {
    Optional<AgeRangeConfig> findByAgeType(AgePriceType ageType);
    List<AgeRangeConfig> findByIsActiveTrue();
}
```

#### Nuevo Servicio: `AgeRangeConfigService.java`
```java
@Service
public class AgeRangeConfigService {
    private final AgeRangeConfigRepository repository;
    
    public AgeRangeConfig getByAgeType(AgePriceType ageType) {
        return repository.findByAgeType(ageType)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Age range config not found for type: " + ageType));
    }
    
    public Map<AgePriceType, AgeRangeConfig> getAllAsMap() {
        return repository.findByIsActiveTrue().stream()
            .collect(Collectors.toMap(
                AgeRangeConfig::getAgeType,
                Function.identity()
            ));
    }
}
```

### 3. Modificar Entidades y DTOs

#### `TourScheduleConfigPrice.java`
- ❌ Eliminar campos `minAge` y `maxAge`
- ✅ Mantener solo `ageType` y `price`

#### `TourScheduleConfigPriceDto.java`
- ❌ Eliminar campos `minAge` y `maxAge`
- ✅ Mantener solo `ageType` y `price`

#### DTOs Response
- ✅ **MANTENER** `minAge` y `maxAge` en los DTOs de respuesta
- ✅ Poblar estos campos desde `AgeRangeConfig` al mapear

### 4. Modificar Servicios

#### `TourScheduleConfigGeneralService.java`

**Antes:**
```java
price.setMinAge(priceDto.getMinAge());
price.setMaxAge(priceDto.getMaxAge());
```

**Después:**
```java
// Ya no se setean, se obtienen de age_range_config al consultar
```

**Al mapear respuestas:**
```java
// Inyectar AgeRangeConfigService
private final AgeRangeConfigService ageRangeConfigService;

// Al mapear
TourSchedulePriceResponse priceDto = new TourSchedulePriceResponse();
priceDto.setId(price.getId());
priceDto.setAgeType(price.getAgeType());
priceDto.setPrice(price.getPrice());

// Obtener rangos de edad desde la configuración
AgeRangeConfig ageConfig = ageRangeConfigService.getByAgeType(price.getAgeType());
priceDto.setMinAge(ageConfig.getMinAge());
priceDto.setMaxAge(ageConfig.getMaxAge());
```

## ✅ Ventajas de este Enfoque

1. **Centralización**: Un solo lugar para gestionar rangos de edad
2. **Consistencia**: Todos los tours usan los mismos rangos
3. **Flexibilidad**: Cambios globales desde configuración
4. **Simplicidad**: No validar rangos en cada precio
5. **Mantenibilidad**: Más fácil de mantener
6. **Escalabilidad**: Fácil agregar nuevos tipos de edad
7. **Administración**: Panel admin para gestionar rangos

## ⚠️ Consideraciones

1. **Migración de Datos**: Los rangos actuales en BD se perderán
2. **Validación**: Asegurar que todos los `age_type` tengan configuración
3. **Performance**: JOIN adicional al consultar precios (mínimo impacto)
4. **Caché**: Considerar cachear `age_range_config` (raramente cambia)

## 📊 Impacto Estimado

- **Archivos a Modificar**: ~10 archivos
- **Archivos a Crear**: ~4 archivos nuevos
- **Scripts de Migración**: 2 (crear tabla + eliminar columnas)
- **Tiempo Estimado**: 2-3 horas
- **Riesgo**: BAJO (cambio bien definido)

## 🚀 Plan de Implementación

1. ✅ Crear tabla `age_range_config` y poblar datos
2. ✅ Crear entidad `AgeRangeConfig` y repositorio
3. ✅ Crear servicio `AgeRangeConfigService`
4. ✅ Modificar `TourScheduleConfigPrice` (eliminar campos)
5. ✅ Modificar DTOs Request (eliminar campos)
6. ✅ Modificar servicios (usar `AgeRangeConfigService`)
7. ✅ Mantener DTOs Response (poblar desde config)
8. ✅ Eliminar columnas de BD
9. ✅ Actualizar DDL
10. ✅ Testing y validación

¿Procedo con la implementación?
