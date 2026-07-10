# 04 — Entidades de dominio

✅ 54 entidades JPA agrupadas en **10 bounded contexts** (subsistemas con borde claro). Para cada entidad: tabla, campos clave, relaciones, y notas relevantes.

Esta estructura guía la organización lógica del código (aunque Tourya hoy NO está modularizada formalmente en bounded contexts — todo vive bajo `com.tourya.api.models`).

---

## Bounded Contexts

| # | Contexto | Responsabilidad | # entidades |
|---|----------|-----------------|-------------|
| 1 | **Usuarios y Auth** | Identidad, roles, tokens, sub-usuarios | 6 |
| 2 | **Proveedores y Tours** | Contenido del catálogo | 15 |
| 3 | **Horarios y Slots** | Disponibilidad y precios variables | 6 |
| 4 | **Reservas y Carrito** | Compra del turista | 8 |
| 5 | **Pagos y Finanzas** | Liquidación monetaria | 7 |
| 6 | **Reviews** | Reseñas y respuestas | 4 |
| 7 | **Onboarding (KYB)** | Validación de operadores | 3 |
| 8 | **Maritime Reports** | Cumplimiento DIMAR | 1 |
| 9 | **Catálogos / Maestros** | Geo + config | 5 |
| 10 | **Otros** | Misceláneo | 1 |

---

## Convenciones

- ✅ Las entidades marcadas con **(BaseEntity)** heredan auditoría: `createdDate`, `lastModifiedDate`, `createdBy`, `lastModifiedBy`.
- ✅ Las entidades marcadas con **(TranslatedField)** tienen al menos un campo multilingüe (JSONB).
- ✅ El nombre real de la tabla aparece entre `backticks` cuando difiere del nombre de clase.

---

## Context 1 — Usuarios y Auth

### 1.1 `User` (tabla `_user`) (BaseEntity)
Cuenta de autenticación principal.

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Integer | PK |
| `email` | String (unique) | login |
| `password` | String | BCrypt hash |
| `firstname`, `lastname` | String | |
| `dateOfBirth` | LocalDate | |
| `enabled` | boolean | `false` hasta activar email |
| `accountLocked` | boolean | bloqueo manual del admin |
| `mustChangePassword` | boolean | true tras reset / creación con clave temporal |
| `uuidSocial` | String (nullable) | Firebase UID o sub de Google/Facebook |
| `roles` | List\<Role\> | many-to-many vía `user_roles` |

**Implementa**: `UserDetails`, `Principal` (Spring Security).

**Relaciones**: `↔ Role`, `← Token`, `← ProviderUser`, `← TouristProfile`.

⚠️ `password` se setea con UUID random al crear vía social login (el usuario nunca lo usa).

---

### 1.2 `Role` (BaseEntity)
Roles del sistema.

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Integer | PK |
| `name` | String (unique) | "USER", "PROVIDER", "PROVIDER_OPERATOR", "ADMIN", "BACKOFFICE_OPERATION" |

**Relaciones**: `← User` (M2M inverse).

---

### 1.3 `Token` (tabla `token`)
Códigos de activación de cuenta.

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Integer | PK |
| `token` | String | código 6 dígitos |
| `createdAt` | LocalDateTime | |
| `expiresAt` | LocalDateTime | `createdAt + 15 min` |
| `validatedAt` | LocalDateTime | null hasta activar |
| `user` | User | → ManyToOne |

