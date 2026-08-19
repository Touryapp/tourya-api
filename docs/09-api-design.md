# 09 — API design

✅ **150+ endpoints** en **35 controllers**. REST clásico, JSON, JWT Bearer.

> **Base URL**: `http://localhost:8088/api/v1/` (local), `https://tourya.co/api/v1/` (prod).
> **Auth**: `Authorization: Bearer {jwt}` excepto rutas públicas (`/auth/**`, `/public/**`).
> **Swagger UI**: `http://localhost:8088/api/v1/swagger-ui/index.html`.

---

## Convenciones REST

### URL paths
- `/{recurso}` para colecciones (`/tour`, `/reservations`).
- `/{recurso}/{id}` para item.
- `/{recurso}/user/...` para acciones del owner (provider o user).
- `/{recurso}/admin/...` para acciones administrativas.
- `/public/...` y `/auth/...` para endpoints sin JWT.

### Verbos
- `GET` para lecturas.
- `POST` para creación / acción.
- `PUT` para reemplazo total.
- `PATCH` para actualización parcial (raro en Tourya — solo `PATCH /users` para cambio de password).
- `DELETE` para eliminación.

### Auth
- JWT en header `Authorization: Bearer {token}`.
- `JwtFilter` intercepta toda request a `/api/v1/**` excepto rutas en `permitAll()`.
- Roles validados por path o por `@PreAuthorize` en algunos endpoints.

### Paginación
Param estándar: `page` (0-based, default=0), `size` (default=10). Algunos endpoints usan `pageNumber` y `pageSize`.

Sorting: `sortBy`, `sortDirection` (ASC/DESC).

### Response envelope
✅ Algunos endpoints usan `MetaResponse + data + error`, otros devuelven directamente la entidad. **Inconsistencia detectada** — se sugiere estandarizar.

---

## Inventario completo de endpoints

### 1. Auth y usuarios

