# 05 — Reglas de negocio

Catálogo de las reglas que rigen el comportamiento de Tourya. Marcadas según origen:
- ✅ **deducido del código** (regla implementada y verificable).
- ⚙️ **parametrizable** (configurable por properties / DB).
- 📌 **pendiente Luis** (requiere validación).

---

## 1. Cuentas y autenticación

### RN-001 — Email único
✅ Un email solo puede tener una cuenta. El check es case-insensitive (se guarda en minúsculas).

> **Decisión**: si un usuario se registra con `Juan@Gmail.COM`, queda guardado como `juan@gmail.com`.

---

### RN-002 — Contraseña mínima 8 caracteres
✅ Validación `@Size(min=8)` en `RegistrationRequest` y `AuthenticationRequest`.

📌 PENDIENTE LUIS — ¿hay requisitos adicionales que no están implementados? (mayúsculas, números, especiales). hacer los ajustes utilizando las buenas practicas para la creacion de constraseñas.

---

### RN-003 — Activación de cuenta por email
✅ Al registrarse:
1. Se crea el usuario con `enabled = false`.
2. Se genera un token de 6 dígitos numéricos.
3. Se envía email con la URL: `{ACTIVATION_URL}{token}`.
4. El token expira en **15 minutos**.

✅ Al intentar activar:
- Si el token es válido: `enabled = true`, `validatedAt = now()`.
- Si el token expiró: se genera un nuevo token + se reenvía email + se lanza error.

⚠️ Sin protección contra **replay**: un token validado podría reutilizarse (es un bug en `AuthenticationService.activateAccount`). se debe resolver este bug y dejarlo de manera que un token valido no pueda ser reutilizado. 

---

### RN-004 — Hash de contraseña con BCrypt
✅ Algoritmo: `BCryptPasswordEncoder` (cost factor por defecto = 10).

---

### RN-005 — JWT de 24 horas
✅ Expiración: `application.security.jwt.expiration=86400000` ms = 24 horas.

⚠️ Sin refresh token. El usuario debe volver a loguearse cada 24h. Definir una buena practica en este punto. deberiamos tener tanto access tolen como Refresh token. para el cliente el access token podria ser de 15 a 30 min y el refresh token podria ser de 14 a 30 dias sin expiracion de inactividad. para el proveedor el access token deberia ser de maximo 15 min, el refresh token de 1 a 7 dias y la expiracion por inactividad de 1 a 2 horas. para el backoffice el access token es de maximo 10 min, el refresh token es de 8 a 12 horas y la expiracion por inactivida de 15 minutos.

---

### RN-006 — Login social NO valida token
⚠️ El backend confía ciegamente en el `uuidSocial` que envía el frontend. **Cualquiera puede suplantar a un usuario conociendo solo su email**. esto debe solucionarse con la solución propuesta.e

Solución propuesta: ver `social-login-google-facebook.md` (Token Exchange).

---

### RN-007 — Sub-usuario con contraseña temporal
✅ El PROVIDER crea sub-usuarios (`PROVIDER_OPERATOR`) definiendo una contraseña temporal:
1. Sub-usuario se crea con `mustChangePassword = true`, `enabled = true`.
2. En el primer login, la respuesta indica `mustChangePassword: true`.
3. El frontend debe forzar cambio de contraseña antes de continuar.
4. El sub-usuario usa `PATCH /users` con `currentPassword + newPassword + confirmationPassword`.

✅ El PROVIDER puede resetear la clave del sub-usuario: `PUT /provider/users/{id}/reset-password` con nueva contraseña temporal.

---

### RN-008 — Roles asignables y no-asignables
✅ Roles asignables por API:
- `USER` — auto, al registrarse.
- `PROVIDER` — automático al aprobar `RequestProvider`.
- `PROVIDER_OPERATOR` — el PROVIDER lo asigna al crear sub-usuario.
- `BACKOFFICE_OPERATION`— el ADMIN lo asigna al crear sub-usuario.

⚠️ Roles NO asignables por API (manual en BD):
- `ADMIN`

---

## 2. Tours y catálogo

### RN-009 — Tour empieza en estado CREATED
✅ Al crearse, todo tour está en `status = CREATED`. El PROVIDER debe enviarlo explícitamente a aprobación (`PUT /tour/user/submitTourById/{id}` → `SUBMITTED`).

### RN-010 — Solo ADMIN aprueba/rechaza tours
✅ Endpoints `admin/acceptTourById`, `admin/returnedTourById`, `admin/cancelTourById` requieren rol ADMIN.

### RN-011 — Todos los textos del tour requieren español
✅ La validación `@NotBlank` en `TranslatedField.es` se aplica al request de creación / update. Inglés y portugués son opcionales.