✅ Protección contra replay implementada (PR #147, SEC-04, 2026-07-08): `AuthenticationService.activateAccount()` verifica `validatedAt != null` al inicio y rechaza tokens ya usados.

---

### 1.4 `ProviderUser` (BaseEntity)
Vincula un User a un Provider (titular o sub-usuario).

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Integer | PK |
| `provider` | Provider | → ManyToOne |
| `user` | User | → ManyToOne |
| `isPrimary` | boolean | true = titular |

**Relaciones**: `← ProviderUserTour`.

✅ Agregado en migración **044**.

---

### 1.5 `ProviderUserTour`
Asignación de tours específicos a un sub-usuario.

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Integer | PK |
| `providerUser` | ProviderUser | → ManyToOne |
| `tour` | Tour | → ManyToOne |
| `isPrincipal` | boolean | "tour principal" del operario |

✅ Agregado en migración **044**.

---

### 1.6 `TouristProfile`
Datos extendidos del turista (separado de `User` para no recargar el auth principal).

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Integer | PK |
| `userId` | Integer | FK lógico a `_user.id` (read-only) |
| `firstName`, `lastName` | String | duplicado de User (para overrides) |
| `documentNumber` | String | cédula/pasaporte |
| `phone`, `email` | String | |
| `city`, `state`, `country` | String | |
| `photoUrl` | String | URL en GCS/S3 |
| `createdAt`, `updatedAt` | LocalDateTime | |

✅ Agregado en migración **033**.

---

## Context 2 — Proveedores y Tours

### 2.1 `Provider` (BaseEntity)
Operador turístico (entidad de negocio).

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Integer | PK |
| `name` | String | razón social |
| `documentNumber` | String | NIT |
| `documentType` | enum (`ProviderDocumentTypeEnum`) | |
| `rnt` | String | Registro Nacional de Turismo |
| `serviceType` | enum | tipo de servicio |
| `status` | enum | ACTIVE / INACTIVE |
| `address`, `phone`, `department` | String | |
| `country`, `state`, `city` | FK | → Catálogos |

**Relaciones**: `← Tour`, `← ProviderUser`, `← RequestProvider`, `← AccountPayable`, `← ProviderPayoutOrder`.

✅ `rnt` agregado en migración **041**.

---

### 2.2 `Tour` (BaseEntity) (TranslatedField)
Producto principal.

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Integer | PK |
| `name` | TranslatedField (JSONB) | |
| `description` | TranslatedField (JSONB) | |
| `duration` | Integer | |
| `durationEnum` | enum | HORAS / DIAS |
| `maxPeople` | Integer | |
| `subCategory` | enum | |
| `timeOfDay` | String[] | array de momentos del día |
| `status` | enum (`TourStatusEnum`) | CREATED → SUBMITTED → ACCEPTED / RETURNED / CANCELLED |
| `provider` | Provider | → ManyToOne |
| `category` | TourCategory | → ManyToOne |

**Relaciones**: `← TourAddress`, `← TourMainAttraction`, `← TourIncludesExcludes`, `← TourFaq`, `← TourItinerary`, `← TourCancellationPolicy`, `← TourGallery`, `← TourSchedule`, `← Review`.

---

### 2.3 `TourAddress` (BaseEntity) (TranslatedField)
Ubicación geográfica del tour (puede haber múltiples: encuentro, finalización, recogida).

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Integer | PK |
| `location` | TranslatedField (JSONB) | texto descriptivo |
| `latitude`, `longitude` | Double | |
| `tour`, `country`, `state`, `city` | FK | |
| `addressType` | enum | encuentro / finalización / recogida |

---

### 2.4 `TourMainAttraction` (BaseEntity) (TranslatedField)
Atracción destacada del tour.

| `description` | TranslatedField (JSONB) | |
| `tour` | FK | |

---

### 2.5 `TourIncludesExcludes` (BaseEntity) (TranslatedField)

| `description` | TranslatedField (JSONB) | |
| `type` | enum | INCLUDE / EXCLUDE |
| `tour` | FK | |

---

### 2.6 `TourFaq` (BaseEntity) (TranslatedField)

| `question` | TranslatedField (JSONB) | |
| `answer` | TranslatedField (JSONB) | |
| `tour` | FK | |

---

### 2.7 `TourItinerary` (BaseEntity) (TranslatedField)

| `day`, `time` | | |
| `title` | TranslatedField (JSONB) | |
| `description` | TranslatedField (JSONB) | |
| `tour` | FK | |

---

### 2.8 `TourCancellationPolicy` (BaseEntity) (TranslatedField)

| `cancellationPolicyType` | enum | |
| `allowsRainRefund` | boolean | |
| `allowsRescheduling` | boolean | |
| `observations` | TranslatedField (JSONB) | |
| `tour` | FK | |

---

### 2.9 `TourGallery` (BaseEntity) (TranslatedField)

| `imageUrl` | String | URL en GCS/S3 |
| `description` | TranslatedField (JSONB) | |
| `orderIndex` | Integer | |
| `tour` | FK | |

---

### 2.10 `TourCategory` (BaseEntity)
Catálogo de categorías (Aventura, Cultura, Comida, Familias, etc.).

| `name`, `description` | String | |

---

### 2.11 `TourTag` / `tour_tag_mapping`
Etiquetas para enriquecer búsqueda. Con 11 categorías predefinidas en enum (`tour_tag_category_enum`).

---

### 2.12 `Filter` / `tags` (con TranslatedField)
Filtros multilingües para la búsqueda (categorías y subcategorías de tags). Modelo nuevo, agregado en migraciones **055-056** para soportar i18n.

---

### 2.13 `TourCancelCategory` (BaseEntity)
Catálogo de razones de cancelación.

| `name`, `description` | String | |

---

### 2.14 `TouryaService` (BaseEntity)
Servicios adicionales.

| `name`, `description`, `cancellationPolicy`, `status` | | |
| `serviceType` | FK | → ServiceType |

📌 **A redefinir**: este módulo se dejó preparado para los servicios futuros que ofertará Tourya (**transporte, souvenirs, Duty Free**, hospedaje, domicilios). Estamos en el momento de **redefinir su modelo** de acuerdo al roadmap de producto (ver [01 — Visión y negocio](01-vision-y-negocio.md)).

---

### 2.15 `ServiceType`
Clasificación de servicios adicionales.

---

## Context 3 — Horarios y Slots

### 3.1 `TourSchedule` (BaseEntity)
Instancia concreta del tour en una fecha.

| `scheduleDate` | LocalDate | |
| `status` | enum | |
| `tour`, `tourScheduleConfig` | FK | |
| `maxCapacity`, `reservedCapacity` | Integer | ✅ **Eliminados** de `tour_schedule` en migración 029 (2026-04-08); capacidad efectiva vive en `tour_schedule_config_slot` |
| ~~`isUnlimitedCapacity`~~ | ~~boolean~~ | ✅ **Eliminado** de `tour_schedule` y `tour_schedule_config` en migración 029 (2026-04-08). La fuente de verdad es `Tour.isUnlimitedCapacity`. Ver RN-021 en [05](05-reglas-de-negocio.md) |

---

### 3.2 `TourScheduleConfig` (BaseEntity)
Plantilla reutilizable de horario.

| `label` | String | "Horario estándar verano" |
| `daysOfWeek` | String[] | ["MONDAY", "FRIDAY"] |
| `isTemplate` | boolean | |
| `tour`, `provider` | FK | |

**Relaciones**: `← TourScheduleConfigSlot`.

---

### 3.3 `TourScheduleConfigSlot` (BaseEntity)
Franja horaria dentro del config.

| `startTime`, `endTime` | LocalTime | |
| `capacity` | Integer | |
| `bookings` | Integer | reservas actuales |
| `availability` | Integer | capacity - bookings |
| `minCapacityCalc`, `checkAvailability` | flags | |

**Relaciones**: `← TourScheduleConfigPrice`.

---

### 3.4 `TourScheduleConfigPrice` (BaseEntity)
Precio por tipo de persona dentro del slot.

| `slot` | FK | |
| `ageType` | enum | ADULT / CHILD / INFANT |
| `providerPrice` | Decimal | lo que el operador define |
| `price` | Decimal | precio de venta = providerPrice × (1 + %/100) |

⚠️ El proveedor NO ve `price` ni `slotPercentageTourya` — solo `providerPrice`.

---

### 3.5 `TourSchedulePriceOverride` (BaseEntity)
Override de precio para un slot en una fecha puntual.

| `schedule`, `price` | FK | |
| `priceOverride` | Decimal | |

✅ Agregado en migración **052**.

---

### 3.6 `TourScheduleSlotOverride` (BaseEntity)
Override de % comisión Tourya para un slot en una fecha puntual.

| `schedule`, `slot` | FK | |
| `slotPorcentajeTourya` | Integer | puntos |

✅ Agregado en migración **052**.

---

## Context 4 — Reservas y Carrito

### 4.1 `ShoppingCart` (BaseEntity)
Carrito del turista.

| `user` | FK | |
| `status` | enum | ACTIVE / PAID / COMPLETED / ABANDONED |
| `accommodationName`, `accommodationLatitude`, `accommodationLongitude` | | dirección del hotel del turista |
| `country`, `state`, `city` | FK | |

**Relaciones**: `← ShoppingCartItem`.

---

### 4.2 `ShoppingCartItem` (BaseEntity)
Item del carrito (un tour en una fecha/slot).

| `shoppingCart`, `tourSchedule`, `tourScheduleConfigSlot` | FK | |
| `productId`, `productType` | | TOUR / SERVICE |
| `scheduleDate` | LocalDate | |
| `totalPrice` | Decimal | |

**Relaciones**: `← ShoppingCartItemDetail`.

---

### 4.3 `ShoppingCartItemDetail` (BaseEntity)
Detalle del item por tipo de persona.

| `cartItem` | FK | |
| `ageType` | enum | |
| `quantity` | Integer | |
| `unitPrice`, `providerUnitPrice` | Decimal | |
| `totalPrice`, `providerTotalPrice` | Decimal | |

---

### 4.4 `Reservation` (BaseEntity)
Reserva confirmada.

| `payment` | FK | |
| `itemId` | Integer | ref al cart item |
| `reservationDate` | Date | fecha del tour |
| `qrUrl` | String | URL del QR |
| `deliveryStatus` | enum | PENDING / DELIVERED / CANCELED |
| `totalAmount`, `providerTotalAmount` | Decimal | |
| `expiresAt` | LocalDateTime | hold temporal |
| `payoutStatus` | enum | PENDING / PAID |
| `payoutAvailableDate` | LocalDate | `reservationDate + 2 días` |
| `serviceResponsibleName`, `serviceResponsibleEmail`, `serviceResponsiblePhone` | | contacto |
| `cancellationReason`, `cancellationDate` | | |
| `maxCancellationDate`, `maxReschedulingDate` | | |
| `canCancel`, `canReschedule` | boolean | flags expirables por job |

✅ Es el modelo principal de reservas.

---

### 4.5 `ReservationItem` (BaseEntity)
Items dentro de una reserva.

| `reservation`, `shoppingCartItem` | FK | |
| `serviceResponsibleName/Email/Phone` | | |

---

### 4.6 ~~`TourReservation`~~ — **ELIMINADO** (legacy)
✅ Modelo antiguo de reservas **eliminado del código** el 2026-06-28 (commit `076f006`, migración `065`). Reemplazado por `Reservation` + `ReservationItem`.

---

## Context 5 — Pagos y Finanzas

### 5.1 `Payment` (BaseEntity)

| `transactionId` | String | de Wompi |
| `transactionData` | JSON | payload completo de Wompi |
| `payerId`, `payerName`, `payerEmail`, `payerPhone` | | |
| `payerDocumentType`, `payerDocumentNumber` | | |
| `amountCredit` | Decimal | si pagó con créditos |

**Relaciones**: `← Reservation`.

---

### 5.2 `PaymentCredit`
Link entre Payment y Credit (cuánto crédito se consumió en cada pago).

| `payment`, `credit` | FK | |
| `amountUsed` | Decimal | |

---

### 5.3 `Credit` (BaseEntity)
Crédito a favor del turista.

| `reservationId` | Integer | origen del crédito (cancelación) |
| `userId` | Integer | dueño |
| `amount` | Decimal | total disponible |
| `reservedAmount` | Decimal | bloqueado en checkout |
| `creationDate`, `expirationDate` | | 1 año de vida |
| `status` | enum | CREATED / CANCELED / DELETED |
| `transferredFromUserId`, `transferredAt` | | si fue transferido de otro turista |

✅ Transferencia agregada en migración **048**.

---

### 5.4 `AccountPayable` (BaseEntity)
Cuenta por pagar al proveedor.

| `reservationId`, `providerId` | FK | |
| `transactionDate` | Date | |
| `amount` | Decimal | `providerPrice × quantity` |
| `deliveryStatus` | enum | |

---

### 5.5 `ProviderPayoutOrder`
Orden de pago semanal al proveedor.

| `providerId` | FK | |
| `createdAt`, `payDate` | | |
| `status` | enum | PENDING / PAID / CANCELED |
| `amountTotal` | Decimal | |

**Relaciones**: `← ProviderPayoutOrderReservation`, `← ProviderPayoutAttachment`.

---

### 5.6 `ProviderPayoutOrderReservation`
Link entre payout order y reservations incluidas.

| `payoutOrderId`, `reservationId`, `accountPayableId` | FK | |
| `amount` | Decimal | |

**Composite PK**: `(payoutOrderId, reservationId)`.

---

### 5.7 `ProviderPayoutAttachment`
Comprobante de pago.

| `payoutOrderId` | FK | |
| `fileUrl` | String | URL en GCS/S3 |
| `createdAt` | | |

---

## Context 6 — Reviews

### 6.1 `Review` (BaseEntity) (TranslatedField)

| `reservationId`, `itemId`, `tourId`, `userId` | FK | |
| `rating` | int (1-5) | |
| `comment` | TranslatedField (JSONB) | |
| `status` | enum | PENDING / PUBLISHED / CANCELED |
| `reviewDate` | | |
| `likes`, `dislikes`, `hearts` | int | reacciones |
| `rejectionReason` | String | si fue rechazada |

✅ Migración **040** cambió defaulting a `PUBLISHED`.

---

### 6.2 `ReviewAnswer` (BaseEntity) (TranslatedField)
Respuesta del proveedor.

| `reviewId` | FK | |
| `comment` | TranslatedField (JSONB) | |
| `providerName`, `providerImage`, `date` | | |
| `likes`, `dislikes`, `hearts` | int | |

---

### 6.3 `ReviewAttachment` (BaseEntity)
Fotos de la review.

| `reviewId`, `fileUrl`, `fileName`, `fileType`, `fileSize` | | |

---

### 6.4 `ReviewAnswerAttachment` (BaseEntity)
Fotos de la respuesta.

---

## Context 7 — Onboarding (KYB)

### 7.1 `RequestProvider` (BaseEntity)

| `provider` | FK | nullable hasta aprobar |
| `status` | enum | DRAFT / SUBMITTED / PRE_APPROVED / INCOMPLETE / APPROVED / CANCELED |
| `declinedReason`, `incompleteReason` | String | |

**Relaciones**: `← RequestProviderGallery`.

---

### 7.2 `RequestProviderDocumentType` (BaseEntity)
Catálogo: tipos de documento que el KYB requiere.

| `name`, `description` | | |
| `mandatory` | boolean | |

---

### 7.3 `RequestProviderGallery` (BaseEntity)
Documentos cargados por el aplicante.

| `requestId`, `documentTypeId` | FK | |
| `imageUrl`, `description`, `orderIndex` | | |

---

## Context 8 — Maritime Reports

### 8.1 `MaritimActivityReport` (BaseEntity)

| `countryId`, `stateId`, `cityId` | FK | |
| `businessCategoryId`, `subcategoryCode` | | |
| `flag` | enum | bandera ondeada (verde / amarilla / roja) |
| `reportStartDate`, `reportEndDate` | | |
| `department` | String | |

Usado por backoffice para registrar condiciones marítimas que justifican cancelaciones por mal tiempo.

✅ Mejorado en migraciones **055-058** (location + dateRange estructurados).

---

## Context 9 — Catálogos / Maestros

### 9.1 `Country`, `State`, `City`
Catálogo geográfico. State = departamento (en Colombia). City = municipio.

### 9.2 `AgeRangeConfig` (BaseEntity)
Definición central de rangos de edad por `ageType`.

| `ageType` | enum | ADULT / CHILD / INFANT |
| `minAge`, `maxAge` | Integer | |
| `description`, `isActive` | | |

### 9.3 `AppConfig` (BaseEntity)
Key-value de configuración del sistema.

| `configKey` | String | |
| `configValue` | JSONB | |
| `description` | | |

---

## Context 10 — Otros

### 10.1 `WishlistItem` (tabla `user_wishlist`)
Tours favoritos del turista.

| `userId`, `tourId` | FK | composite PK |
| `createdAt` | | |

---

## Resumen

- ✅ **54 entidades** JPA totales.
- ✅ **~38 entidades** extienden `BaseEntity` (auditoría).
- ✅ **~10 entidades** usan `TranslatedField` (JSONB i18n).
- ✅ **2 entidades** con PK compuesta: `ProviderPayoutOrderReservation`, `WishlistItem`.
- ⚠️ **Sin single-table inheritance ni soft deletes**.
- ⚠️ **Sin multi-tenancy** (un solo tenant — toda Tourya es un solo entorno).
- ⚠️ **`TourReservation` legacy** debería deprecarse y eliminarse (reemplazado por `Reservation`).

---

## Referencias cruzadas

- Modelo de BD (DDL, índices, SPs): [08-modelo-de-datos.md](08-modelo-de-datos.md)
- Reglas de negocio por entidad: [05-reglas-de-negocio.md](05-reglas-de-negocio.md)
- API que expone estas entidades: [09-api-design.md](09-api-design.md)
