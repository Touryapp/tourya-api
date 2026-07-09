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
| GET | `/tour/user/findAllByUser` | JWT | PROVIDER |
| POST | `/tour/user/saveAll` | JWT | PROVIDER |
| GET | `/tour/user/consultDataTourById/{tourId}` | JWT | PROVIDER |
| PUT | `/tour/user/submitTourById/{tourId}` | JWT | PROVIDER |
| GET | `/tour/details/{tourId}` | público u opt-in | Todos |

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

---

### 4. Búsqueda y públicos

#### `SearchTourScheduleFullController` — `/tour/schedule`
| Método | Path | Auth |
|--------|------|------|
| POST | `/tour/schedule/search` | público u opt-in |

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
