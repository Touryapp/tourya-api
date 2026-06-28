# 06 — Flujos y eventos

Los **8 flujos críticos** de Tourya, paso a paso, con entidades modificadas y eventos async generados. Todos basados en el código actual.

---

## Flujo 1 — Registro y activación de cuenta

### Entrada
`POST /api/v1/auth/register` — body: `RegistrationRequest` (email, firstname, lastname, password).

### Pasos

1. **`AuthenticationController.register()`** → llama a `AuthenticationService.register()`.

2. **Validaciones**:
   - Formato email (`EmailInvalidFormatException`).
   - Email no existe (`EmailAlreadyExistsException`).

3. **Crear usuario**:
   - Email a minúsculas.
   - Password con BCrypt.
   - `enabled = false`, `accountLocked = false`.
   - Asigna rol `USER`.
   - Persiste `User`.

4. **Generar y enviar token**:
   - 6 dígitos numéricos.
   - `Token` persistido con `expiresAt = now + 15 min`.
   - **@Async** → `EmailService.sendEmail()` con template `ACTIVATE_ACCOUNT`.
   - Email contiene URL: `{ACTIVATION_URL}{token}` + código en cuerpo.

5. **Respuesta**: HTTP 202 Accepted (sin body).

### Activación

`GET /api/v1/auth/activate-account?token={6-digit-code}`:
- Si token expiró: regenera + reenvía email + lanza error.
- Si válido: `user.enabled = true`, `token.validatedAt = now`.

### Entidades modificadas
- `User` (creado, `enabled` true al activar)
- `Token` (creado, `validatedAt` set)

### Eventos async
- Envío de email (vía `@Async`)

### Final
Usuario puede hacer `POST /auth/authenticate` y recibir JWT.

---

## Flujo 2 — Creación de tour por proveedor

### Entrada
`POST /api/v1/tour/user/saveAll` (JWT con rol PROVIDER) — body: `TourFullDataRequest`.

### Pasos

1. **`TourController.saveCreateFullData()`** → `TourService.saveCreateOrUpdateFullData()`.

2. **Branch**:
   - Si `id == null` → `processCreateTourFullData()`.
   - Si `id != null` → `processUpdateTourFullData()`.

3. **Para creación**:
   - Resuelve `Provider` activo del JWT.
   - Mapea request → `Tour` con `status = CREATED`.
   - Persiste `Tour`.

4. **Crea entidades anidadas (en la misma transacción)**:
   - `tourTagsRepository.replaceTourTags()` — tags.
   - `TourAddress` (puede haber varios: encuentro, finalización, recogida).
   - `TourMainAttraction` (atracciones).
   - `TourIncludesExcludes` (incluye/no incluye).
   - `TourFaq`.
   - `TourItinerary`.
   - `TourCancellationPolicy`.

5. **TranslatedField**:
   - Solo se carga español (en/pt suelen quedar vacíos).
   - `TranslatedFieldConverter` serializa a JSONB.

6. **Galería e imágenes**:
   - Se gestionan en endpoints separados: `POST /tours/{tourId}/gallery/sync` (multipart).

7. **Enviar a aprobación**:
   - El proveedor llama `PUT /tour/user/submitTourById/{id}` → `status = SUBMITTED`.

### Entidades modificadas
- `Tour`, `TourAddress`, `TourMainAttraction`, `TourIncludesExcludes`, `TourFaq`, `TourItinerary`, `TourCancellationPolicy`, `TourGallery`, `tour_tag_mapping`.

### Eventos async
- Ninguno por defecto.

### Final
Tour en estado `CREATED` o `SUBMITTED`, listo para que ADMIN lo apruebe (`PUT /tour/admin/acceptTourById/{id}` → `ACCEPTED`).

---

## Flujo 3 — Creación de schedule + asignación de comisión

### Parte A: Provider crea config

`POST /tour-schedules/config?isTemplate=false` (JWT PROVIDER):

1. **`TourScheduleController.createTourSchedule()`** → `TourScheduleConfigGeneralService.createTourScheduleConfig()`.

2. **Valida**: tour existe y pertenece al provider.

3. **Crea `TourScheduleConfig`**:
   - `label`, `daysOfWeek`, `tour`, `provider`.

