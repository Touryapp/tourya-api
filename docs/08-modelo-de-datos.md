# 08 — Modelo de datos

Schema PostgreSQL de Tourya: 68 tablas, 91 migraciones, ~20 stored procedures, índices, JSONB para i18n.

> Referencia complementaria: [04 — Entidades de dominio](04-entidades-dominio.md) para el lado JPA.

---

## Métricas generales

| Métrica | Valor |
|---------|-------|
| Total tablas | 68 |
| Total migraciones aplicadas | 91 (numeradas `001_` a `091_`) |
| Stored procedures de negocio | ~14 |
| UUID helper functions | 6 (extensión `uuid-ossp`) |
| Total índices | ~52 |
| Tablas con columnas JSONB | 12 |
| Tipos ENUM nativos PostgreSQL | 11+ (incluye `tour_subcategory_enum` en `tour_schedule_config` desde migración 086) |

---

## Convenciones

- **Naming**: snake_case en tablas y columnas. Prefijo de dominio cuando aplica (`tour_`, `provider_`, `reservation_`).
- **Auditoría**: la mayoría de tablas tienen `created_date`, `last_modified_date`, `created_by`, `last_modified_by` (inyectado por `@EnableJpaAuditing`).
- **Sin soft deletes** ni `deleted_at` columns.
- **Sin multi-tenancy** (un solo tenant).
- **Hibernate `ddl-auto=none`** — schema gestionado manualmente.

---

## Catálogo de tablas por dominio

### Auth y usuarios (5 tablas)

| Tabla | Propósito |
|-------|-----------|
| `_user` | Cuenta principal (note: prefijo `_` por palabra reservada en SQL) |
| `role` | Roles (USER, PROVIDER, PROVIDER_OPERATOR, ADMIN, BACKOFFICE_OPERATION) |
| `user_roles` | M2M user ↔ role |
| `token` | Tokens de activación (6 dígitos, 15 min) |
| `provider_user` | Vínculo proveedor ↔ usuario (incluye sub-usuarios) |

### Geografía (5 tablas)

| Tabla | Propósito |
|-------|-----------|
| `country` | Países |
| `state` | Departamentos (Colombia tiene 32) |
| `city` | Municipios |
| `tour_address` | Direcciones de tours (encuentro, finalización, recogida, **Hotel Pickup**) |
| `provider` | Datos del operador (incluye dirección, RNT, NIT) |