### RN-012 — Tour debe solo un Provider asignado
✅ El backend resuelve el provider del JWT del PROVIDER que crea.

### RN-013 — Galería del tour ≤ 1MB por imagen (asunción)
❓ ASUNCIÓN — verificar. El profile photo tiene límite 1MB; las imágenes del tour ❓. 

📌 PENDIENTE LUIS — ¿cuántas imágenes máximo? 7 imagenes ¿tamaño máximo por imagen? 5MB. usar imagenes de 1920 pixeles de ancho, siempre en formato horizontal (landscape) y nunca verticales (Portrait).

---

## 3. Horarios y precios

### RN-014 — Cálculo de precio de venta
✅ Fórmula:
```
price = providerPrice × (1 + slotPercentageTourya / 100)
```
Donde:
- `providerPrice` lo define el PROVIDER.
- `slotPercentageTourya` (en puntos, ej. `15` = 15%) lo define BACKOFFICE/ADMIN.

### RN-015 — Slot inicia con comisión 0
✅ Al crear un slot nuevo, `slotPercentageTourya = 0`. Hasta que BACKOFFICE asigne %, el `price = providerPrice` (Tourya no gana nada).

### RN-016 — Asignación de comisión por rango de fechas
✅ `PUT /tour-schedules/tours/{tourId}/percentage` con `slotPercentageTourya + startDate + endDate` actualiza todos los slots de los `TourSchedule`s en ese rango.

### RN-017 — Override de slot puntual
✅ `PUT /tour-schedules/tours/{tourId}/percentage/{slotId}` permite ajustar el % de UN slot específico en UN día específico, sin afectar el resto.

### RN-018 — Override de precio puntual
✅ `TourSchedulePriceOverride` permite ajustar el precio de un slot en una fecha (ej. surge pricing, promoción).

### RN-019 — El PROVIDER no ve la comisión Tourya
✅ Los responses del PROVIDER omiten `slotPercentageTourya`. Solo ven `providerPrice` y `price`. BACKOFFICE/ADMIN sí lo ven.

### RN-020 — Precios por tipo de persona obligatorios
✅ Cada slot debe tener un precio por cada `ageType` configurado (ADULT, CHILD, INFANT). El INFANT puede tener `providerPrice = 0`.

### RN-021 — Capacidad ilimitada
✅ Un schedule puede tener `isUnlimitedCapacity = true`, lo que desactiva el control de capacidad por slot. ❓ — ASUNCIÓN: probablemente para tours sin restricción física.

📌 PENDIENTE LUIS — ¿cuándo aplica `isUnlimitedCapacity`? ¿Solo en algunos tipos de tour? Esto se cambio, ya que la capacidad ilimitada es del tour y no del slot ni del schedule. por tanto, cuando un tour tiene capacidad ilimitada al momento de configurar el slot solo se debe ingresar el precio mas no la capacidad. por tanto los campos isUnlimitedCapacity se puede borrar de la tabla schedule.

---

## 4. Carrito y checkout

### RN-022 — Hold temporal de 15 minutos
✅ Al hacer checkout (POST `/reservations`), se crean `Reservation`s en estado `TEMPORAL` con `expiresAt = now + 15 minutos`. Si el usuario no paga en ese tiempo, el job `TemporalReservationExpiryJob` (corre cada 60s) las cancela y libera el slot.

⚙️ Configurable: `tourya.reservations.holdMinutes` (default 15). el usuario ADMIN debe poder configurar este campo.

### RN-023 — Validación de capacidad en checkout
✅ Al agregar al carrito, se valida que el slot tenga capacidad suficiente (`requestedUnits <= availability`). esto solo se debe realizar si el tour el campo `isUnlimitedCapacity= false`

### RN-024 — Validación de monto total en checkout
✅ Si el pago incluye créditos: `amountCredit + amountPlatform == totalAmount` debe cumplirse, sino error.

### RN-025 — Una sola transacción Wompi por payment
✅ Cada `Payment` tiene un único `transactionId` de Wompi. Si Wompi retorna fallida la transacción, no se crea Payment ni se confirman reservas.

⚠️ NO hay webhook server-side de Wompi: la confirmación es client-side. Si el cliente cierra la app entre Wompi success y POST `/payment`, queda en limbo. esto se debe resolver.

---

## 5. Reservas

### RN-026 — Estados de la reserva
✅ `Reservation.deliveryStatus`:
```
TEMPORAL → CONFIRMED → DELIVERED
                    ↘ CANCELED
                    ↘ CANCELED_RAIN (por lluvia, solo ADMIN)
```

### RN-027 — Generación de QR al confirmar
✅ Al confirmar la reserva, se genera un QR con URL única (`qrUrl`). Se sube a GCS/S3.