4. **Para cada slot del request**:
   - Crea `TourScheduleConfigSlot`:
     - `startTime`, `endTime`, `capacity`.
     - `bookings = 0`, `availability = capacity`.
     - `slotPorcentajeTourya = 0%`.
   - Aplica reglas de min/check availability.
   - Para cada precio del slot:
     - Crea `TourScheduleConfigPrice` con `ageType`, `providerPrice`.
     - `price = TouryaPriceCalculator.calculateSalePrice(providerPrice, 0)` = providerPrice (inicial).

5. **Genera `TourSchedule` instancias** (uno por cada día matching de `daysOfWeek` en un rango).

### Parte B: Backoffice asigna comisión Tourya

`PUT /tour-schedules/tours/{tourId}/percentage` (JWT ADMIN/BACKOFFICE_OPERATION):
- Body: `{ slotPercentageTourya: 15, startDate: "...", endDate: "..." }`.

1. **`TourScheduleConfigGeneralService.updateSlotPercentageByDateRange()`**.

2. **Para cada `TourSchedule` en el rango**:
   - Para cada slot:
     - Crea/actualiza `TourSchedulePriceOverride` por slot/schedule.
     - Recalcula `price = providerPrice × (1 + 15/100)` para todos los ageType.

3. **Respuesta**: `{ tourId, slotsUpdated, pricesRecalculated }`.

### Parte C: Override puntual de UN slot

`PUT /tour-schedules/tours/{tourId}/percentage/{slotId}`:
- Body: `{ slotPercentageTourya: 10 }`.

Crea `TourScheduleSlotOverride` específico → afecta solo ese slot en ese día sin tocar el resto.

### Entidades modificadas
- `TourScheduleConfig`, `TourScheduleConfigSlot`, `TourScheduleConfigPrice`, `TourSchedule`, `TourSchedulePriceOverride`, `TourScheduleSlotOverride`.

### Eventos async
- Ninguno.

### Final
Tour publicable con slots y precios calculados según comisión.

---

## Flujo 4 — Búsqueda y carrito

### Búsqueda

`POST /api/v1/public/tours/schedule/search`:

1. **`PublicController` → `SearchTourScheduleFullService.searchTourSchedule()`**.

2. Internamente llama al **stored procedure** `sp_get_tour_schedule_json` con filtros:
   - `categoryId`, `subCategory`, `durationEnum`, `timeOfDay`.
   - Rango de precios, tags, fechas.
   - `requestedUnits` (para validar capacidad de slot).

3. **Respuesta**: `Page<SearchTourScheduleFullResponse>` con tour + schedules + slots + precios.

### Agregar al carrito

1. **`POST /shopping-cart`** (si no hay activo) — `ShoppingCartService.createShoppingCart()`:
   - Crea `ShoppingCart` con `status = ACTIVE`.

2. **`POST /shopping-cart/items`** — `ShoppingCartService.addMultipleItemsToCart()`:
   - Body:
     ```json
     {
       "productId": 1,
       "productType": "TOUR",
       "tourScheduleId": 5,
       "slotId": 25,
       "scheduleDate": "2026-06-15",
       "details": [
         { "ageType": "ADULT", "quantity": 2 },
         { "ageType": "CHILD", "quantity": 1 }
       ]
     }
     ```
   - Crea `ShoppingCartItem` + `ShoppingCartItemDetail` por cada `ageType`.

### Entidades modificadas
- `ShoppingCart`, `ShoppingCartItem`, `ShoppingCartItemDetail`.

### Eventos async
- Ninguno en este paso. El hold del slot ocurre al checkout, no al agregar al carrito.

### Final
Cliente tiene items en su carrito, listos para checkout.

---

## Flujo 5 — Checkout y pago con Wompi

### Paso 1: Crear hold temporal

`POST /api/v1/reservations` (JWT USER) — body: `CreateTemporalReservationHoldRequest`:

1. **`ReservationService.createTemporalReservationHolds()`**.

2. **Para cada cart item**:
   - Crea `Reservation` con:
     - `status = TEMPORAL` (no pagado aún).
     - `itemId`, `shoppingCartId`, `scheduleId`, `slotId`.
     - `totalAmount` (suma de ageType prices × quantities).
     - `expiresAt = now + 15 min` (configurable: `tourya.reservations.holdMinutes`).
     - `deliveryStatus = PENDING`.

3. **Respuesta**: lista de `reservationIds` para usar en `/payment`.

### Paso 2: Wompi WebView (frontend)

- Frontend abre Wompi widget con la referencia generada por `GET /reference/generate`.
- Usuario paga.
- Wompi retorna `transactionId` + `transactionData` al frontend (NO hay webhook server-side).