> 📌 **Cambio TC-017 (#220) — Hotel Pickup** (migración 087, 2026-08-11):
> - `tour_address.country_id`, `state_id`, `city_id` pasan de `NOT NULL` a **NULLABLE**.
> - Nuevo `CHECK constraint tour_address_geo_required_unless_hotel_pickup`: solo `address_type = 'Hotel Pickup'` admite los 3 geo IDs en NULL. Cualquier otro valor de `address_type` mantiene la exigencia previa (los 3 NOT NULL).
> - Nuevo valor `HOTEL_PICKUP` en el enum `address_type` (display value: `"Hotel Pickup"`). Ver [RN — Hotel Pickup en doc 05](05-reglas-de-negocio.md) y [TC-017 en doc 17](17-backlog-implementacion.md).

### Tour core (8 tablas)

> 📌 **Cambios en la tabla `tour`** (aportados por Luis, 2026-06-28 / 2026-07-07):
> - ✅ Campo **`porcentaje_tourya`** (español, no `percentageTourya`): reactivado 2026-07-09 (PR #163). Existía como DEPRECATED desde 050; ahora es el % Tourya default del tour, se hereda al crear cada slot nuevo. DEFAULT = 0.15. Ver [RN-015](05-reglas-de-negocio.md).
> - ✅ Campo `isUnlimitedCapacity` **eliminado** de `tour_schedule` y `tour_schedule_config` desde la migración 029 (2026-04-08). La fuente de verdad es `Tour.isUnlimitedCapacity`. Ver [RN-021](05-reglas-de-negocio.md).

| Tabla | Propósito |
|-------|-----------|
| `tour` | Tour principal (`name`, `description` JSONB, status) |
| `tour_category` | Categorías de tour |
| `tour_tag` | Tags simples por tour |
| `tour_tag_mapping` | M2M tour ↔ tag |
| `tour_business_category` | Categorías de negocio (i18n) |
| `tour_business_subcategory_mapping` | Subcategorías de negocio |
| `tags` | Catálogo nuevo con i18n JSONB |
| `tag_dimensions` | Metadata de tags |

### Tour content (8 tablas)

| Tabla | Propósito | i18n |
|-------|-----------|------|
| `tour_gallery` | Imágenes con orden | sí (description) |
| `tour_main_attractions` | Atracciones destacadas | sí |
| `tour_includes_excludes` | Lo que incluye / no incluye | sí |
| `tour_itinerary` | Plan día a día | sí (title + description) |
| `tour_faq` | FAQ | sí (question + answer) |
| `tour_cancellation_policy` | Políticas de cancelación | sí (observations) |
| `tour_price` | (legacy, mayormente reemplazado) | — |
| `tour_status_history` | Historial de cambios de estado | — |

### Scheduling y precios (6 tablas)

| Tabla | Propósito |
|-------|-----------|
| `tour_schedule` | Instancia concreta del tour en una fecha |
| `tour_schedule_config` | Plantilla reutilizable (**TC-019**: nueva columna `sub_category tour_subcategory_enum NULL` — migración 086) |
| `tour_schedule_config_slot` | Franja horaria (capacity, bookings) |
| `tour_schedule_config_price` | Precio por ageType (ADULT/CHILD/INFANT) |
| `tour_schedule_price_override` | Override de precio por slot/fecha |
| `tour_schedule_slot_price_override` | (variante) override slot |

> 📌 **Cambio TC-019 (#231)** (migración 086, 2026-08-10): `tour_schedule_config.sub_category` permite filtrar templates por subcategoría del tour desde el combo del PROVIDER. Templates existentes quedan con NULL (no filtrable hasta que el PROVIDER edite el template y elija la subcat). `get_templates_by_provider` acepta parámetro `p_sub_category` opcional; templates con `sub_category = NULL` **se ocultan** cuando se pasa el filtro (no aplican a ninguna subcat).
>
> 📌 **TC-019 R1/R2 — backfill de márgenes** (migración 088, 2026-08-11): 444 slots tenían `slot_porcentaje_tourya = 0` (drift heredado — BE-02 solo hereda `tour.porcentaje_tourya` en slots NUEVOS). La migración 088 hace backfill al margen default del tour cuando el slot está en 0 y el tour tiene margen > 0, y recalcula `tour_schedule_config_price.price = provider_price * (1 + slot_pct)` en 783 filas con drift acumulado. Idempotente.

### Carrito y reservas (7 tablas)

| Tabla | Propósito |
|-------|-----------|
| `shopping_cart` | Carrito del turista |
| `shopping_cart_item` | Item del carrito |
| `shopping_cart_item_detail` | Detalle por ageType |
| `reservation` | Reserva confirmada (modelo principal) |
| `tour_reservation` | (legacy) |
| `tour_reservation_detail` | (legacy) |
| `tour_reservation_status_history` | (legacy) |

### Pagos y créditos (4 tablas)

| Tabla | Propósito |
|-------|-----------|
| `payment` | Transacción Wompi |
| `payment_credit` | Link payment ↔ credit |
| `credit` | Crédito a favor del turista. **TC-022** (mig 091): 3 columnas nuevas `refund_requested_at TIMESTAMPTZ NULL`, `refunded_at TIMESTAMPTZ NULL`, `refund_proof_url VARCHAR(500) NULL` para el flujo de devolución en efectivo — ver [RN-062](05-reglas-de-negocio.md#rn-062) |
| `account_payable` | Cuenta por pagar al provider |

> 📌 **Cambio TC-022 (#253)** (migración 091, 2026-08-13): flujo de devolución en efectivo de crédito. Además de las 3 columnas nuevas en `credit`, la migración agrega los estados `REFUND_REQUESTED` y `REFUNDED` al catálogo del CHECK constraint `credit_status_check` (transiciones `CREATED → REFUND_REQUESTED` vía turista, `REFUND_REQUESTED → REFUNDED` vía ADMIN/BACKOFFICE con comprobante). **Bonus fix incluido en la misma migración**: la migración 074 (BE-19) empezó a persistir `'EXPIRED'` en `credit.status` sin actualizar el CHECK — la 091 normaliza el catálogo completo (`CREATED, RESERVED, CONSUMED, CANCELED, DELETED, EXPIRED, REFUND_REQUESTED, REFUNDED`) cerrando esa deuda latente. Ver también [RN-062](05-reglas-de-negocio.md#rn-062) y la sección "Credit status enum" más abajo.

### Payouts (4 tablas)

| Tabla | Propósito |
|-------|-----------|
| `provider_payout_order` | Orden de pago semanal |
| `provider_payout_order_reservation` | Reservas incluidas |
| `provider_payout_attachment` | Comprobante de pago (PDF/imagen) |
| `provider_user_tour` | Tours asignados a sub-usuarios |

### Reviews (4 tablas)

| Tabla | Propósito | i18n |
|-------|-----------|------|
| `review` | Reseña del turista | sí (comment) |
| `review_answer` | Respuesta del provider | sí (comment) |
| `review_attachment` | Fotos de la reseña | — |
| `review_answer_attachment` | Fotos de la respuesta | — |

### Perfiles y wishlists (2 tablas)

| Tabla | Propósito |
|-------|-----------|
| `tourist_profile` | Perfil extendido del turista (1:1 con `_user`). **TC-009**: nueva columna `document_type varchar(50) NULL` — migración 083 |
| `user_wishlist` | Tours favoritos (composite PK userId + tourId) |

> 📌 **Cambio TC-009 (#196)** (migración 083, 2026-08-06): `tourist_profile.document_type` persiste el tipo de documento (`CC/CE/PP/NIT/TI`) para pre-llenar el checkout. Antes vivía solo en `payment.payer_document_type` y se re-preguntaba en cada compra. Sin enum: `varchar(50)` para compat con la columna homónima en `payment`. Frontend consumido en TC-020 #235 (PR #105) para mandar el `docType` real a Wompi (antes hardcodeaba `"CC"`).

### DIMAR / Maritime (1 tabla)

| Tabla | Propósito |
|-------|-----------|
| `maritim_activity_report` | Reporte de actividad marítima |

### Configuración y misc (8 tablas)

| Tabla | Propósito | i18n |
|-------|-----------|------|
| `app_config` | Key-value config | sí (config_value JSONB) |
| `service` | Servicios adicionales (mayormente sin usar) | — |
| `service_type` | Tipos de servicio | — |
| `tour_cancel_category` | Categorías de cancelación | — |
| `request_provider` | Solicitud KYB | — |
| `request_provider_document_type` | Catálogo de docs KYB | — |
| `request_provider_gallery` | Docs subidos KYB | — |
| `age_range_config` | Rangos de edad por ageType | — |

---

## Evolución del schema — 8 fases

### Fase 1 (migraciones 01-03) — Fundación e i18n
Conversión de columnas TEXT a JSONB para soporte multilingüe (es/en/pt). Constraint de español obligatorio. Índices GIN para búsqueda en JSONB.

### Fase 2 (migraciones 001-009) — Pricing y age ranges
Experimentación con modelos de pricing por rango de edad. Múltiples rollbacks. Llegada al modelo final: `ageType` (ADULT/CHILD/INFANT) asociado al slot.

### Fase 3 (migraciones 011-023) — Schedule y slots refinados
Capacidad ilimitada (`is_unlimited_capacity`), subcategorías, duración configurable, time-of-day arrays. Reservas temporales con `expires_at`. SP de búsqueda v2.

### Fase 4 (migraciones 024-030) — Business categories y tags
Sistema de tags estandarizado. `tour_business_category` para clasificación de negocio. `sp_search_slot_effective_availability` para inventario.

### Fase 5 (migraciones 031-039) — Payments, payout y user features
Infraestructura de payout (`provider_payout_order`, `provider_payout_order_reservation`, `provider_payout_attachment`). `tourist_profile`. `user_wishlist`. Estados temporales de reservación.

### Fase 6 (migraciones 040-045) — Provider management y compliance
Reviews `PENDING → PUBLISHED` (no más moderación). RNT agregado a provider. Multi-usuario provider (`provider_user`, `provider_user_tour`). `must_change_password` para sub-usuarios.

### Fase 7 (migraciones 046-054) — Optimización y dynamic pricing
`sp_count_tour_schedule_json` para paginación. Filtros enriquecidos en payouts. `tour_schedule_price_override` para sobreprecios puntuales. Transferencia de créditos.

### Fase 8 (migraciones 055-061) — Maritime + refinements finales
Reportes DIMAR estructurados con location + dateRange. i18n completo en filtros. Backfill de provider_price histórico.

### Fase 9 (migraciones 062-077) — Push, agentes IA, refresh tokens, notificaciones
Refresh tokens (067), Wompi webhook (066), device tokens FCM (075), notificaciones de expiración de crédito (074), `agent_audit_log` (076), `_user.phone` para operadores (077). Bootstrap de `app_config` (068-073).

### Fase 10 (migraciones 078-091) — TCs QA ciclo julio-agosto 2026
Fase intensiva de bug-fixes reportados por Luis:
- 078: `fn_slot_booked_units_on_schedule` — bookings por `(slot, fecha)` en runtime (TC-004).
- 079: `sp_get_provider_reservations` respeta `price_type` grupo para `providerPrice` (TC-005).
- 080: backfill drift de `slot.bookings` (BE-27).
- 081: `reservation.provider_declined_at` (BE-24).
- 082: `sp_get_provider_reservations` expone `providername` y `tourimageurl` (issue #188 refinamiento).
- 083: `tourist_profile.document_type` (TC-009).
- 084: `sp_get_provider_reservations` expone `travelerBreakdown` (TC-016 B).
- 085: `sp_get_tour_schedule_json` con `blockedByMaritimeReport` (TC-018 Bug B). ⚠️ regresión accidental de TC-004 corregida en 088.
- 086: `tour_schedule_config.sub_category` + `get_templates_by_provider(p_sub_category)` (TC-019).
- 087: `tour_address` geo NULLABLE + CHECK Hotel Pickup (TC-017).
- 088: backfill `slot_porcentaje_tourya` + `price` + restore TC-004 en `sp_get_tour_schedule_json` (TC-019 R1/R2/R3).
- 089: `fn_slot_booked_units_on_schedule` incluye `RESCHEDULED` en el `IN` de delivery_status + backfill idempotente de `slot.bookings` (792 filas) y `slot.availability` (404 filas) con la definición nueva (TC-019 bug 3). Complementa mig 080. Ver [RN-061](05-reglas-de-negocio.md).
- 090: backfill de drift entre `tour_schedule_config_price.price` y `provider_price × (1 + slot_pct)` — 4 filas en dev (slots 522/526/629 tour 43, slot 527 tour 36). Idempotente `WHERE ABS(...) > 0.01`. Complementa mig 088 cubriendo el caso `price != provider_price × (1 + slot_pct)` cuando `slot_pct > 0` (TC-019 bug 4). Habilita el flujo `TourScheduleConfigSlotDto.slotPorcentajeTourya` — ver [RN-015](05-reglas-de-negocio.md) refinamiento.
- 091: **TC-022** flujo de devolución en efectivo de crédito (issue #253). 3 columnas nuevas en `credit` (`refund_requested_at TIMESTAMPTZ NULL`, `refunded_at TIMESTAMPTZ NULL`, `refund_proof_url VARCHAR(500) NULL`) + normaliza `credit_status_check` para admitir los 8 valores del catálogo (`CREATED, RESERVED, CONSUMED, CANCELED, DELETED, EXPIRED, REFUND_REQUESTED, REFUNDED`) — cierra la deuda latente donde la mig 074 (BE-19) empezó a usar `'EXPIRED'` sin actualizar el CHECK. Aplicada a Cloud SQL dev (`tourya-dev-db`). Ver [RN-062](05-reglas-de-negocio.md#rn-062).

> 📌 **Cambio TC-019 bug 4 (PR #247)** — nuevo campo `slotPorcentajeTourya` (0-100 puntos, nullable) en `TourScheduleConfigSlotDto`. Persistido en `tour_schedule_config_slot.slot_porcentaje_tourya` **solo si el rol es BACKOFFICE** al invocar `POST/PUT /tour-schedules/config` o `POST /tour-schedules/batch`. Recalcula `tour_schedule_config_price.price` en el mismo request (idempotente, siempre desde `providerPrice`).

> 📌 **Cambio TC-022 (PR #254)** — nuevos estados `REFUND_REQUESTED` y `REFUNDED` en `CreditStatusEnum` + entity `Credit` con 3 campos nuevos + endpoints `POST /credits/{id}/request-refund` (turista) y `POST /admin/credits/{id}/upload-refund-proof` (multipart, BACKOFFICE/ADMIN) + `GET /admin/credits` paginado. Uso de `IStorageService` (S3 o GCS) para el comprobante en path `credit-refund-proofs/{creditId}/...`. Ver [RN-062](05-reglas-de-negocio.md#rn-062) y [doc 09 Credits](09-api-design.md).

---

## Stored Procedures de negocio

### Los grandes (14 SPs de negocio)

| SP | Propósito | Llamado desde |
|----|-----------|---------------|
| `sp_get_tour_schedule_json` | Búsqueda principal de tours con filtros (20+ dimensiones) | `SearchTourScheduleFullService`, `PublicController` |
| `sp_count_tour_schedule_json` | Conteo de resultados (paginación) | mismo flujo de búsqueda |
| `sp_get_provider_reservations` | Dashboard de reservas del provider con filtros | `ReservationService` |
| `get_templates_by_provider` | Lista de plantillas del provider | `TourScheduleConfigGeneralService` |
| `sp_clear_shopping_cart` | Vaciar carrito | `ShoppingCartService` |
| `sp_get_tag_categories` | Categorías de tags | `PublicController` |
| `sp_get_tour_tags` | Tags por tour | `TourService` |
| `sp_get_all_tour_tags` | Catálogo completo de tags | `PublicController` |
| `sp_get_categories_with_tours` | Categorías con tours publicados | `PublicController` |
| `sp_get_locations_with_tours` | Ubicaciones con tours | `PublicController` |
| `sp_get_tour_schedule` | Schedules de tour (legacy, paginado) | — |
| `sp_search_slot_effective_availability` | Verificar disponibilidad de slot | `SearchTourScheduleFullService` |

### UUID helpers (extensión `uuid-ossp`)

`uuid_generate_v1()`, `uuid_generate_v4()`, `uuid_nil()`, etc. — usadas para generar IDs únicos (ej. QR URLs).

---

## TranslatedField (i18n vía JSONB)

### Estructura de la columna

```json
{
  "es": "Tour a las Islas del Rosario",
  "en": "Tour to Rosario Islands",
  "pt": "Tour às Ilhas Rosario"
}
```

### Constraint: español obligatorio

```sql
CONSTRAINT tour_name_es_required
  CHECK (name IS NULL OR (name->>'es' IS NOT NULL AND name->>'es' != ''))
```

### Índices

```sql
-- GIN para búsqueda full-text en todo el JSONB
CREATE INDEX idx_tour_description_gin ON tour USING gin (description);

-- B-tree expression para búsqueda específica por idioma
CREATE INDEX idx_tour_description_es ON tour ((description->>'es'));
```

### Tablas con TranslatedField (12 columnas en 9 tablas)

| Tabla | Columnas JSONB |
|-------|----------------|
| `tour` | `name`, `description` |
| `tour_address` | `location` |
| `tour_main_attractions` | `description` |
| `tour_includes_excludes` | `description` |
| `tour_faq` | `question`, `answer` |
| `tour_itinerary` | `title`, `description` |
| `tour_cancellation_policy` | `observations` |
| `tour_gallery` | `description` |
| `review` | `comment` |
| `review_answer` | `comment` |
| `app_config` | `config_value` (uso genérico, no i18n) |
| `tags`, `tour_business_category`, `tour_business_subcategory_mapping` | `name` (catálogos con i18n) |

---

## Tipos ENUM nativos PostgreSQL

| ENUM | Valores |
|------|---------|
| `account_payable_status_enum` | PENDING, PAID, REJECTED |
| `age_price_type_enum` | ADULT, CHILD, INFANT |
| `duration_type_enum` | HORAS, DIAS |
| `tour_duration_type_enum` | HORAS, DIAS |
| `person_type` | adulto, niño, bebé |
| `reservation_status` | PENDING, CONFIRMED, CANCELED, COMPLETED |
| `review_reason` | 6 opciones |
| `shopping_cart_status_enum` | ACTIVE, PAID, COMPLETED, ABANDONED |
| `tour_tag_category_enum` | 11 categorías (Acuáticas, Naturaleza, Playas, etc.) |
| `address_type` | punto de encuentro, finalización, recogida, **Hotel Pickup** (TC-017 mig 087) |
| `tour_subcategory_enum` | (declarado en migración 016). Ahora también usado en `tour_schedule_config.sub_category` (TC-019 mig 086). Cast al persistir vía `@ColumnTransformer(write = "?::tour_subcategory_enum")` en `Tour.java:70` y en la entidad de config |
| `inclusion_type` | incluido, no incluido |

---

## Tablas legacy (candidatos a deprecación)

| Tabla | Reemplazo | Estado |
|-------|-----------|--------|
| ~~`tour_reservation`~~ | `reservation` | ✅ **Eliminada** — migración 065 (commit `076f006`) |
| ~~`tour_reservation_detail`~~ | `reservation_item` + `shopping_cart_item_detail` | ✅ **Eliminada** — migración 065 |
| ~~`tour_reservation_status_history`~~ | (auditoría inline en `reservation`) | ✅ **Eliminada** — migración 065 |
| `tour_price` | `tour_schedule_config_price` | 📌 Pendiente cleanup |
| `tour_review` (si existe) | `review` | 📌 Pendiente cleanup |

---

## Índices clave

### Performance crítica

| Índice | Tabla | Para qué |
|--------|-------|----------|
| `idx_tour_description_gin` | tour | Búsqueda full-text JSONB |
| `idx_review_comment_gin` | review | Búsqueda full-text reviews |
| `idx_tour_schedule_date` | tour_schedule | Filtro por fecha en búsqueda |
| `idx_tour_provider` | tour | Listar tours del provider |
| `idx_reservation_payment` | reservation | Lookup por paymentId |
| `idx_reservation_delivery_status` | reservation | Filtros de panel |
| `idx_credit_user_status` | credit | Listar créditos del usuario |
| `idx_provider_payout_pay_date` | provider_payout_order | Cronograma de pagos |

### Composite indexes

- `(country_id, state_id, city_id)` en `tour_address` y `maritim_activity_report` para filtros geográficos.

---

## Constraints destacadas

### Español obligatorio en TranslatedField

```sql
CHECK (name IS NULL OR (name->>'es' IS NOT NULL AND name->>'es' != ''))
```

### Rating de review en rango 1-5

```sql
CHECK (rating >= 1 AND rating <= 5)
```

### Crédito no negativo

```sql
CHECK (amount >= 0)
CHECK (expiration_date >= creation_date)
```

### Credit status enum

**Estado actual desde migración 091 (TC-022, 2026-08-13)** — catálogo normalizado que refleja todos los valores realmente en uso por el código:

```sql
CHECK (status IN (
  'CREATED', 'RESERVED', 'CONSUMED',
  'CANCELED', 'DELETED', 'EXPIRED',
  'REFUND_REQUESTED', 'REFUNDED'
))
```

**Contexto histórico**:
- Original: `('CREATED', 'CANCELED', 'DELETED')`.
- La mig 074 (BE-19) empezó a persistir `'EXPIRED'` **sin** actualizar el CHECK — deuda latente que no explotó porque el valor caía por otro camino (nunca se rechazó un INSERT). La 091 la cierra.
- La 091 agrega además `REFUND_REQUESTED` y `REFUNDED` para el flujo de RN-062.

> **Nota**: `credit.status` es `VARCHAR(20)` con CHECK constraint — **no** un enum nativo PostgreSQL (a diferencia de `address_type` o `tour_subcategory_enum`). Esto simplifica agregar valores nuevos (basta con `ALTER TABLE ... DROP/ADD CONSTRAINT`) pero requiere que el Java `CreditStatusEnum` y el CHECK se mantengan sincronizados manualmente.

### Hotel Pickup exceptúa NOT NULL geo (TC-017, migración 087)

```sql
-- tour_address.country_id, state_id, city_id son NULLABLE desde mig 087
ALTER TABLE tour_address
  ADD CONSTRAINT tour_address_geo_required_unless_hotel_pickup
  CHECK (
    address_type = 'Hotel Pickup'
    OR (country_id IS NOT NULL AND state_id IS NOT NULL AND city_id IS NOT NULL)
  );
```
Solo direcciones con `address_type = 'Hotel Pickup'` pueden omitir los 3 geo IDs (la ubicación real es el hotel del turista al momento de reservar). El enum guarda el **display value** (`"Hotel Pickup"`), no la key (`HOTEL_PICKUP`) — ver `AddressTypeEnumConverter`.

### Unique constraints

- `_user.email` único.
- `role.name` único.
- `provider.document_number` ❓ (verificar).

---

## Ubicación de scripts

| Archivo | Propósito |
|---------|-----------|
| `database/ddl.sql` | Schema completo (snapshot) — 81 KB |
| `database/migrations/001_*.sql` ... `061_*.sql` | Migraciones numeradas + rollbacks |
| `database/migrations/README.md` | Doc de migraciones |
| `database/scripts/SEED_*.sql` | Datos de ejemplo |
| `database/scripts/FIX_*.sql` | Fixes puntuales |
| `database/EJECUTAR_DESPUES_DE_DEPLOY.sql` | Acciones manuales post-deploy |
| `tourya-api/MIGRACIONES_A_EJECUTAR.txt` | Lista de migraciones pendientes de aplicar a un entorno |

---

## Migraciones pendientes de aplicar en producción

✅ El archivo `MIGRACIONES_A_EJECUTAR.txt` actualizado debe consultarse. Migraciones recientes (040+) particularmente importantes:

| Migración | Qué hace | ¿Aplicada en DEV? | ¿Aplicada en PROD? |
|-----------|----------|-------------------|--------------------|
| 040 | Review status `PENDING → PUBLISHED` | ❓ | ❓ |
| 041 | Add RNT to provider | ❓ | ❓ |
| 042 | Update provider document types | ❓ | ❓ |
| 043 | Add tourya_percentage fields | ❓ | ❓ |
| 044 | Provider users roles + tour assignment | ❓ | ❓ |
| 045 | must_change_password | ❓ | ❓ |
| 046 | sp_count_tour_schedule_json | ❓ | ❓ |
| 047-054 | Optimizaciones y overrides | ❓ | ❓ |
| 055-058 | Maritime activity reports v2 | ❓ | ❓ |
| 059-061 | Provider price fallback + backfill | ❓ | ❓ |
| 066 | Wompi webhook event table | ✅ | ❓ |
| 067 | Refresh token table | ✅ | ❓ |
| 074 | Credit expiration notifications | ✅ (post-hoc, 2026-07-17) | ❓ |
| 075 | Device token FCM | ✅ (post-hoc, 2026-07-17) | ❓ |
| 076 | Agent audit log | ✅ (post-hoc, 2026-07-17) | ❓ |
| 077 | `_user.phone` | ✅ | ❓ |
| 078 | `fn_slot_booked_units_on_schedule` (TC-004) | ✅ | ❓ |
| 079 | `sp_get_provider_reservations` con `price_type` grupo (TC-005) | ✅ | ❓ |
| 080 | Backfill drift slot.bookings (BE-27) | ✅ | ❓ |
| 081 | `reservation.provider_declined_at` (BE-24) | ✅ | ❓ |
| 082 | `sp_get_provider_reservations` con `providername` + `tourimageurl` | ✅ | ❓ |
| 083 | `tourist_profile.document_type` (TC-009) | ✅ | ❓ |
| 084 | `sp_get_provider_reservations` con `travelerBreakdown` (TC-016 B) | ✅ | ❓ |
| 085 | `sp_get_tour_schedule_json` con `blockedByMaritimeReport` (TC-018 B) — ⚠️ regresión de TC-004, corregida en 088 | ✅ | ❓ |
| 086 | `tour_schedule_config.sub_category` + filtro en `get_templates_by_provider` (TC-019) | ✅ | ❓ |
| 087 | `tour_address` geo NULLABLE + CHECK Hotel Pickup (TC-017) | ✅ | ❓ |
| 088 | Backfill `slot_porcentaje_tourya` + `price` + restore SP TC-004 (TC-019) | ✅ | ❓ |

⚠️ **Deuda operativa** (registrada en backlog como **INF-05**): las migraciones no se aplican automáticamente en el pipeline. Cada release requiere aplicarlas manualmente en dev (y luego en prod). Ver [doc 17 INF-05](17-backlog-implementacion.md) y feedback `how_to_apply_migrations.md`.

📌 PENDIENTE — verificar estado real de migraciones por entorno.

---

## Decisiones del modelo de datos

### Decisión: JSONB > tablas separadas para i18n
Ya explicado en [07 — Arquitectura técnica](07-arquitectura-tecnica.md).

### Decisión: SPs para queries complejas
La búsqueda de tours pasa por `sp_get_tour_schedule_json` porque filtros dinámicos en 20+ dimensiones eran demasiado lentos vía JPA Specifications.

### Decisión: Composite PKs en `user_wishlist` y `provider_payout_order_reservation`
Para garantizar unicidad y simplificar joins.

### Decisión: Sin soft delete
Toda eliminación es real. Auditoría inline en BaseEntity (no historial separado).

### Decisión: Sin multi-tenancy
Tourya es un solo entorno multi-usuario, no SaaS multi-tenant. Sin `tenant_id` ni RLS.