### RN-028 — Formato de booking ID
✅ Los endpoints públicos aceptan dos formatos:
- Numérico: `250`
- Con prefijo: `TB-250`

Útil para que el turista pegue el código tal como aparece en su email/recibo.

### RN-029 — Consumo de reserva por QR
✅ El operador escanea el QR (mobile `QrScannerPage`) o el endpoint `POST /reservations/{id}/consume` la marca como `DELIVERED`. Esto:
1. Marca el `ShoppingCartItem` como `COMPLETED`.
2. Crea un `AccountPayable` para el provider.
3. Habilita el payout en `reservationDate + 2 días`.

### RN-030 — Cancelación por turista según política
✅ El turista puede cancelar si:
- `canCancel = true` (flag expirable por job).
- `cancellationDate <= maxCancellationDate`.

Razones (`CancellationReasonEnum`):
- `CANNOT_ATTEND`
- `ILLNESS`
- `INABILITY_TO_TRAVEL`
- `Legal obligations`
- `Change of plans`

### RN-031 — Refund por cancelación → Crédito
✅ El refund NO es devolución directa a la tarjeta. Se genera un `Credit` a favor del turista, según el % refundable de la política del tour:
- Ejemplo: tour con política "100% refund si cancela 7+ días antes".
- Si cancela 8 días antes: crédito por 100%.
- Si cancela 3 días antes: ❓ — depende de la política del operador.

📌 PENDIENTE LUIS — confirmar las ventanas de cancelación y % de refund estándar. el operador al crear el tour especifica la politica de cancelacion. la politica de cancelacion puede ser: Flexible (hasta 24 hotas antes), Estandar (hasta 48 horas antes), Moderado (hasta 4 dias antes), Estricto (Hasta 7 dias antes).  si se cumple con la política de cancelación se hará la devolución del 100% del dinero en el credito.

### RN-032 — Cancelación por lluvia
✅ Solo ADMIN: `PUT /reservations/{id}/cancel/rain`. Requiere DIMAR flag (un `MaritimActivityReport` activo en la fecha + ubicación). Crédito 100%.

### RN-033 — Reschedule de reserva
✅ El turista puede reagendar si:
- `canReschedule = true`.
- La política del tour `allowsRescheduling = true`.
- Antes de `maxReschedulingDate`.

✅ 3 casos según diferencia de precio:
- Precio igual: cambio directo.
- Precio menor: se genera crédito por la diferencia.
- Precio mayor: el turista debe pagar la diferencia.

### RN-034 — Job de expiración de flags de cancelación/reschedule
✅ `ReservationCancellationFlagsJob` corre diariamente a las **5:00 AM Bogotá**. Desactiva `canCancel = false` / `canReschedule = false` cuando `maxCancellationDate / maxReschedulingDate < today`.

---

## 6. Créditos

### RN-035 — Origen de créditos
✅ Un crédito se genera por:
- Cancelación de reserva (por turista o por lluvia).
- Reschedule a tour más barato.
- Transferencia desde otro turista.

### RN-036 — Expiración de créditos
✅ `expirationDate = creationDate + 1 año`. Después de expirar, no se pueden usar. enviar un correo al expirar un crédito.

### RN-037 — Reserva parcial de crédito
✅ En checkout, el turista puede pre-reservar parte del crédito (`POST /credits/reserve`). El monto queda en `reservedAmount` hasta que se confirme el pago o expire el hold.

### RN-038 — Transferencia de crédito (una vez)
✅ Un crédito se puede transferir a otro turista UNA vez (`POST /credits/{creditId}/transfer`). Identificación por documento del destinatario.

⚠️ Una vez transferido, no se puede revertir.

### RN-039 — Consumo de créditos en pago híbrido
✅ El turista puede pagar parte con crédito y parte con tarjeta (`paymentType = CREDIT_AND_PLATFORM`). Validación: la suma debe igualar el total.

---

## 7. Payouts a proveedores

### RN-040 — Buffer de 2 días para payout
✅ Una reserva entra al payout solo si han pasado **2 días** desde su `reservationDate` (`payoutAvailableDate = reservationDate + 2 días`). Esto da tiempo a reclamos/disputas.

### RN-041 — Cronograma de payouts
✅ `ProviderPayoutOrderJob` corre **lunes y jueves a las 7:00 AM (Bogotá)**:
- **Lunes**: agrupa reservas ejecutadas Jueves-Viernes-Sábado-Domingo → paga el martes.
- **Jueves**: agrupa reservas ejecutadas Lunes-Martes-Miércoles → paga el viernes.

### RN-042 — Cálculo del monto del payout
✅ Tourya paga al operador solo el `providerPrice`, no el `price` de venta:
```
amount al operador = providerPrice × quantity (sumado en todas las reservas del payout)
amount retenido por Tourya = (price - providerPrice) × quantity
```