#### `AuthenticationController` — `/auth`
| Método | Path | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/auth/register` | público | Registro USER |
| POST | `/auth/authenticate` | público | Login email/password. Response incluye `token` (legacy alias) + `accessToken` + `refreshToken` |
| POST | `/auth/social-auth` | público | Login social (Firebase UID). Idem response con refresh token |
| POST | `/auth/refresh` | público | Rota tokens: recibe `{refreshToken}`, revoca el actual, emite nuevo par access + refresh. Detecta reuso → revoca familia + 401 |
| POST | `/auth/logout` | público | Revoca la familia entera del refresh token. Idempotente (204 aunque el token sea inválido) |
| POST | `/public/wompi/webhook` | público | Recibe eventos server-to-server de Wompi. Verifica firma SHA-256 con events secret. Persiste todo en `wompi_webhook_event` para reconciliación |
| GET | `/auth/activate-account` | público | Activar cuenta con código |

#### `UserController` — `/users`
| Método | Path | Auth | Roles |
|--------|------|------|-------|
| PATCH | `/users` | JWT | Todos — cambio de password |
| GET | `/users/admin/findAll` | JWT | ADMIN |
| GET | `/users/admin/consultDataById/{userId}` | JWT | ADMIN |
| PUT | `/users/admin/blockById/{userId}` | JWT | ADMIN |
| PUT | `/users/admin/unBlockById/{userId}` | JWT | ADMIN |

#### `TouristProfileController` — `/tourist/profile`
| Método | Path | Auth | Roles |
|--------|------|------|-------|
| GET | `/tourist/profile` | JWT | USER |
| PUT | `/tourist/profile` | JWT | USER |
| DELETE | `/tourist/profile` | JWT | USER |
| GET | `/tourist/profile/address-complete` | JWT | USER |
| PUT | `/tourist/profile/photo` | JWT | USER (multipart, ≤1MB) |

#### `ProviderUserController` — `/provider/users`
Gestión de sub-usuarios del provider.

| Método | Path | Auth | Roles |
|--------|------|------|-------|
| GET | `/provider/users` | JWT | PROVIDER |
| POST | `/provider/users` | JWT | PROVIDER |
| PUT | `/provider/users/{providerUserId}` | JWT | PROVIDER |
| PUT | `/provider/users/{providerUserId}/principal-tour?tourId=X` | JWT | PROVIDER |
| PUT | `/provider/users/{providerUserId}/reset-password` | JWT | PROVIDER |

---

### 2. Tours y catálogo

#### `TourController` — `/tour`
| Método | Path | Auth | Roles |
|--------|------|------|-------|
| GET | `/tour/admin/findAll` | JWT | ADMIN |
| GET | `/tour/admin/consultDataTourById/{tourId}` | JWT | ADMIN |
| PUT | `/tour/admin/acceptTourById/{tourId}` | JWT | ADMIN |
| PUT | `/tour/admin/returnedTourById/{tourId}` | JWT | ADMIN |
| PUT | `/tour/admin/cancelTourById/{tourId}` | JWT | ADMIN |
| PATCH | `/tour/admin/{tourId}/porcentajeTourya` | JWT | ADMIN/BACKOFFICE. Body `{ porcentajeTourya: BigDecimal }` (0.0–1.0, ej. `0.15` = 15%). Actualiza el default por tour usado como fallback al crear slots nuevos (RN-015). Retorna `TourFullDataResponse`. |
| GET | `/tour/user/findAllByUser` | JWT | PROVIDER |
| POST | `/tour/user/saveAll` | JWT | PROVIDER |
| GET | `/tour/user/consultDataTourById/{tourId}` | JWT | PROVIDER |
| PUT | `/tour/user/submitTourById/{tourId}` | JWT | PROVIDER |
| GET | `/tour/details/{tourId}` | público u opt-in | Todos |

> 📌 **Cambio PR #255 (2026-08-13)** — `TourFullDataResponse` expone ahora el campo opcional `blockedByMaritimeReport: boolean` (para cerrar la deuda mobile de TC-018 detectada en Sprint 2). Aplica a todos los endpoints que devuelven este DTO: `GET /public/tour/details/{tourId}`, `GET /public/consultDataTourById/{tourId}`, `GET /tour/details/{tourId}`, `GET /tour/user/consultDataTourById/{tourId}`, `GET /tour/admin/consultDataTourById/{tourId}` (y los mutadores admin que retornan el DTO — `acceptTourById`, `returnedTourById`, `cancelTourById`, `porcentajeTourya`, `submitTourById`, `saveCreateFullData`). Semántica: `true` si hay al menos un `MaritimActivityReport` en estado RED activo hoy (`LocalDate.now(America/Bogota)`) que matchee la subcategoría del tour y alguna de sus `locations` con geo. Hotel Pickup y locations sin geo se ignoran (DIMAR no las cubre) → `false` por defecto en esos casos. Reusa `MaritimActivityReportRepository.findActiveRedReportsForSubcategoryAndLocation` — mismo helper del guard duro `ShoppingCartService.validateNoActiveMaritimeAlert`. Antes del PR el flag solo lo exponía el search (`SearchTourScheduleFullResponse.schedules[].blockedByMaritimeReport`); ahora el detail también, para que mobile deshabilite el CTA "Reservar" sin depender del fallback 400 del carrito.

#### `TourCategoryController` — `/tourCategory`
| Método | Path | Auth |
|--------|------|------|
| POST | `/tourCategory/admin/save` | ADMIN |
| GET | `/tourCategory/admin/findAll` | ADMIN |
| GET | `/tourCategory/user/getAllTourCategoryList` | público |

#### `TourAddressController` — `/tourAddress`
| Método | Path | Auth |
|--------|------|------|
| POST | `/tourAddress/user/save/{tourId}` | PROVIDER |
| POST | `/tourAddress/user/saveList/{tourId}` | PROVIDER |
| GET | `/tourAddress/user/consultDataTourAddressById/{id}` | PROVIDER |
| GET | `/tourAddress/user/consultDataTourAddressListByTourId/{tourId}` | PROVIDER |

#### `TourGalleryController` — `/tours/{tourId}/gallery`

**RN-013 aplicado desde 2026-07-09 (PR #161, BE-10)**: los métodos `sync` y `syncWithUpdate` validan formato/tamaño/orientación/ancho/cuenta antes de subir a S3. Si una imagen falla → 400 con `issues[]` completo y rollback JPA (no toca S3). Umbrales configurables en `app_config` (ver keys `GALLERY_*` en la sección `/config`).


| Método | Path | Auth | Notas |
|--------|------|------|-------|
| GET | `/tours/{tourId}/gallery` | PROVIDER | |
| POST | `/tours/{tourId}/gallery/sync` | PROVIDER | multipart: newFiles + galleryData JSON |
| POST | `/tours/{tourId}/gallery/syncWithUpdate` | PROVIDER | multipart con reemplazos |

#### `TourItineraryController` — `/tours/{tourId}/itinerary`
| Método | Path | Auth |
|--------|------|------|
| POST | `/tours/{tourId}/itinerary/replace` | PROVIDER |
| GET | `/tours/{tourId}/itinerary` | público |

#### `TourFaqController` — `/tourFaq`
| Método | Path | Auth |
|--------|------|------|
| POST | `/tourFaq/user/save/{tourId}` | PROVIDER |
| POST | `/tourFaq/user/saveList/{tourId}` | PROVIDER |
| POST | `/tourFaq/user/replaceAll/{tourId}` | PROVIDER |

#### `TourMainAttractionController` — `/tours/{tourId}/main-attractions`
| Método | Path | Auth |
|--------|------|------|
| POST | `/tours/{tourId}/main-attractions/replace` | PROVIDER |
| GET | `/tours/{tourId}/main-attractions` | público |

#### `TourIncludesExcludesController` — `/tours/{tourId}/includes-excludes`
| Método | Path | Auth |
|--------|------|------|
| POST | `/tours/{tourId}/includes-excludes/replace` | PROVIDER |
| GET | `/tours/{tourId}/includes-excludes` | público |

#### `TourCancellationPolicyController` — `/tourCancellationPolicy`
| Método | Path | Auth |
|--------|------|------|
| POST | `/tourCancellationPolicy/user/save/{tourId}` | PROVIDER |
| POST | `/tourCancellationPolicy/user/saveList/{tourId}` | PROVIDER |
| POST | `/tourCancellationPolicy/user/replaceAll/{tourId}` | PROVIDER |

#### `TourCancelCategoryController` — `/tourCancelCategory`
| Método | Path | Auth |
|--------|------|------|
| POST | `/tourCancelCategory/admin/save` | ADMIN |
| GET | `/tourCancelCategory/admin/findAll` | ADMIN |
| GET | `/tourCancelCategory/user/getAllTourCancelCategoryList` | público |

---

### 3. Horarios y precios

#### `TourScheduleController` — `/tour-schedules`
| Método | Path | Auth | Roles |
|--------|------|------|-------|
| GET | `/tour-schedules/tours/{tourId}` | JWT | PROVIDER |
| POST | `/tour-schedules/config` | JWT | PROVIDER |
| PUT | `/tour-schedules/config/{configId}` | JWT | PROVIDER |
| GET | `/tour-schedules/config/{configId}` | público | |
| POST | `/tour-schedules/batch` | JWT | PROVIDER |
| GET | `/tour-schedules/templates` | JWT | PROVIDER |
| PUT | `/tour-schedules/tours/{tourId}/percentage` | JWT | BACKOFFICE/ADMIN |
| PUT | `/tour-schedules/tours/{tourId}/percentage/{slotId}` | JWT | BACKOFFICE/ADMIN |

> 📌 **Cambio TC-019 bug 4 (PR #247, 2026-08-12)** — `TourScheduleConfigSlotDto` (payload de `POST /config`, `PUT /config/{configId}` y `POST /batch`) expone ahora el campo opcional `slotPorcentajeTourya: number` (0-100 puntos, `@DecimalMin(0.0)` / `@DecimalMax(100.0)`, nullable). Se persiste **solo si el rol del caller es BACKOFFICE** (`Utils.isTouryaBackoffice(roles)`); PROVIDER lo ignora silenciosamente. Cuando se persiste, el backend recalcula `price = providerPrice × (1 + slotPct)` en el mismo request (idempotente). El endpoint per-schedule `PUT /percentage/{slotId}` no cambió — sigue siendo la vía para overrides puntuales sobre una fecha específica. Ver [RN-015](05-reglas-de-negocio.md) §Refinamiento 2026-08-12.

---

### 4. Búsqueda y públicos

#### `SearchTourScheduleFullController` — `/tour/schedule`
| Método | Path | Auth |
|--------|------|------|
| POST | `/tour/schedule/search` | público u opt-in |

> 📌 **Cambio PR #255 (2026-08-13)** — `SearchTourScheduleFullResponse.AddressResponse` expone ahora el campo opcional `addressType: string` (`AddressTypeEnum.name()` — `"FIXED_LOCATION"` / `"HOTEL_PICKUP"` / etc., nullable si el enricher no matchea). Antes solo el detail traía este dato, obligando al mobile a heurística `empty(city) && empty(address) → HOTEL_PICKUP` (TC-017). Aplica a **todos los endpoints que usan `SearchTourScheduleFullService`**: `POST /tour/schedule/search`, `POST /public/tours/schedule/search` y `POST /wishlist/search`. Implementado con `SearchTourAddressTypeEnricher` (batch load Java, **sin tocar el SP `sp_get_tour_schedule_json`**): recolecta `tour_address` por `tour_id_in` y matchea por tupla `(country_id, state_id, city_id)` — incluye el caso HOTEL_PICKUP con los 3 nulls. Fallback: si el tour tiene una sola dirección, se usa esa. Sin N+1.

#### `PublicController` — `/public`
| Método | Path | Descripción |
|--------|------|-------------|
| GET | `/public/country/getAllCountryList` | |
| GET | `/public/state/getAllStateByCountryIdList/{countryId}` | |
| GET | `/public/city/getAllCityByStateIdList/{stateId}` | |
| POST | `/public/tours/schedule/search` | Búsqueda (SP `sp_get_tour_schedule_json`) |
| GET | `/public/search/locations` | |
| GET | `/public/search/categories` | |
| GET | `/public/search/subcategories` | |
| GET | `/public/tag/categories` | |
| GET | `/public/tags` | |
| GET | `/public/age-price-types` | |
| GET | `/public/consultDataTourById/{tourId}` | público |
| GET | `/public/tour/details/{tourId}` | público |
| GET | `/public/bookings/{bookingId}` | público (acepta `250` o `TB-250`) ⚠️ filtración de PII |

---

### 5. Reservas y carrito

#### `ReservationController` — `/reservations`
| Método | Path | Auth | Roles |
|--------|------|------|-------|
| GET | `/reservations` | JWT | PROVIDER/ADMIN/USER |
| POST | `/reservations` | JWT | USER — crea holds temporales |
| GET | `/reservations/{id}` | público | |
| GET | `/reservations/qr/{qrUrl}` | público | |
| GET | `/reservations/payment/{paymentId}` | público | |
| GET | `/reservations/delivery-status/{status}` | público | |
| GET | `/reservations/date/{date}` | público | |
| GET | `/reservations/date-range?startDate=&endDate=` | público | |
| GET | `/reservations/payment/{paymentId}/exists` | público | |
| POST | `/reservations/{id}/qr/regenerate` | público | |
| DELETE | `/reservations/{id}/qr` | público | |
| POST | `/reservations/{id}/consume` | público | Marca DELIVERED |
| PUT | `/reservations/{id}/cancel` | JWT | USER/ADMIN |
| PUT | `/reservations/{id}/cancel/rain` | JWT | ADMIN |
| GET | `/reservations/{id}/reschedule/validate` | JWT | USER |
| PUT | `/reservations/{id}/reschedule` | JWT | USER |

> 📌 **Cambio PR #255 (2026-08-13)** — `ReservationResponse` expone ahora el campo opcional `travelerBreakdown: List<TravelerBreakdownDto>` (reusa el tipo ya existente del provider). Cada entrada trae `ageType` (`ADULT` / `CHILD` / `INFANT`), `count`, `unitPrice` y `providerUnitPrice`. Aplica a todos los endpoints que devuelven `ReservationResponse` — vista turista `GET /reservations/{id}`, `GET /reservations`, `GET /reservations/qr/{qrUrl}`, `GET /reservations/payment/{paymentId}`, listas por fecha / estado, etc. Antes solo la vista provider (`ReservationDetailsResponse` vía `sp_get_provider_reservations`, TC-016) traía este desglose; el turista tenía únicamente `priceBreakdown: ReservationPriceBreakdownResponse` (datos equivalentes pero distinto shape, agrupado por `age_price_type` de la tarifa). **Coexisten** ambos en la vista turista — `priceBreakdown` sigue como fuente autoritativa para totales, `travelerBreakdown` existe para que el mobile reuse el mismo componente de UI entre provider y turista sin duplicar renderers. Poblado en `ReservationService.enrichReservationResponse` desde `shopping_cart_item_detail` del carrito original.

#### `ShoppingCartController` — `/shopping-cart`
| Método | Path | Auth | Roles |
|--------|------|------|-------|
| POST | `/shopping-cart` | JWT | USER |
| POST | `/shopping-cart/items` | JWT | USER |
| GET | `/shopping-cart` | JWT | USER |
| GET | `/shopping-cart/user` | JWT | USER |
| GET | `/shopping-cart/{cartId}` | JWT | USER |
| GET | `/shopping-cart/{cartId}/details` | JWT | USER |
| DELETE | `/shopping-cart/{cartId}/items/{itemId}` | JWT | USER |
| PUT | `/shopping-cart/{cartId}/items/{itemId}/status` | JWT | USER |
| POST | `/shopping-cart/{cartId}/checkout` | JWT | USER |
| DELETE | `/shopping-cart/{cartId}/clear` | JWT | USER |

> 📌 **Cambio PR #255 (2026-08-13)** — `ShoppingCartItemResponse` expone ahora el campo opcional `addressType: string` (`AddressTypeEnum.name()`, nullable). Aplica a todos los endpoints que devuelven items del cart — `POST /shopping-cart`, `GET /shopping-cart`, `GET /shopping-cart/user`, `GET /shopping-cart/{cartId}`, `GET /shopping-cart/{cartId}/details`, `POST /shopping-cart/items`. Se mapea desde el primer `tour_address` asociado al tour del item (mismo patrón que `city` / `department` ya presentes). **`null` para items `SERVICE`** — no tienen dirección asociada. Cierra la deuda mobile TC-017 (antes el cliente heurística sobre `city`/`address` vacíos para inferir Hotel Pickup).

#### `TourReservationController` — `/tour-reservations` (legacy)
| Método | Path | Notas |
|--------|------|-------|
| POST | `/tour-reservations` | legacy |
| GET | `/tour-reservations/{id}` | legacy |
| GET | `/tour-reservations/my-reservations` | legacy |
| PUT | `/tour-reservations/{id}/cancel` | legacy |

---

### 6. Pagos

#### `PaymentController` — `/payment`
| Método | Path | Auth |
|--------|------|------|
| POST | `/payment` | JWT USER |
| GET | `/payment/{paymentId}` | JWT |
| GET | `/payment/transaction/{transactionId}` | JWT |
| GET | `/payment/qr/{qrUrl}` | JWT |

#### `ReferenceController` — `/reference`
| Método | Path | Notas |
|--------|------|-------|
| GET | `/reference/generate` | público — genera referencia Wompi + SHA256 |

---

### 7. Reviews

#### `ReviewController` — `/public` (algunos públicos, algunos JWT)
| Método | Path | Auth |
|--------|------|------|
| GET | `/public/search/pending-reviews` | JWT USER |
| GET | `/public/search/reviews` | JWT (filtrado por rol) |
| POST | `/public/save/review` | JWT USER (multipart, ≤5 fotos) |
| PATCH | `/public/save/review/{reviewId}` | JWT USER |
| GET | `/public/tour/{tourId}/reviews/summary` | público |
| GET | `/public/tour/{tourId}/reviews` | público |
| GET | `/public/review/reasons` | público |
| GET | `/public/review/reasons/by-rating?rating=N` | público |

---

### 8. Proveedores y KYB

#### `ProviderController` — `/provider`
| Método | Path | Auth | Roles |
|--------|------|------|-------|
| PUT | `/provider/user/update` | JWT | PROVIDER |
| GET | `/provider/user/consultData` | JWT | PROVIDER |
| GET | `/provider/admin/findAll` | JWT | ADMIN |
| GET | `/provider/admin/consultDataById/{id}` | JWT | ADMIN |
| DELETE | `/provider/admin/deleteById/{id}` | JWT | ADMIN |
| PUT | `/provider/admin/activeOrInactiveById/{id}?status=` | JWT | ADMIN |

#### `RequestProviderController` — `/requestProvider`
| Método | Path | Auth | Roles |
|--------|------|------|-------|
| POST | `/requestProvider/user/save` | JWT | USER |
| GET | `/requestProvider/user/consultData` | JWT | USER |
| PUT | `/requestProvider/user/send` | JWT | USER |
| GET | `/requestProvider/admin/findAll` | JWT | ADMIN |
| GET | `/requestProvider/admin/consultDataById/{id}` | JWT | ADMIN |
| PUT | `/requestProvider/admin/approve/{id}` | JWT | ADMIN |
| PUT | `/requestProvider/admin/cancel/{id}` | JWT | ADMIN |
| PUT | `/requestProvider/admin/incomplete/{id}` | JWT | ADMIN |
| PUT | `/requestProvider/admin/pre-approve/{id}` | JWT | ADMIN |

#### `RequestProviderDocumentTypeController` — `/requestProviderDocumentType`
| Método | Path | Auth |
|--------|------|------|
| POST | `/requestProviderDocumentType/admin/save` | ADMIN |
| GET | `/requestProviderDocumentType/admin/findAll` | ADMIN |
| GET | `/requestProviderDocumentType/user/getAllRequestProviderDocumentTypeList` | público |

#### `RequestProviderGalleryController` — `/requestProvider/{requestId}/gallery`
| Método | Path | Auth |
|--------|------|------|
| POST | `/requestProvider/{requestId}/gallery` | JWT USER (multipart) |
| GET | `/requestProvider/{requestId}/gallery` | JWT USER |

---

### 9. Payouts

#### `ProviderPayoutOrderController` — `/provider/payout-orders`
| Método | Path | Auth | Roles |
|--------|------|------|-------|
| GET | `/provider/payout-orders` | JWT | PROVIDER |
| GET | `/provider/payout-orders/{orderId}` | JWT | PROVIDER |
| GET | `/provider/payout-orders/admin` | JWT | ADMIN |
| GET | `/provider/payout-orders/admin/{orderId}` | JWT | ADMIN |
| POST | `/provider/payout-orders/admin/{orderId}/proof` | JWT | ADMIN (multipart) |

---

### 10. Otros

#### `WishlistController` — `/wishlist`
| Método | Path | Auth |
|--------|------|------|
| GET | `/wishlist` | JWT USER |
| POST | `/wishlist` | JWT USER |
| DELETE | `/wishlist` | JWT USER |
| POST | `/wishlist/search` | JWT USER |

#### `CreditController` — `/credits`
| Método | Path | Auth |
|--------|------|------|
| GET | `/credits?status=` | JWT |
| POST | `/credits/reserve` | JWT USER |
| GET | `/credits/tourist-lookup?documentNumber=` | JWT |
| POST | `/credits/{creditId}/transfer` | JWT USER |
| POST | `/credits/{creditId}/request-refund` | JWT USER — TC-022 #253 |

**TC-022 `/credits/{creditId}/request-refund`** (turista solicita devolución en efectivo — ver [RN-062](05-reglas-de-negocio.md#rn-062)):

- Body: vacío.
- Response 200: `CreditResponse` con `status: "REFUND_REQUESTED"` y `refundRequestedAt` seteado (Bogota).
- Guards:
  - Ownership: el crédito debe pertenecer al usuario del JWT (`credit.userId == auth.principal.id`), sino `OperationNotPermittedException`.
  - Estado: `status == CREATED`, sino `OperationNotPermittedException` con mensaje del estado actual.
  - Saldo libre: `(amount - reservedAmount) > 0`, sino `OperationNotPermittedException` ("no free balance available for refund").
  - No vencido: `expirationDate >= today (Bogota)`, sino `OperationNotPermittedException` ("Cannot request refund on an expired credit").
- Efectos: transición `CREATED → REFUND_REQUESTED`, persiste `refund_requested_at` (Bogota). El crédito ya no se puede usar en checkout ni transferir (checks existentes `status != CREATED` lo excluyen automáticamente).
- Errores: 400 (transición no permitida), 404 (crédito no existe).

#### `AdminCreditController` — `/admin/credits` (TC-022 #253)

Endpoints backoffice/admin del flujo de devolución de créditos. Requieren rol **ADMIN** o **BACKOFFICE_OPERATION** — guard aplicado en el service vía `Utils.isTouryaBackoffice(roles)` (retorna `InsufficientPrivilegesException` → 403).

| Método | Path | Auth | Roles |
|--------|------|------|-------|
| GET | `/admin/credits?status=&page=&size=&sort=` | JWT | ADMIN + BACKOFFICE_OPERATION |
| POST | `/admin/credits/{creditId}/upload-refund-proof` (multipart) | JWT | ADMIN + BACKOFFICE_OPERATION |

**`GET /admin/credits`** — listado global paginado de créditos con datos del turista embebidos:

- Query params: `status` (opcional, filtra por `CreditStatusEnum`) + paginación estándar Spring (`page`, `size`, `sort`).
- Response 200: `Page<CreditResponse>` con campos extra `touristName` y `touristEmail` en cada item.
- Errores: 403 (rol insuficiente).

**`POST /admin/credits/{creditId}/upload-refund-proof`** — sube el comprobante y marca la devolución completa (transición `REFUND_REQUESTED → REFUNDED`):

- `Content-Type: multipart/form-data`.
- Part `proof` (requerido): archivo del comprobante. **Validación**: MIME `application/pdf` / `image/png` / `image/jpeg` / `image/jpg`; tamaño ≤ 5MB (`REFUND_PROOF_MAX_BYTES`); no vacío.
- Response 200: `CreditResponse` con `status: "REFUNDED"`, `refundedAt` (Bogota) y `refundProofUrl` (URL pública).
- Efectos:
  - Sube el archivo a `IStorageService` (S3 o GCS según env — `GcsStorageService` es la implementación en Cloud Run) al path `credit-refund-proofs/{creditId}/...` con nombre generado por el storage.
  - Persiste transición + timestamps + URL.
- Guards:
  - Rol backoffice (403 si no).
  - Archivo válido (400 con `IllegalArgumentException` si formato/tamaño/vacío no cumple).
  - Estado: `status == REFUND_REQUESTED`, sino `OperationNotPermittedException` (400).
- Errores: 400 (transición o archivo inválido), 403 (rol insuficiente), 404 (crédito no existe).

#### `MaritimActivityReportController` — `/maritime-activity-reports`
| Método | Path | Auth | Roles |
|--------|------|------|-------|
| POST | `/maritime-activity-reports` | JWT | BACKOFFICE/ADMIN |
| GET | `/maritime-activity-reports` | JWT | |
| GET | `/maritime-activity-reports/{id}` | JWT | |
| PUT | `/maritime-activity-reports/{id}` | JWT | |
| DELETE | `/maritime-activity-reports/{id}` | JWT | |
| GET | `/maritime-activity-reports/date?reportDate=` | JWT | |
| GET | `/maritime-activity-reports/location?countryId=&stateId=&cityId=` | JWT | |

#### `ServiceController` — `/api/v1/services`
| Método | Path | Auth |
|--------|------|------|
| GET | `/services` | JWT |
| GET | `/services/{id}` | JWT |
| GET | `/services/active` | JWT |
| GET | `/services/type/{serviceTypeId}` | JWT |
| GET | `/services/active/type/{serviceTypeId}` | JWT |

#### `ServiceTypeController` — `/api/v1/service-types`
| Método | Path | Auth |
|--------|------|------|
| GET | `/service-types` | JWT |
| GET | `/service-types/{id}` | JWT |
| GET | `/service-types/active` | JWT |
| GET | `/service-types/name/{name}` | JWT |

#### `AppConfigController` — `/config`
| Método | Path | Auth |
|--------|------|------|
| GET | `/config/{configKey}` | JWT |
| PUT | `/config/{configKey}` | JWT + ADMIN. Body: `{"value": {...}, "description": "..."}`. Upsert idempotente. Agregado 2026-07-09 (PR #160, BE-09) |

Claves configurables disponibles hoy (patrón JSON `{"value": N}` para escalares):
- `HOLD_MINUTES` — minutos del hold del carrito (default 15)
- `PAYOUT_BUFFER_DAYS` — días entre reserva y payout available (default 2)
- `CREDIT_EXPIRATION_MONTHS` — meses de vigencia de un crédito (default 6)
- `GALLERY_MAX_SIZE_MB` — tamaño máximo por imagen en galería (default 5) — agregado 2026-07-09
- `GALLERY_MIN_WIDTH_PX` — ancho mínimo aceptable para imagen (default 800) — agregado 2026-07-09
- `GALLERY_MAX_IMAGES_PER_TOUR` — cuenta máxima por tour (default 7) — agregado 2026-07-09
- `KYB_REQUIRE_MANDATORY_DOCS` — feature flag: 1 activa RN-045 en `PUT /requestProvider/user/send` (default 0=OFF) — agregado 2026-07-09
- `AUTH_RATE_LIMIT_ENABLED` — feature flag: 1 activa rate limiting en `/auth/**` (default 0=OFF) — agregado 2026-07-09
- `AUTH_RATE_LIMIT_PER_MINUTE` — requests máx por IP por minuto en `/auth/**` (default 60) — agregado 2026-07-09
- `AUTH_LOCKOUT_ENABLED` — feature flag: 1 activa lockout por cuenta tras N intentos fallidos (default 0=OFF) — agregado 2026-07-09
- `AUTH_LOCKOUT_MAX_ATTEMPTS` — intentos fallidos consecutivos antes de bloquear (default 5) — agregado 2026-07-09
- `AUTH_LOCKOUT_BASE_BACKOFF_SECONDS` — base del backoff exponencial en segundos (default 60, cap 24h) — agregado 2026-07-09
- `CANCELLATION_POLICY` — políticas de cancelación i18n (JSON estructurado, no usa el wrapper)

#### `AgentController` — `/agents` (IA-02, agregado 2026-08-14)

Endpoints REST de los agentes IA. Hoy solo expone Travel Concierge; futuros agentes (Support 24/7, Operator Support, etc.) se agregarán aquí.

| Método | Path | Auth |
|--------|------|------|
| POST | `/agents/travel-concierge/chat` | JWT USER |

**`POST /agents/travel-concierge/chat`** — Agente 1 del [doc 16](16-agentes-ia.md#agente-1--travel-concierge). El agente puede responder texto natural o disparar function calls contra búsqueda y carrito (loop max 5 iteraciones).

- Body (`ConciergeChatRequest`):
  - `sessionId` (string, required, max 128) — identifica la conversación multi-turno del turista. Persistido en `agent_audit_log.metadata->>'session_id'`. El cliente lo genera (UUID) en el primer mensaje y lo mantiene por sesión.
  - `userMessage` (string, required, max 2000) — texto del turista en es/en/pt.
  - `locale` (string, opcional, `es`/`en`/`pt`) — sugerencia explícita de idioma.
  - `tourId` (int, opcional) — ID del tour foco de la conversación; el agente inyecta la ficha completa al contexto.
  - `cartId` (long, opcional) — no es requerido (el agente resuelve el carrito activo del usuario si aplica).
- Response 200 (`ConciergeChatResponse`):
  - `assistantMessage` (string) — respuesta natural al turista en su idioma.
  - `actionsExecuted[]` — lista de function calls disparados por el LLM (`search_tours`, `get_tour_detail`, `add_to_cart`, `get_cart`) con input parseado + `success` + `error?`.
  - `fraudSuspected` (boolean) — flag interno del guard (3+ pagos fallidos por sessionId). **NUNCA se le comunica al turista** — la API lo expone al BFF/frontend para tagging operativo.
  - `escalatedToHuman` (boolean) — `true` cuando un guardrail (intento de leak de secretos, budget agotado, error irrecuperable) bloqueó la request y el agente delega a humano.
  - `sessionId` (string) — eco del input para conveniencia del cliente.
- Provider LLM: Vertex AI Gemini 2.5 Pro (`AGENTS_PROVIDER=gemini`, default). Anthropic disponible como alternativa (`AGENTS_PROVIDER=anthropic + ANTHROPIC_API_KEY`).
- Guardrails:
  - Deny-list de palabras clave sensibles (`WOMPI_INTEGRITY_SECRET`, `JWT_SECRET`, `ANTHROPIC_API_KEY`, etc.) en el `userMessage` → responde genérico + audit `result_type=rejected`, `escalated_to_human=true`.
  - Scrub de `providerPrice` / `slotPercentageTourya` / `slotPorcentajeTourya` / `porcentajeTourya` del contexto (cart + tour detail) antes de mandarlo al LLM — defense-in-depth aunque los DTOs actuales ya no exponen esos campos al turista.
  - `BudgetGuard` (IA-01) por `AGENT_BUDGET_TRAVELCONCIERGE_USD_MONTHLY` (default $30 USD/mes) — si se agota escala a humano.
- Auditoría: cada llamada persiste una fila en `agent_audit_log` (Principio rector #4 del doc 16) vía `AgentAuditWriter @Async`. Metadata JSONB incluye `session_id`, `fraud_suspected`, `actions_executed[]`, `escalated_to_human`.
- Errores: 400 (validación DTO), 401 (JWT), 429 (rate limit si activo).

#### `OperatorSupportController` — `/agents/operator-support` (IA-07, agregado 2026-08-14)

Endpoints REST del agente **Operator Support** (Agente 4 del [doc 16](16-agentes-ia.md#agente-4--operator-support)). Cuatro capabilities action-specific (no chat unificado): cada endpoint es un caso de uso puntual del wizard de tour + gestión de reseñas del provider. Todos requieren JWT con rol `PROVIDER` o `PROVIDER_OPERATOR`.

| Método | Path | Auth |
|--------|------|------|
| POST | `/agents/operator-support/suggest-tour-content` | JWT PROVIDER / PROVIDER_OPERATOR |
| POST | `/agents/operator-support/price-alert/{tourId}` | JWT PROVIDER / PROVIDER_OPERATOR |
| POST | `/agents/operator-support/draft-review-reply/{reviewId}` | JWT PROVIDER / PROVIDER_OPERATOR |
| POST | `/agents/operator-support/validate-gallery` | JWT PROVIDER / PROVIDER_OPERATOR |

**`POST /agents/operator-support/suggest-tour-content`** — Genera un borrador de contenido SEO para el wizard.
- Body (`SuggestTourContentRequest`):
  - `tourId` (int, opcional) — para editar tours existentes; valida ownership antes de ejecutar.
  - `draft` (object, required) — datos parciales: `name`, `categoryId`, `subcategory`, `durationMinutes`, `minAge`, `priceType`, `isUnlimitedCapacity`, `currentDescription`, `maxPeople` (todos opcionales).
- Response 200 (`TourContentSuggestion`):
  - `nameSuggestions[]` (3 nombres) + `descriptionSuggestion` (200-400 palabras en español, RN-011) + `tagSuggestions[]` (5-10 slugs del catálogo `tags`) + `reasoning` + `escalatedToHuman`.
- Provider LLM: **Gemini 2.5 Pro**, 1 call (generación pura, sin function calling).

**`POST /agents/operator-support/price-alert/{tourId}`** — Analiza si el precio del tour está alineado con comparables.
- Response 200 (`PriceAlert`):
  - `severity` (`OK` / `WARN` / `CRITICAL` / `UNKNOWN`) — heurística: ≤15% del median = OK, 15-35% = WARN, >35% = CRITICAL, <3 comparables = UNKNOWN.
  - `currentAvgPrice` (BigDecimal) — promedio ADULT del tour actual (precio público, nunca `providerPrice` interno).
  - `comparablePriceRange` (`{min, max, median}`) — rango observado en tours con la misma subcategoría, hasta 20 comparables.
  - `comparablesCount` (int) + `reasoning` + `escalatedToHuman`.
- **RN-014**: solo alerta, jamás modifica `providerPrice`.
- Provider LLM: Gemini 2.5 Pro con heurística fallback si el JSON es inválido (el service computa severity localmente).

**`POST /agents/operator-support/draft-review-reply/{reviewId}`** — Borrador de respuesta a una reseña.
- Response 200 (`DraftReviewReplyResponse`):
  - `draftText` (60-150 palabras) + `detectedLocale` (`es` / `en` / `pt`) + `tone` (`PROFESSIONAL` / `WARM` / `APOLOGETIC`) + `reasoning` + `escalatedToHuman`.
- Autorización: verifica que `review.tourId` pertenece a un tour del provider del usuario autenticado.
- El backend **no publica** la respuesta — la devuelve al frontend. El operador aprueba y llama a `PATCH /public/save/review/{reviewId}` (endpoint existente en `ReviewController`).
- Provider LLM: Gemini 2.5 Pro, 1 call.

**`POST /agents/operator-support/validate-gallery`** — Validación pre-upload de metadata de galería (RN-013). **Cero costo LLM**.
- Body (`ValidateGalleryRequest`):
  - `images[]` — array de `{filename, sizeBytes, widthPx, heightPx, format}`. El frontend envía solo metadata (no bytes).
- Response 200 (`ValidateGalleryResponse`):
  - `isValid` (boolean) + `issues[]` (`{severity ERROR|WARNING, code, message, fileIndex}`) + `suggestions[]`.
- Reglas: max 7 imágenes (`GALLERY_MAX_IMAGES_PER_TOUR`), max 5 MB (`GALLERY_MAX_SIZE_MB`), min 800px (`GALLERY_MIN_WIDTH_PX`), horizontal, formato JPEG/PNG/WebP + WARNING advisory si el ancho es menor a 1920px. Reusa los mismos umbrales de `app_config` que `GalleryValidator`.

**Guardrails comunes (idénticos a IA-02)**:
- Deny-list de secretos (`WOMPI_INTEGRITY_SECRET`, `JWT_SECRET`, `ANTHROPIC_API_KEY`, `WOMPI_EVENTS_SECRET`, `FIREBASE_ADMIN_SDK_JSON`, `GEMINI_API_KEY`) en cualquier input textual → `escalatedToHuman=true` + audit `result_type=rejected` sin llamar al LLM.
- Scrub recursivo de `providerPrice` / `slotPercentageTourya` / `slotPorcentajeTourya` / `porcentajeTourya` en cualquier JSON al LLM.
- `BudgetGuard` por `AGENT_BUDGET_OPERATORSUPPORT_USD_MONTHLY` (default $20 USD/mes) — si se agota escala a humano.
- Autorización owner-based: el service valida que el `PROVIDER` autenticado sea el owner del tour (`tour.provider.id == providerService.findByUser(user).id`) — para `draft-review-reply` la validación es transitiva vía `review.tourId`.

**Traducción es→en/pt-BR (IA-09 cerrado 2026-08-15)**: el service sigue devolviendo solo español (RN-011: el operador aprueba en su idioma). La traducción de los campos JSONB del tour ocurre en background cuando el operador guarda vía `TourService.saveCreateOrUpdateFullData` (ver §Agente 4 en [16 — Agentes IA](16-agentes-ia.md) y §5 en [11 — Integraciones](11-integraciones.md)).

**Feature flag**: `agents.operator.enabled=${AGENTS_OPERATOR_ENABLED:true}` para apagar el agente sin re-deploy.

Errores: 400 (validación DTO), 401 (falta rol PROVIDER/PROVIDER_OPERATOR o el recurso no pertenece al provider — `InsufficientPrivilegesException`), 404 (tour/review no encontrado).

#### `AgentObservabilityController` — `/admin/agents` (IA-11, agregado 2026-08-15)

Dashboard de observabilidad de agentes IA para el admin. Todos los endpoints requieren JWT + rol `ADMIN` o `BACKOFFICE_OPERATION` (guard en `AgentObservabilityService` vía `Utils.isTouryaBackoffice`, mismo patrón que `AdminCreditController` — TC-022 #253). Nunca exponen `prompt_input`, `result_json` ni el `metadata` completo — solo agregados numéricos derivados de `agent_audit_log`. Respuestas con `Cache-Control: private, max-age=60` para amortiguar refresh masivo del frontend.

| Método | Path | Auth |
|--------|------|------|
| GET | `/admin/agents/summary` | JWT ADMIN / BACKOFFICE_OPERATION |
| GET | `/admin/agents/timeseries` | JWT ADMIN / BACKOFFICE_OPERATION |
| GET | `/admin/agents/latency` | JWT ADMIN / BACKOFFICE_OPERATION |
| GET | `/admin/agents/top-consumers` | JWT ADMIN / BACKOFFICE_OPERATION |
| GET | `/admin/agents/result-types` | JWT ADMIN / BACKOFFICE_OPERATION |
| GET | `/admin/agents/overrides` | JWT ADMIN / BACKOFFICE_OPERATION |

**`GET /admin/agents/summary?from&to&agent`** — 1 fila por agente en el rango.
- Query params: `from`, `to` (YYYY-MM-DD, UTC, inclusive; default últimos 30 días), `agent` (opcional, ej. `TravelConcierge` | `OperatorSupport`).
- Response 200: `List<AgentSummaryDto>` con `{agent, totalCalls, tokensIn, tokensOut, costUsd, avgLatencyMs, successRate, escalatedRate}`. `successRate` agrupa `suggestion + autonomous_action`; `escalatedRate` mira `metadata->>'escalated_to_human' = true`.

**`GET /admin/agents/timeseries?from&to&agent&granularity`** — serie temporal.
- Query params: `granularity` ∈ `day|week|month` (default `day`, valores no whitelisted → `day`). El bucket sale de `date_trunc(:granularity, created_at)`.
- Response 200: `List<AgentTimeseriesPointDto>` con `{date, agent, calls, costUsd, avgLatencyMs}`.

**`GET /admin/agents/latency?from&to&agent&capability`** — percentiles p50/p95/p99/min/max.
- Query params: `capability` (opcional; hoy sólo `OperatorSupport` persiste `metadata->>'capability'` con valores `suggest_tour_content | price_alert | draft_review_reply`).
- Response 200: `List<LatencyDistributionDto>` con `{agent, capability, p50, p95, p99, min, max}`. Los percentiles vienen de `percentile_cont(0.5/0.95/0.99) WITHIN GROUP (ORDER BY duration_ms)`. Si sin `capability`, `LatencyDistributionDto.capability = null`.

**`GET /admin/agents/top-consumers?from&to&agent&limit`** — top-N usuarios por call count.
- Query params: `limit` (default 10, max 100).
- Response 200: `List<TopConsumerDto>` con `{userId, userEmail, calls, costUsd}` ordenado `calls DESC, costUsd DESC`. `LEFT JOIN _user` para el email; `user_id`/`userEmail` pueden ser `null` (calls anónimos del Concierge pre-login).

**`GET /admin/agents/result-types?from&to`** — distribución de `result_type` por agente.
- Response 200: `List<ResultTypeDistributionDto>` con `{agent, success, error, rejected}` donde `success = suggestion + autonomous_action` (los dos valores que hoy representan "el agente cerró OK", ver migración `076_agent_audit_log.sql`).

**`GET /admin/agents/overrides?from&to&agent`** — override rate proxy MVP (Opción A).
- Response 200: `List<OverrideMetricsDto>` con `{agent, capability, totalSuggestions, escalated, errored, escalatedRate, errorRate, overrideRateProxy}`. Se agrupa por `agent_name + metadata->>'capability'`. `overrideRateProxy = escalatedRate + errorRate` — aproximación a "casos donde el agente no cerró solo".
- **Deuda IA-11b (Opción B)**: para computar override rate verdadero hace falta agregar columna `human_outcome VARCHAR(20) NULL` en `agent_audit_log` (`KEPT|EDITED|DISCARDED`) + hook desde el frontend en el wizard de tour que reporta el outcome cuando el provider guarda. Documentado como deuda separada en [16 — Agentes IA §Observabilidad](16-agentes-ia.md#observabilidad-de-agentes).

**Índices que sostienen estos queries**: los tres creados en la migración `076_agent_audit_log.sql` (`idx_agent_audit_log_agent_date (agent_name, created_at DESC)`, `idx_agent_audit_log_user_date (user_id, created_at DESC) WHERE user_id IS NOT NULL`, `idx_agent_audit_log_metadata` GIN sobre `metadata`) — no requirió migración nueva.

Errores: 401 (falta rol ADMIN o BACKOFFICE_OPERATION — `InsufficientPrivilegesException`).

#### `AdminTourTranslationController` — `/admin/tours` (IA-09)
| Método | Path | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/admin/tours/{tourId}/retranslate` | JWT ADMIN | Re-dispara la traducción es→en/pt-BR de un tour existente vía Google Cloud Translation. Solo rellena campos en/pt vacíos (respeta lo que el provider haya escrito). Uso: (a) backfill de tours legacy sin en/pt, (b) testing end-to-end del pipeline, (c) recovery si el listener AFTER_COMMIT falló. Async — la respuesta HTTP confirma el dispatch, no el resultado (revisar logs Cloud Run). Response: `{tourId, translationEnabled, dispatched, message}`. |

Errores: 401 (falta rol ADMIN), 404 (tour no encontrado).

#### `TestController` — `/api/v1`
| Método | Path | Auth |
|--------|------|------|
| GET | `/connectionTest` | público |

---

## Endpoints con manejo especial

### Multipart (file upload)
- `PUT /tourist/profile/photo` (≤1MB)
- `POST /tours/{tourId}/gallery/sync`
- `POST /tours/{tourId}/gallery/syncWithUpdate`
- `POST /public/save/review` (≤5 fotos)
- `PATCH /public/save/review/{reviewId}` (≤5 fotos)
- `POST /requestProvider/{requestId}/gallery`
- `POST /provider/payout-orders/admin/{orderId}/proof`
- `POST /admin/credits/{creditId}/upload-refund-proof` (JPG/PNG/PDF, ≤5MB) — TC-022 #253, ADMIN/BACKOFFICE_OPERATION

### Auth opt-in (público con datos extra si hay JWT)
- `GET /tour/details/{tourId}`
- `POST /public/tours/schedule/search`
- `GET /public/consultDataTourById/{tourId}`
- `GET /public/tour/details/{tourId}`

### Endpoints sin auth pero con datos sensibles ⚠️
- `GET /public/bookings/{bookingId}` — devuelve datos del pagador (PII). **Vulnerabilidad documentada**.

---

## Error handling

✅ `GlobalExceptionHandler` con `BusinessErrorCodes` enum genera respuestas tipo:

```json
{
  "code": "USER_EMAIL_ALREADY_EXISTS",
  "message": "El email ya está registrado",
  "errors": ["..."]
}
```

Códigos comunes:
- `USER_EMAIL_ALREADY_EXISTS`
- `USER_NOT_FOUND`
- `TOUR_NOT_FOUND`
- `INSUFFICIENT_CAPACITY`
- `RESERVATION_CANNOT_CANCEL`
- `CREDIT_INSUFFICIENT`

📌 PENDIENTE LUIS — confirmar si quieren un manual de error codes para frontend / clientes.

---

## CORS

✅ Configurado en `BeansConfig.java`. Orígenes permitidos (actualizado 2026-07-08, PR #150 SEC-11):

```
http://localhost:4200                                                    (dev local Angular)
http://localhost:8080                                                    (dev local)
http://localhost:8100                                                    (dev local Ionic/MAUI)
https://tourya-dev-front-640622322458.us-east1.run.app                   (Cloud Run front dev)
https://tourya-dev-api-640622322458.us-east1.run.app                     (Cloud Run api dev)
https://tourya.co                                                        (dominio custom prod)
https://www.tourya.co                                                    (dominio custom prod con www)
```

✅ IPs AWS legacy y URLs viejas de Cloud Run eliminadas.

Métodos permitidos: GET, POST, DELETE, PUT, PATCH.
Headers permitidos: Origin, Content-Type, Accept, Authorization.

---

## Versionado

✅ Tourya está en `/api/v1/`. **No hay v2** ni planes documentados.

📌 PENDIENTE LUIS — ¿se planea API pública para terceros? (justificaría versionado más estricto).

---

## Documentación interactiva

✅ Swagger UI activa en `/api/v1/swagger-ui/index.html`. La config (`OpenApiConfig.java`) define dos servers:
- DEV: `https://tourya-dev-api-5j2nd2oflq-ue.a.run.app`
- LOCAL: `http://localhost:8088`

⚠️ Sin auth en Swagger UI — accesible sin restricciones. En producción esto debería protegerse.

---

## Endpoints heredados (Postman docs)

✅ El equipo mantiene colecciones Postman en:
- `tourya-api/CURL_POSTMAN.txt`
- `tourya-api/CURL_POSTMAN_NUEVOS_ENDPOINTS.txt`
- `tourya-api/CURL_HOY.txt`
- `tourya-api/CURL_HOY_PEGAR_POWERSHELL.txt`
- `tourya-api/CURL_PRUEBAS_FIXES_JUN11.txt`

Útiles para entender ejemplos reales de payload.

---

## Resumen

| Métrica | Valor |
|---------|-------|
| Total controllers | 35 |
| Total endpoints | 150+ |
| Endpoints públicos | ~30 |
| Endpoints multipart | 7 |
| Endpoints con SP de PostgreSQL | 2 (búsqueda y conteo) |
| Endpoints legacy (a deprecar) | 4 (`TourReservationController`) |