### Paso 3: Confirmar pago

`POST /api/v1/payment` (JWT USER) — body con `transactionId`, `reservationIds`, datos del pagador, `paymentType`:

1. **`PaymentService.createPayment()`**.

2. **Validar consistencia**:
   - Suma de `totalAmount` de reservations == monto pagado.
   - Si `paymentType = CREDIT_AND_PLATFORM`: `amountCredit + amountPlatform == total`.

3. **Crear `Payment`**:
   - `transactionId`, `transactionData`, datos del pagador.

4. **Confirmar reservas**:
   - Para cada reservation: cambia `deliveryStatus` PENDING → CONFIRMED.
   - Link a `paymentId`.
   - Calcula `maxCancellationDate`, `maxReschedulingDate` desde política del tour.
   - Genera QR (`qrUrl`) y sube a GCS/S3.

5. **Procesar créditos (si aplica)**:
   - `validateAndConsumeReservedCredits()`.
   - Para cada `Credit`: reduce `reservedAmount`, crea `PaymentCredit`, marca `CONSUMED` si se acabó.

6. **Actualizar carrito**:
   - `ShoppingCartItem.status = PAID`.
   - Link a `reservationId`.

### Entidades modificadas
- `Reservation` (TEMPORAL → CONFIRMED, con QR, payment link).
- `Payment` (creado).
- `PaymentCredit` (si usó créditos).
- `Credit` (`reservedAmount` consumido).
- `ShoppingCartItem` (`status = PAID`).

### Eventos async
- Generación de QR.
- Email de confirmación de reserva (vía `@Async`).

### Final
Reservas confirmadas, QR generado, listo para que el turista asista.

---

## Flujo 6 — Cancelación de reserva

### Por turista

`PUT /api/v1/reservations/{id}/cancel` (JWT USER):
- Body: `CancelReservationRequest` con `cancellationReason`.

1. **`ReservationService.cancelReservation()`**.

2. **Valida**:
   - Reserva existe.
   - `deliveryStatus = CONFIRMED` (no cancelada ni entregada).
   - `canCancel = true`.
   - `now <= maxCancellationDate`.

3. **Actualiza reserva**:
   - `deliveryStatus = CANCELED`.
   - `cancellationDate = now`.
   - `cancellationReason` set.

4. **Calcula refund** desde `TourCancellationPolicy`:
   - Busca política por `daysBeforeSchedule`.
   - `refundAmount = totalAmount × (refundPercentage / 100)`.

5. **Crea Credit**:
   - `userId = turista`.
   - `amount = refundAmount`.
   - `status = CREATED`.
   - `expirationDate = now + 1 año`.

6. **Libera capacidad del slot**:
   - `tourScheduleSlotAvailabilityService.recalculate(slotId)`.

### Por lluvia (solo ADMIN)

`PUT /api/v1/reservations/{id}/cancel/rain`:
- Requiere `MaritimActivityReport` activo en la fecha + ubicación.
- Refund 100% como crédito.

### Job nocturno

`ReservationCancellationFlagsJob` (cron `0 5 0 * * *` — 5 AM Bogotá):
- Reservas con `maxCancellationDate < today` → `canCancel = false`.
- Reservas con `maxReschedulingDate < today` → `canReschedule = false`.

### Entidades modificadas
- `Reservation` (CANCELED).
- `Credit` (creado).
- `TourScheduleConfigSlot` (availability recalculada).

### Eventos async
- Email de notificación de cancelación.

### Final
Reserva CANCELED, crédito disponible para futuras compras.

---

## Flujo 7 — Payout al proveedor

### Job programado

`ProviderPayoutOrderJob` (cron `0 0 7 ? * MON,THU` — Mon/Thu 7 AM Bogotá):

1. **Calcula `payDate`** = mañana (Mar o Vie).

2. **Query a `AccountPayable`** con:
   - `status = PENDING`.
   - `paymentAvailableDate <= payDate` (reserva entregada + 2 días).
   - Rango de fechas de ejecución (Jue-Dom o Lun-Mie).
   - Sin asociación previa a payout order.

3. **Para cada provider con `AccountPayable`s pendientes**:
   - Crea `ProviderPayoutOrder`:
     - `providerId`, `payDate`, `status = PENDING`.
     - `amountTotal` = suma.
   - Para cada `AccountPayable`: crea `ProviderPayoutOrderReservation`.

