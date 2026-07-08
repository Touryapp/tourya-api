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

📌 **A ajustar**: aplicar **buenas prácticas** para creación de contraseñas — al menos 8 caracteres con combinación de mayúsculas, minúsculas, números y caracteres especiales. Feedback en tiempo real al usuario en el formulario.

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

✅ **Protección anti-replay implementada (2026-07-08, PR #147, SEC-04)**: `AuthenticationService.activateAccount()` verifica `validatedAt != null` al inicio y rechaza tokens ya usados con "Invalid token" (mismo mensaje que "no encontrado", para no filtrar info al atacante). Método anotado con `@Transactional` para atomicidad entre `user.enabled=true` y `token.validatedAt`.

---

### RN-004 — Hash de contraseña con BCrypt
✅ Algoritmo: `BCryptPasswordEncoder` (cost factor por defecto = 10).

---

### RN-005 — Access + Refresh tokens diferenciados por rol (a implementar)
✅ Expiración actual: `application.security.jwt.expiration=86400000` ms = 24 horas. **Sin refresh token**.

✅ **Decisión Franklin (2026-07-07)** — basada en OWASP ASVS Level 2 (V3.5) y benchmarks de industria (Airbnb, Booking, Uber para turista; patrones bancarios para backoffice):

#### Tiempos por rol

| Rol | Access token | Refresh token | Idle (inactividad) |
|-----|--------------|---------------|--------------------|
| **USER (turista)** | 60 min | 30 días | Sin idle |
| **PROVIDER / PROVIDER_OPERATOR** | 30 min | 7 días | 4 horas |
| **ADMIN / BACKOFFICE_OPERATION** | 15 min | 8 horas | 15 minutos |

**Racional**:
- **Turista 60 min**: 15 min se caducaría en medio del checkout Wompi (mala UX). 60 min cubre el flujo compra + navegación sin fricción.
- **Proveedor 30 min + 4h idle**: operador en campo pasa horas sin abrir la app (tour largo). Menos de 30 min mata la productividad al escanear QRs.
- **Backoffice 15 min + 15 min idle**: maneja dinero (aprueba KYB, ajusta comisiones, sube payouts). Ventana corta reduce riesgo si el token se compromete.

#### Reglas transversales (patrón industry-standard contra robo de tokens)

1. **Refresh token rotativo**: cada uso emite un token nuevo y **invalida el anterior**. El token viejo NO se puede reutilizar.
2. **Detección de reuso**: si un `jti` ya rotado se intenta usar → **revocar toda la familia** de tokens del usuario. Esto detecta cuando un atacante obtuvo el refresh y el usuario legítimo lo rotó primero.
3. **Almacenamiento**:
   - **Web (Angular)**: cookie `HttpOnly` + `Secure` + `SameSite=Lax`.
   - **Mobile (MAUI)**: `SecureStorage` (ya se usa hoy para el JWT).
   - Access token en `Authorization: Bearer {token}` (igual que hoy).
4. **Logout**: revoca **toda la familia** de tokens del usuario, no solo la sesión actual.

#### Nueva tabla `refresh_token`

| Campo | Tipo | Notas |
|-------|------|-------|
| `jti` | UUID | PK — id único del refresh token |
| `user_id` | int | FK a `_user` |
| `family_id` | UUID | ID de familia (misma sesión → misma familia; rotación mantiene familia) |
| `previous_jti` | UUID | `jti` del token que este reemplaza (null para el primer emitido) |
| `issued_at` | timestamp | |
| `expires_at` | timestamp | Según rol (30d / 7d / 8h) |
| `revoked_at` | timestamp | null si aún válido |
| `revoked_reason` | string | LOGOUT / ROTATED / REUSE_DETECTED / EXPIRED |
| `user_agent`, `ip_address` | string | Para forensia |

Índice compuesto: `(user_id, family_id, revoked_at)` para queries de "familia activa del usuario".

---

### RN-006 — Login social NO valida token
⚠️ El backend confía ciegamente en el `uuidSocial` que envía el frontend. **Cualquiera puede suplantar a un usuario conociendo solo su email**. ✅ **Decisión**: implementar la solución propuesta en `social-login-google-facebook.md` (Token Exchange).

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
- `BACKOFFICE_OPERATION` — el ADMIN lo asigna al crear un usuario de backoffice.

⚠️ Roles NO asignables por API (asignación manual en BD):
- `ADMIN` — solo puede existir **un único ADMIN** en toda la plataforma.

---

## 2. Tours y catálogo

### RN-009 — Tour empieza en estado CREATED
✅ Al crearse, todo tour está en `status = CREATED`. El PROVIDER debe enviarlo explícitamente a aprobación (`PUT /tour/user/submitTourById/{id}` → `SUBMITTED`).

### RN-010 — Solo ADMIN aprueba/rechaza tours
✅ Endpoints `admin/acceptTourById`, `admin/returnedTourById`, `admin/cancelTourById` requieren rol ADMIN.

### RN-011 — Todos los textos del tour requieren español
✅ La validación `@NotBlank` en `TranslatedField.es` se aplica al request de creación / update. Inglés y portugués son opcionales.

### RN-012 — Tour debe tener un único Provider asignado
✅ El backend resuelve el provider del JWT del PROVIDER que crea.

### RN-013 — Restricciones de galería del tour (propuesta a validar)
📌 **Propuesta** (Luis, 2026-07-07 confirma que son valores **propuestos**, a validar contra el template Angular actual):
- **Máximo 7 imágenes** por tour.
- **Máximo 5 MB** por imagen.
- Formato **horizontal (landscape)** obligatorio — nunca verticales (portrait).
- Ancho recomendado: **1920 px**.

📌 **Pendiente**: revisar el **template Angular actual** (tarjetas de tour, hero, detalle) para determinar el tamaño ideal real y ajustar estos valores. Luego implementar en frontend (validación pre-upload) **y** backend (validación al recibir).

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

### RN-015 — Comisión Tourya por default a nivel de Tour (aprobado — a implementar)
Actualmente al crear un slot nuevo, `slotPercentageTourya = 0` — hasta que el BACKOFFICE lo asigne, Tourya no gana nada por ese slot.

✅ **Aprobado por Luis (2026-07-07)** — el motivo original está confirmado: *"evitar que el proveedor configurara el schedule (Slot) y Tourya no ganara nada por no tener configurado el % en el Slot."*

📌 **A implementar**:
1. Crear un nuevo campo **`percentageTourya`** en la tabla `Tour`.
2. El **ADMIN diligencia** este campo al momento de **aprobar un tour**.
3. Cuando un PROVIDER configure un slot nuevo, se debe usar por default el `Tour.percentageTourya` para calcular el `price` del slot.
4. El BACKOFFICE puede cambiar el `slotPercentageTourya` de un slot específico usando los endpoints de override existentes.

Esto elimina la ventana de "slot con 0% de comisión".

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

### RN-021 — Capacidad ilimitada (limpieza de campo redundante)
✅ **Aclaración de Luis (2026-07-07)**: la capacidad ilimitada es una propiedad **del tour** — ya existe en `Tour.isUnlimitedCapacity`. El campo duplicado en `TourSchedule` es un remanente que debe **eliminarse**.

- ✅ El campo `Tour.isUnlimitedCapacity` **ya existe** y es la fuente de verdad.
- 📌 **A eliminar**: el campo `isUnlimitedCapacity` en la tabla `tour_schedule` (redundante).
- Cuando un tour tiene capacidad ilimitada, al configurar el slot **solo se ingresa el precio** — no la capacidad.
- La validación de capacidad en checkout (RN-023) se salta cuando `Tour.isUnlimitedCapacity = true`.

---

## 4. Carrito y checkout

### RN-022 — Hold temporal de 15 minutos (configurable — a implementar)
✅ Al hacer checkout (POST `/reservations`), se crean `Reservation`s en estado `TEMPORAL` con `expiresAt = now + 15 minutos`. Si el usuario no paga en ese tiempo, el job `TemporalReservationExpiryJob` (corre cada 60s) las cancela y libera el slot.

✅ **Aprobado por Luis (2026-07-07)**: hoy son 15 minutos (tiempo que tiene el turista para pagar en la pasarela). Debe ser **configurable por el ADMIN** desde el backoffice para poder ajustarlo si Wompi tarda o si se detectan patrones de abandono a los X minutos.

📌 **A implementar**:
1. Persistir el valor en `app_config` (JSONB) en lugar del property `tourya.reservations.holdMinutes`.
2. UI en backoffice para editar el valor.
3. Aplicar sin reiniciar el servicio (leer de `app_config` en cada checkout).

### RN-023 — Validación de capacidad en checkout
✅ Al agregar al carrito, se valida que el slot tenga capacidad suficiente (`requestedUnits <= availability`). **Esta validación solo aplica si `Tour.isUnlimitedCapacity = false`**.

### RN-024 — Validación de monto total en checkout
✅ Si el pago incluye créditos: `amountCredit + amountPlatform == totalAmount` debe cumplirse, sino error.

### RN-025 — Una sola transacción Wompi por payment
✅ Cada `Payment` tiene un único `transactionId` de Wompi. Si Wompi retorna fallida la transacción, no se crea Payment ni se confirman reservas.

⚠️ NO hay webhook server-side de Wompi: la confirmación es client-side. Si el cliente cierra la app entre Wompi success y POST `/payment`, queda en limbo.

✅ **Aprobado por Luis (2026-07-07)**: **avanzar con el desarrollo del webhook Wompi** para evitar problemas con los pagos de los turistas. Prioridad alta.

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
- `LEGAL_OBLIGATIONS` (nuevo) — ✅ aprobado Luis 2026-07-07
- `CHANGE_OF_PLANS` (nuevo) — ✅ aprobado Luis 2026-07-07

> Contexto: se identificó que faltaban motivos por los cuales un turista puede razonablemente cancelar un tour. Estas razones se agregaron para cubrir esos casos.

### RN-031 — Refund por cancelación → Crédito
✅ El refund NO es devolución directa a la tarjeta. Se genera un `Credit` a favor del turista.

**Políticas de cancelación disponibles** — el operador elige una al crear el tour:

| Política | Ventana permitida | Refund |
|----------|-------------------|--------|
| **Flexible** | Hasta 24 horas antes | 100% en crédito |
| **Estándar** | Hasta 48 horas antes | 100% en crédito |
| **Moderado** | Hasta 4 días antes | 100% en crédito |
| **Estricto** | Hasta 7 días antes | 100% en crédito |

Si se cumple con la ventana de la política, el refund es del **100%** en crédito. Fuera de la ventana, no hay refund.

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
✅ `expirationDate = creationDate + 1 año` (a volver configurable). Después de expirar, no se pueden usar.

📌 **A implementar**: enviar **correo automático** al turista cuando un crédito esté por expirar (ej. 30 y 7 días antes) y otro correo al expirar.

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
✅ `ProviderPayoutOrderJob` corre **lunes y jueves a las 7:00 AM (Bogotá)** para generar las órdenes de pago:
- **Lunes**: agrupa reservas ejecutadas Jueves–Viernes–Sábado–Domingo → se paga el **martes**.
- **Jueves**: agrupa reservas ejecutadas Lunes–Martes–Miércoles → se paga el **viernes**.

### RN-042 — Cálculo del monto del payout
✅ Tourya paga al operador solo el `providerPrice`, no el `price` de venta:
```
amount al operador = providerPrice × quantity (sumado en todas las reservas del payout)
amount retenido por Tourya = (price - providerPrice) × quantity
```

### RN-043 — Comprobante manual para marcar pagado
✅ Tourya hace la transferencia bancaria manualmente (sin integración). El BACKOFFICE/ADMIN sube el comprobante (`POST /provider/payout-orders/admin/{orderId}/proof`) y marca como `PAID`.

📌 **Roadmap**: integrar Tourya con las APIs de las **pasarelas de pago (Wompi / Mercado Pago)** para automatizar los payouts con total trazabilidad. Evaluar esquema de agente con reglas de aprobación por monto.

---

## 8. KYB / Onboarding

### RN-044 — Ciclo del RequestProvider
✅ Estados:
```
DRAFT → SUBMITTED → PRE_APPROVED → APPROVED
                 ↘ INCOMPLETE (loop)
                 ↘ CANCELED
```

### RN-045 — Documentos obligatorios para KYB
✅ Los `RequestProviderDocumentType` con `mandatory = true` deben adjuntarse antes de `SUBMITTED`.

**Documentos obligatorios**:
- RUT
- RNT (Registro Nacional de Turismo, vigente)
- Certificación bancaria
- Cédula del representante legal
- Cámara de Comercio
- Pólizas de seguros vigentes

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

### RN-050 — Reseñas se publican directamente (hoy) — moderación IA en roadmap
✅ Hoy: desde la migración **040**, las nuevas reseñas se crean con `status = PUBLISHED` (sin moderación previa). Solo ADMIN puede ver reseñas en cualquier estado.

📌 **Roadmap**: reintroducir moderación asistida por IA:
- Las reseñas nuevas nacerán con `status = MODERATION`.
- Un **agente IA** analizará el texto buscando spam, lenguaje ofensivo, enlaces sospechosos o patrones de fraude.
- Si la reseña **pasa** la revisión → `PUBLISHED`.
- Si **no pasa** → `CANCELED` (con razón registrada).

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
✅ El BACKOFFICE/ADMIN registra reportes (`POST /maritime-activity-reports`) con bandera (flag) — verde / amarilla / roja. Estos reportes sirven como soporte para cancelaciones por mal tiempo.

📌 **Roadmap**: DIMAR envía un **PDF diario** con el reporte. Se creará un servicio que lea automáticamente el PDF y genere el `MaritimActivityReport`. Luis está revisando si DIMAR expone API o link estable.

---

## Cosas que no son reglas, pero son inferencias importantes

### Reservas pueden compartir Payment
✅ Un `Payment` puede tener múltiples `Reservation`s (un solo pago para varios tours del carrito). 

### Comisión de la pasarela (Wompi)
✅ Wompi cobra comisión por transacción (~2.99% + IVA). **Decisión inicial**: esta comisión es **absorbida por Tourya** (sale del `slotPercentageTourya`). Se revisará más adelante si el costo lo asume el proveedor.

### Cupones y códigos de descuento (roadmap)
No hay módulo de cupones ni códigos de descuento hoy. Los overrides de precio sirven para promociones puntuales, pero no hay UI ni concepto de "cupón". 📌 **En roadmap**: implementar cupones / códigos de descuento.

### Push notifications (roadmap)
No hay integración con Firebase Cloud Messaging u otras. Las notificaciones son solo email. 📌 **Roadmap** — evaluar FCM para mobile (avisos de reserva confirmada, recordatorios, cambios de estado, crédito por expirar).