### RN-043 — Comprobante manual para marcar pagado
✅ Tourya hace la transferencia bancaria manualmente (sin integración). El BACKOFFICE/ADMIN sube el comprobante (`POST /provider/payout-orders/admin/{orderId}/proof`) y marca como `PAID`. En el roadmap se tine planeado integrar tourya con las pasarelas de pago para hacer los pagos de forma automatica.

---

## 8. KYB / Onboarding

### RN-044 — Ciclo del RequestProvider
✅ Estados:
```
DRAFT → SUBMITTED → PRE_APPROVED → APPROVED
                 ↘ INCOMPLETE (loop)
                 ↘ CANCELED
```

### RN-045 — Documentos obligatorios
✅ Los `RequestProviderDocumentType` con `mandatory = true` deben adjuntarse antes de SUBMITTED.
Los documentos son obligatorios (RUT, RNT, certificación bancaria, cédula representante legal, camara de comercio, Seguros etc.).

### RN-046 — Aprobación crea Provider + asigna rol
✅ Al aprobar `PUT /requestProvider/admin/approve/{id}`:
1. Se crea entidad `Provider` con `status = ACTIVE`.
2. Se crea `ProviderUser` linkando User a Provider (con `isPrimary = true`).
3. Se agrega rol `PROVIDER` al `User.roles`.
4. Se envía email de bienvenida.

---

## 9. Reseñas

### RN-047 — Solo turistas que asistieron pueden reseñar
✅ Para reseñar, el turista debe tener una `Reservation` con `deliveryStatus = DELIVERED`.

### RN-048 — Una reseña por reserva
✅ ❓ ASUNCIÓN — verificar. El código sugiere que es 1 review por `(userId + tourId + reservationId)`.

### RN-049 — Hasta 5 fotos por reseña
✅ Endpoint multipart: hasta 5 archivos en `images[]`.

### RN-050 — Reseñas se publican directamente
✅ Desde la migración **040**, las nuevas reseñas se crean con `status = PUBLISHED` (sin moderación previa). Solo ADMIN puede ver reseñas en cualquier estado.

📌 PENDIENTE LUIS — ¿por qué se quitó la moderación? ¿hay plan de reintroducirla? en el roadmap las reseñas al crearse tendrán un estado de MODERACION. se construirá un agente IA que revise la reseña para analizar el texto, buscar spam, lenguaje ofensivo, enlaces sospechosos o patrones de fraude. si la reseña pasa la revisión del agente la reseña pasa a estado PUBLISHED. si no pasa a estado CANCELADA.

### RN-051 — Razones de reseña (1-7)
✅ Catálogo predefinido de motivos (`review_reason` enum) — 7 opciones (positivas para rating 4-5, negativas para 1-3).

---

## 10. i18n

### RN-052 — Español obligatorio en todo texto del tour
✅ `TranslatedField.es` es `@NotBlank`. Inglés y portugués opcionales.

### RN-053 — Fallback automático al español
✅ Si se pide un idioma vacío, devuelve el español: `field.get("en")` → si `en=""`, devuelve `es`.

📌 Propuesta para auto-traducir es → en, pt al crear/editar tour: ver `traduccion-automatica-tours.md`.

---

## 11. DIMAR / Maritime Reports

### RN-054 — Reporte de actividad marítima por backoffice
✅ El BACKOFFICE/ADMIN registra reportes (`POST /maritime-activity-reports`) con bandera (flag) — verde / amarilla / roja. Estos reportes sirven como soporte para cancelaciones por mal tiempo. en el roadmap se revisará como integrar el reporte de DIMAR a Tourya. 

---

## Cosas que no son reglas, pero son inferencias importantes

### Reservas pueden compartir Payment
✅ Un `Payment` puede tener múltiples `Reservation`s (un solo pago para varios tours del carrito). 

### Tourya cobra UVA a Wompi
❓ ASUNCIÓN — Wompi cobra comisión por transacción (~2.99% + IVA). Esta comisión sale del `slotPercentageTourya`. No está documentado en código. inicialmente la comisión de las plataforma será absorbida por TOURYA (`slotPercentageTourya`). se revisará mas adelante si el costo lo asuma el proveedor.

### Sin sistema de promociones / cupones
✅ No hay módulo de cupones / códigos de descuento. Los overrides de precio sirven, pero no hay UI ni concepto de "cupón". en el roadmap esta previsto los cupones y codigos de descuento.

### Sin sistema de notificaciones push
✅ No hay integración con Firebase Cloud Messaging u otras. Las notificaciones son solo email. 

📌 PENDIENTE LUIS — ¿está planeado para mobile?