4. **Cálculo del monto al proveedor**:
   - `accountPayable.amount = providerPrice × quantity`.
   - Tourya retiene `(price - providerPrice) × quantity`.

### Backoffice marca como pagado

`POST /provider/payout-orders/admin/{orderId}/proof` (multipart, file):

1. **`ProviderPayoutOrderService.uploadAttachmentAndMarkPaid()`**.

2. **Sube archivo** a GCS/S3 vía `IStorageService`.

3. **Crea `ProviderPayoutAttachment`** con URL.

4. **Actualiza `ProviderPayoutOrder.status = PAID`**.

### Entidades modificadas
- `ProviderPayoutOrder`, `ProviderPayoutOrderReservation`, `ProviderPayoutAttachment`.

### Eventos async
- ❓ ASUNCIÓN: email al proveedor avisando del pago.

### Final
Provider recibe transferencia bancaria (manual) y comprobante en su panel.

---

## Flujo 8 — KYB / Onboarding del proveedor

### Paso 1: Crear solicitud

`POST /api/v1/requestProvider/user/save` (JWT USER) — body: datos legales + bancarios.

1. **`RequestProviderService.save()`**.
2. Crea `RequestProvider` con `status = DRAFT`.

### Paso 2: Subir documentos

`POST /api/v1/requestProvider/{requestId}/gallery` (multipart):
- Sube archivos (RUT, RNT, etc.) a GCS/S3.
- Crea `RequestProviderGallery` por cada documento, con `documentTypeId`.

### Paso 3: Enviar a revisión

`PUT /api/v1/requestProvider/user/send`:
- `RequestProvider.status = SUBMITTED`.
- Envía email a backoffice.

### Paso 4: Revisión por ADMIN

- `PUT /admin/pre-approve/{id}` → `PRE_APPROVED` (visible al aplicante para ajustes).
- `PUT /admin/incomplete/{id}` → `INCOMPLETE` (con `incompleteReason`, vuelve a `DRAFT`-able).
- `PUT /admin/cancel/{id}` → `CANCELED` (con `declinedReason`).

### Paso 5: Aprobación final

`PUT /api/v1/requestProvider/admin/approve/{id}`:

1. **`RequestProviderService.approveRequestProviderById()`**.

2. **Crea `Provider`**:
   - `status = ACTIVE`.
   - Datos de la solicitud.

3. **Crea `ProviderUser`**:
   - `user_id = aplicante`.
   - `provider_id = nuevo`.
   - `isPrimary = true`.

4. **Asigna rol PROVIDER al `User`**.

5. **Marca `RequestProvider.status = APPROVED`**.

6. **Email de bienvenida**.

### Entidades modificadas
- `RequestProvider`, `RequestProviderGallery`, `Provider`, `ProviderUser`, `User` (+rol), `user_roles`.

### Eventos async
- Email de bienvenida.

### Final
Usuario es PROVIDER. Puede crear tours, schedules, recibir payouts.

---

## Resumen de jobs scheduled

| Job | Cron | Función |
|-----|------|---------|
| `TemporalReservationExpiryJob` | cada 60s (`fixedDelay`) | Libera reservations con `TEMPORAL` expiradas |
| `ReservationCancellationFlagsJob` | `0 5 0 * * *` (5 AM Bogotá) | Desactiva flags `canCancel/canReschedule` vencidos |
| `ProviderPayoutOrderJob` | `0 0 7 ? * MON,THU` | Crea payout orders para proveedores |

---

## Resumen de eventos async (vía `@Async`)

Tourya NO usa un broker de eventos (Kafka, RabbitMQ). Los "eventos" son métodos `@Async` que el servicio invoca directamente.

| Evento | Disparado por | Acción |
|--------|---------------|--------|
| `sendEmail(activation)` | `register()` | Envía código de activación |
| `sendEmail(welcome provider)` | `approveRequestProviderById()` | Email al nuevo provider |
| `sendEmail(payment confirmation)` | `createPayment()` | Email con detalle de reserva |
| `sendEmail(cancellation)` | `cancelReservation()` | Notifica cancelación |
| `sendEmail(reset password)` | `resetTemporaryPassword()` | Notifica nueva clave temporal |
| Generación de QR | `confirmTemporalReservations()` | Sube QR a GCS/S3 |

📌 PENDIENTE LUIS — ¿hay más emails / notificaciones que el cliente quiere y aún no están?
