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

### RN-005 — Access + Refresh tokens (implementado, duraciones por rol pendientes)

✅ **Base implementada 2026-07-08 (PR #159, BE-12/13/14/15)**:

- Access token JWT stateless (24h para todos los roles por ahora).
- Refresh token JWT firmado con estado en tabla `refresh_token` (30d para todos por ahora).
- Rotación al usar `POST /auth/refresh`: revoca el actual + emite nuevo par + hereda `family_id` + `previous_jti` = jti anterior.
- Detección de reuso: si un refresh revocado se reusa → revoca **toda la familia** con `revoked_reason='reuse_detected'`.
- Logout server-side: `POST /auth/logout` revoca la familia entera con `revoked_reason='logout'`.
- Backwards compat en response: `token` (legacy alias) + `accessToken` + `refreshToken` coexisten.

⚠️ **Pendiente (backlog)**: duraciones diferenciadas por rol (USER 60m/30d, PROVIDER 30m/7d, ADMIN 15m/8h), idle timeout PROVIDER/ADMIN, cookie HttpOnly para el refresh en web (item FE-01b — hoy los tokens viven en `localStorage`).

✅ **UI web implementada en PR tourya-front #60 (2026-07-10, FE-01)**: `AuthInterceptor` de Angular auto-refresca en 401 con mutex (múltiples 401 concurrentes disparan un solo `/auth/refresh`); `AuthService.logout()` llama a `/auth/logout` server-side; DTOs leen los nuevos campos `accessToken`/`refreshToken`. Backwards compat: sesiones con solo `token` legacy siguen funcionando, al expirar caen en forceLogout como antes.

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

✅ **Contacto principal de un tour**: un tour puede tener uno o varios sub-usuarios (`PROVIDER_OPERATOR`) asignados (esto ya sucede cuando se crea un `PROVIDER_OPERATOR` en la regla `RN-007` por lo que no hace falta construirlo). actualmente lo unico que falta construir, es que por cada Tour, el PROVIDER debe especificar cual de los `PROVIDER_OPERATOR` va a ser el contacto principal (marcarlo como principal✅). por default, el PROVIDER es el contacto principal de cada tour. en caso de que el PROVIDER haya creado sub-usuarios (`PROVIDER_OPERATOR`), el PROVIDER puede colocar un sub-usuario como contacto principal.

### RN-010 — Solo ADMIN aprueba/rechaza tours
✅ Endpoints `admin/acceptTourById`, `admin/returnedTourById`, `admin/cancelTourById` requieren rol ADMIN.

### RN-011 — Todos los textos del tour requieren español
✅ La validación `@NotBlank` en `TranslatedField.es` se aplica al request de creación / update. Inglés y portugués son opcionales.

**Mis Tours en español**: actualmente hay información para la creacion del tour en ingles. el boton `Create tour` debe decir `Crear Tour`. el boton `Edit tour` debe decir `Editar tour`. el boton `Edit gallery` debe decir `Editar galeria`. el boton `Schedule` debe decir `Programar disponibilidad.

### RN-012 — Tour debe tener un único Provider asignado
✅ El backend resuelve el provider del JWT del PROVIDER que crea.

### RN-013 — Restricciones de galería del tour (implementado, umbrales configurables)

✅ **Implementado en PR #161 (2026-07-09, BE-10)** en `POST /tours/{tourId}/gallery/sync` y `/syncWithUpdate`. All-or-nothing: si una imagen falla, se rechaza TODO el sync (rollback antes de tocar S3/BD).

**Reglas obligatorias**:
- Formato: JPEG, PNG o WebP
- Tamaño ≤ `GALLERY_MAX_SIZE_MB` (default 5)
- Orientación landscape (ancho ≥ alto) — rechaza portrait
- Ancho ≥ `GALLERY_MIN_WIDTH_PX` (default 800) para calidad aceptable
- Cuenta total resultante ≤ `GALLERY_MAX_IMAGES_PER_TOUR` (default 7)

**Umbrales en `app_config`** — ADMIN los ajusta sin re-deploy:
```bash
PUT /api/v1/config/GALLERY_MAX_SIZE_MB
Body: {"value": {"value": 10}, "description": "Ampliado por temporada de campaña"}
```

**Formato del error 400**:
```json
{
  "errorCode": "VALIDATION_FAILURE_CODE",
  "message": "Gallery validation failed",
  "issues": [
    {"fileName": "foto1.jpg", "code": "TOO_LARGE", "message": "..."},
    {"fileName": "foto2.png", "code": "NOT_LANDSCAPE", "message": "..."}
  ]
}
```

**Error al subir imagenes en galareia**: actualmente la pantalla para subir imagenes a un tour (Tour Gallery) muestra un error en la pantalla `Gallery [object Object]`. en esa pantalla ocultar el frame superior que dice `Tour Gallery`.

**Mis Tours en español**: actualmente hay información para la creacion del tour en ingles. el boton `Create tour` debe decir `Crear Tour`. el boton `Edit tour` debe decir `Editar tour`. el boton `Edit gallery` debe decir `Editar galeria`. el boton `Schedule` debe decir `Programar disponibilidad`. En la pantalla `Tour Gallery` el botón de enviar las imagenes debe decir `Subir imagenes`

✅ **UI implementada en PR tourya-front #59 (2026-07-10, FE-03)**: `tour-gallery.component` valida MIME, tamaño, orientación landscape, ancho mínimo y cuenta total antes de subir al backend. Umbrales leídos de `app_config` con fallback a los defaults. Los `issues[]` del backend se muestran en el mismo modal Swal si logran llegar (bypass o cambio server-side).


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

### RN-015 — Comisión Tourya por default a nivel de Tour (implementado)

✅ **Implementado en PR #163 (2026-07-09, BE-01/02)**. El campo `tour.porcentaje_tourya` (fracción decimal, ej. `0.15 = 15%`) es el default por tour que se hereda al crear cada slot nuevo. Elimina la ventana de "slot con 0% de comisión". Luego de definido el BACKOFFICE/ADMIN podrá editarlo cuando lo considere conveniente. 

**Estado en dev tras migración 071**:
- 38 tours existentes backfilleados a `0.15` (ninguno tenía valor previamente).
- `ALTER COLUMN porcentaje_tourya SET DEFAULT 0.15` para tours nuevos.
- Comment de columna limpiado (antes decía "DEPRECATED" por decisión anterior de 050 que Luis revirtió).

**Flujo**:
1. ADMIN puede diligenciar `porcentaje_tourya` al aprobar un tour (endpoint dedicado `UpdatePorcentajeTouryaRequest`), o dejar el default 15%.
2. Cuando el PROVIDER (o cualquier flujo) crea un slot vía `TourScheduleConfigGeneralService.buildSlotsAndPricesFromRequest()` o `manageSlotsUpdate()`, el `slot_porcentaje_tourya` **hereda del tour**. Antes se hardcodeaba a `ZERO`.
3. Fallback: si el tour no tiene `porcentaje_tourya` (edge case), cae a `ZERO` para no romper.
4. BACKOFFICE puede sobrescribir el `slot_porcentaje_tourya` de un slot específico usando `tour_schedule_slot_override` (RN por rango de fechas).
5. Luego de definido el campo `slot_porcentaje_tourya`, el BACKOFFICE/ADMIN podrá editarlo cuando lo considere conveniente. 

⚠️ **Notas**:
- **Slots ya existentes NO se tocan** — mantienen su `slot_porcentaje_tourya` actual (posibles overrides). Si el ADMIN quiere aplicar el default a slots viejos, usa el endpoint dedicado.
- **Nombre queda en español** (`porcentaje_tourya`) — rename a `percentage_tourya` (inglés) fue descartado por alto costo/bajo valor.

✅ **UI implementada en PR tourya-front #63 (2026-07-10, FE-05)** con backend habilitado en PR tourya-api #169: nuevo endpoint `PATCH /tours/admin/{tourId}/porcentajeTourya` que consume el `UpdatePorcentajeTouryaRequest` (que existía como dead code desde BE-01/02). El modal de aprobación de `tour-admin-detail` incluye un input numérico (0-100 %) pre-poblado con el valor actual del tour; al aceptar dispara `PATCH` + `PUT accept` si el valor cambió, o solo `PUT accept` si no. Edición del % **después** de aprobar el tour queda como FE-05b futuro.

### RN-016 — Asignación de comisión por rango de fechas
✅ `PUT /tour-schedules/tours/{tourId}/percentage` con `slotPercentageTourya + startDate + endDate` actualiza todos los slots de los `TourSchedule`s en ese rango.

### RN-017 — Override de slot puntual
✅ `PUT /tour-schedules/tours/{tourId}/percentage/{slotId}` permite ajustar el % de UN slot específico en UN día específico, sin afectar el resto.

### RN-018 — Override de precio puntual
✅ `TourSchedulePriceOverride` permite ajustar el precio de un slot en una fecha (ej. surge pricing, promoción).

### RN-019 — El PROVIDER solo ve su `providerPrice`
✅ **Actualizada 2026-07-23 (TC-008 #195, Luis)** — el PROVIDER no ve la comisión Tourya **ni el precio cliente (`price`)**. Los responses del PROVIDER omiten `slotPercentageTourya` **y `price`**. Solo ven su `providerPrice`. BACKOFFICE_OPERATION y ADMIN sí ven los 3 (`providerPrice`, `price`, `slotPercentageTourya`).

**Motivación** (Luis TC-008): al PROVIDER no le interesa saber cuánto paga el turista al final — es información de negocio de Tourya. Solo debe ver su precio de operación.

**Implementación**: `TourScheduleConfigGeneralService.mapSlotToResponse` línea 653 condiciona `priceDto.setPrice()` a `showTouryaFields = Utils.isTouryaBackoffice(roles)`. PROVIDER recibe `price = null` en el JSON.

### RN-020 — Precios por tour obligatorios
✅ Para los tours donde el `priceType` = `individual` (por persona), Cada slot debe tener un precio por cada `ageType` configurado (ADULT, CHILD, INFANT). El INFANT puede tener `providerPrice = 0`. tambien aparece un `ageType` llamado `Cualquiera` (al guardarlo lo define como adulto) y cuando se selecciona el front no debe permitir configurar los otros precios, por lo que quiere decir que todos las turistas pagarán el mismo precio.
✅ Para los tours donde el `priceType` = `grupo`, Cada slot solo debe tener un precio de `ageType` configurado (ADULT). esto quiere decir que debe aparecerle el `ageType` llamado `Cualquiera` (al guardarlo lo define como adulto).

### RN-021 — Capacidad ilimitada (limpieza de campo redundante, implementado)
✅ **Aclaración de Luis (2026-07-07)**: la capacidad ilimitada es una propiedad **del tour** — ya existe en `Tour.isUnlimitedCapacity`. El campo duplicado en `TourSchedule` es un remanente que debe **eliminarse**.

- ✅ `Tour.isUnlimitedCapacity` es la fuente de verdad.
- ✅ **Columna eliminada de `tour_schedule` y `tour_schedule_config` en la migración 029** (2026-04-08). Descubrimiento durante la auditoría del 2026-07-10: el trabajo ya se había hecho meses atrás y el backlog había quedado desactualizado (BE-03/04/FE-07 marcados como pendientes cuando ya no lo estaban).
- ✅ **Backend Java** (Tour.java + services): todos los flujos leen de `tour.getIsUnlimitedCapacity()`, ninguno consulta el schedule. `TourSchedule.java` no tiene la propiedad; el `TourScheduleConfigGeneralService:523` conserva solo un comentario legacy inocuo.
- ✅ **Frontend Angular** (`tour-schedule.component.ts:144`): getter `isUnlimitedCapacity()` que retorna `this.tour?.isUnlimitedCapacity` — la vista de schedule ya no tiene input del campo, solo lo usa como readonly/placeholder.
- Cuando un tour tiene capacidad ilimitada, al configurar el slot **solo se ingresa el precio** — no la capacidad.
- La validación de capacidad en checkout (RN-023) se salta cuando `Tour.isUnlimitedCapacity = true`.
- ⚠️ **Deuda residual (no bloqueante)**: el SP `sp_get_tour_schedule` (viejo, sin `_json`) tiene `ts.is_unlimited_capacity` en su cuerpo referenciando la columna inexistente. No lo llama ningún código Java (confirmado por grep). Es dead SQL. Cierra con un `DROP FUNCTION` cuando se quiera limpiar.

---

## 4. Carrito y checkout

### RN-022 — Hold temporal de 15 minutos (configurable, implementado)
✅ Al hacer checkout (POST `/reservations`), se crean `Reservation`s en estado `TEMPORAL` con `expiresAt = now + N minutos`. Si el usuario no paga en ese tiempo, el job `TemporalReservationExpiryJob` (corre cada 60s) las cancela (revisar porque actualmente las esta dejando en estado temportal) y libera el slot.

✅ **Implementado en PR #160 (2026-07-09)**: `ReservationService.createTemporalReservationHolds()` lee `HOLD_MINUTES` de `app_config` vía `appConfigService.getInt(HOLD_MINUTES, 15)`. Fallback silencioso a 15 si la key no está.

**Cómo ajustar sin re-deploy**:
```bash
PUT /api/v1/config/HOLD_MINUTES
Body: {"value": {"value": 30}, "description": "Ampliado a 30 min por temporada alta"}
```
Requiere token de ADMIN. Aplica desde el próximo checkout (no requiere reiniciar el servicio).

✅ **UI implementada en PR tourya-front #58 (2026-07-10)**: `AppConfigAdminComponent` en el Dashboard de ADMIN edita `HOLD_MINUTES` (y las otras 11 configs de Fase 1) con inputs numéricos y toggles booleanos, sin necesidad de curl.

### RN-023 — Validación de capacidad en checkout
✅ Al agregar al carrito, se valida que el slot tenga capacidad suficiente (`requestedUnits <= availability`). **Esta validación solo aplica si `Tour.isUnlimitedCapacity = false`**.
✅ Para los tours donde el `priceType` = `individual` (por persona), si tiene 2 adultos en el carrito, se debe validar que `availability`>=2. Para los tours donde el `priceType` = `grupo` (el campo `maxPeople` del Tour debe ser visible antes de agregarlo al carrito ), la cantidad en el carrito al seleccionar el slot debe estar por default en 1 (no toma en cuenta el numero de turistas) pero el turista puede aumentarlo. al agregarlo al carrito se debe validar al igual la capacidad suficiente (`requestedUnits <= availability`). 

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
**Contacto principal del tour**: al momento en que se genera la reserva, debe aparecer la informacion (nombre, telefono y correo electrónico) del contacto principal del tour.

**Error al comprar un tour con tipo de precio GRUPO**: 🟢 **RESUELTO** (auditoría 2026-07-24). Cuando Luis reportó este bug (2026-07-19), el mensaje era el catch-all del `handlePaymentResult` en `cart-summary.component.ts:912` — se dispara para **cualquier** error tras el pago Wompi aprobado, no es específico de GRUPO. Auditoría de logs Cloud Run dev: los últimos errores 500 en `POST /payment` fueron entre `2026-07-15 21:24Z` y `2026-07-17 00:04Z` (todos causados por bug #179 / migración 075 device_token_fcm faltante). Desde el 17-jul (fix MO-40b + HOTFIX 075) **cero errores 500 en /payment**. Cero errores del `PaymentService` en los últimos 7 días. Luis probablemente vio el mensaje antes del fix y lo reportó 2 días después. La lógica GRUPO en `ReservationService.java:1945/2012/2485` no genera fallos en producción.

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
- `LEGAL_OBLIGATIONS` — ✅ implementado 2026-07-09 (PR #166, BE-05)
- `CHANGE_OF_PLANS` — ✅ implementado 2026-07-09 (PR #166, BE-05)
- `RAIN` — solo vía `PUT /reservations/{id}/cancel/rain` (DIMAR)

> Contexto: se identificó que faltaban motivos por los cuales un turista puede razonablemente cancelar un tour. Estas razones se agregaron para cubrir esos casos.

✅ **Frontend implementado 2026-07-09** (PR tourya-front #56, FE-06): `provider-tour-management` incluye las 2 opciones en el dropdown con traducciones es/en/pt. Turista puede elegir `LEGAL_OBLIGATIONS` o `CHANGE_OF_PLANS` desde la UI. **Pendiente**: MAUI mobile (dropdown equivalente).

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

### RN-033 — Reschedule de reserva (implementado)
✅ El turista puede reagendar si:
- `canReschedule = true`.
- La política del tour `allowsRescheduling = true`.
- Antes de `maxReschedulingDate`.
- La nueva fecha **no está en el pasado** (guard agregado en BE-20, 2026-07-11).

✅ 3 casos según diferencia de precio:
- **EQUAL**: cambio directo — actualiza `Reservation` + `ShoppingCartItem`, marca `RESCHEDULED`, recalcula `maxCancellationDate`/`maxReschedulingDate`.
- **LOWER**: se genera `Credit` por la diferencia (`currentPrice - newPrice`) con `expirationDate = today + CREDIT_EXPIRATION_MONTHS` (`app_config`, RN-036).
- **HIGHER**: cancela la reserva anterior (sin crear crédito duplicado), crea `Credit` con el valor original pagado, limpia items ACTIVE del carrito, agrega el nuevo item con la nueva fecha. El turista completa el pago vía checkout normal (crédito + diferencia por Wompi). Response `transactionStatus = CANCELLED_AND_ADDED_TO_CART`.

✅ **Guardas comunes** (validate + execute):
- No permite reagendar reservas CANCELED, DELIVERED o ya RESCHEDULED (un solo reagendamiento por reserva).
- Valida pertenencia al usuario autenticado.
- Valida capacidad del nuevo slot (`tourScheduleSlotAvailabilityService.ensureSlotHasCapacity`).
- Recalcula availability de slot viejo y nuevo tras el cambio.
- Todo el flow bajo `@Transactional` a nivel de clase + método.

✅ **Implementado en PR #170 (2026-07-11, BE-20)**: auditoría línea por línea (los 3 casos ya estaban implementados desde antes), agregado guard de `newDate` en el pasado, primera suite JUnit del proyecto (14 tests con Mockito en `ReservationServiceRescheduleTest`).

⚠️ **Deuda futura BE-20b**: test integrado con `@SpringBootTest` + Testcontainers para verificar los 3 flujos end-to-end con BD real (createCredit, addItemToCart, recálculos).

### RN-034 — Job de expiración de flags de cancelación/reschedule
✅ `ReservationCancellationFlagsJob` corre diariamente a las **5:00 AM Bogotá**. Desactiva `canCancel = false` / `canReschedule = false` cuando `maxCancellationDate / maxReschedulingDate < today`.

---

## 6. Créditos

### RN-035 — Origen de créditos
✅ Un crédito se genera por:
- Cancelación de reserva (por turista o por lluvia).
- Reschedule a tour más barato.
- Transferencia desde otro turista.

### RN-036 — Expiración de créditos (configurable, implementado)
✅ `expirationDate = creationDate + CREDIT_EXPIRATION_MONTHS` (configurable en `app_config`). Después de expirar, no se pueden usar.

✅ **Implementado en PR #160 (2026-07-09)**: `ReservationService` en 3 puntos donde crea `Credit` lee `CREDIT_EXPIRATION_MONTHS` de `app_config` vía `appConfigService.getInt(CREDIT_EXPIRATION_MONTHS, 6)`.

⚠️ **Corrección vs backlog original**: el código real usa **6 meses**, no 1 año como decía el backlog. Se mantuvo 6 como default para no cambiar comportamiento. Si Luis define oficialmente que deben ser 12 meses, se cambia sin deploy:
```bash
PUT /api/v1/config/CREDIT_EXPIRATION_MONTHS
Body: {"value": {"value": 12}, "description": "Alineado con política oficial"}
```

✅ **BE-18/BE-19 implementados en PR #173 (2026-07-12)**: `CreditExpirationJob` corre cron 5am Bogotá diariamente. 3 pases por corrida — recordatorio a 30 días, a 7 días, y aviso al expirar. Idempotencia por columnas timestamp (`reminder_30d_sent_at`, `reminder_7d_sent_at`, `expired_notified_at`) — si el mail falla, el timestamp no se marca y la próxima corrida reintenta. Nuevo valor `EXPIRED` en `CreditStatusEnum` — todos los checks existentes `status != CREATED` lo rechazan automáticamente (cero regresión en checkout/reserva/transferencia). Migración 074 hace backfill de créditos ya vencidos sin reenviar correo retroactivo. 7 tests JUnit con Mockito.

### RN-037 — Reserva parcial de crédito
✅ En checkout, el turista puede pre-reservar parte del crédito (`POST /credits/reserve`). El monto queda en `reservedAmount` hasta que se confirme el pago o expire el hold.

### RN-038 — Transferencia de crédito (una vez)
✅ Un crédito se puede transferir a otro turista UNA vez (`POST /credits/{creditId}/transfer`). Identificación por documento del destinatario.

⚠️ Una vez transferido, no se puede revertir.

### RN-039 — Consumo de créditos en pago híbrido
✅ El turista puede pagar parte con crédito y parte con tarjeta (`paymentType = CREDIT_AND_PLATFORM`). Validación: la suma debe igualar el total.

---

## 7. Payouts a proveedores

### RN-040 — Buffer de N días para payout (configurable, implementado)
✅ Una reserva entra al payout solo si han pasado **N días** desde su `reservationDate` (`payoutAvailableDate = reservationDate + PAYOUT_BUFFER_DAYS`). Esto da tiempo a reclamos/disputas.

✅ **Implementado en PR #160 (2026-07-09)**: `ReservationService` al confirmar pago lee `PAYOUT_BUFFER_DAYS` de `app_config` vía `appConfigService.getInt(PAYOUT_BUFFER_DAYS, 2)`. Default: 2 días. Aplica solo a **reservas nuevas**; las existentes con `payout_available_date` ya seteado no cambian.

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
**Proceso de registro de proveedor**: todo el proceso debe estar completamente en español. La creacion del registro del proveedor tendrá los siguientes campos:
1. llenar el formulario con la informacion: Informacion del Contacto (nombre, apellido, correo electronico, contraseña), informacion de la empresa(Tipo de documento, numero de documento, nombre de la empresa, tipo de servicio (debe ser seleccion multiple), pais, departamento ciudad, telefono de empresa, RNT, direccion de empresa (que muestre direcciones y lugares en la isla de san andres)).
2. Al presionar el boton de `Registrarme` el back debe hacer lo siguiente: crear el registro de la cuenta del proveedor con la informacion enviada en el formulario, enviar un correo electrónico con el codigo para la activacion de la cuenta y por ultimo mostrar un modal con el siguiente mensaje: `¡Su registro de proveedor ha sido creado! Revisa tu correo electrónico para activar tu cuenta y continuar con el proceso`. El modal debe tener un boton que diga `Entendido, ir al inicio`, y al presionar el boton lo debe llevar al formulario de login.
3. El correo electrónico enviado para la activacion de la cuenta debe tener lo siguiente:  Subject (`Activacion de cuenta Tourya`) y mensaje (`Hola` + campo `Nombre` `apellido` del contacto del proveedor, gracias por registrarte en Tourya! Para activar tu cuenta debes dar clic en el sigiente boton'. el boton debe decir `Activar cuenta`). al presoinar el boton debe mostrar una mensaje que diga `Activando tu cuenta... Por favor, espera mientras verificamos tu cuenta. Cuenta activada satisfactoriamente.`. en caso de que el contacto del proveedor no revise el correo a tiempo para activar la cuenta, el BACKOFFICE/ADMIN podrá enviarle el correo nuevamente.
4. Luego el contacto del proveedor debe ingresar al login y debe aparecer el formulario de solicitud de proveedor (Request provider) en español y el contacto del proveedor debe dar clic en el boton `Enviar solicitud`.
5. Luego que el BACKOFFICE/ADMIN Pre-apruebe la solicitud del proveedor el proveedor debe adjuntar documentos requeridos de la empresa. luego el contacto del proveedor debe dar clic en el boton `Enviar documentos` y debe mostrar un modal con el siguiente mensaje `Documentos enviados con éxito` y con un boton de Aceptar para cerrar el modal y volver al panel del proveedor. 
6. Luego el BACKOFFICE/ADMIN pruebe aprobar, cancelar o solicitar mas informacion.

### RN-045 — Documentos obligatorios para KYB (implementado con feature flag, default OFF)

✅ **Implementado en PR #162 (2026-07-09, BE-11)** en `RequestProviderService.send()`:

Cuando el feature flag `KYB_REQUIRE_MANDATORY_DOCS` (en `app_config`) es **1**, `PUT /requestProvider/user/send` verifica que existan galleries adjuntas para cada `RequestProviderDocumentType` con `mandatory=true`. Si falta alguno → 400 con:

```json
{
  "errorCode": "VALIDATION_FAILURE_CODE",
  "message": "KYB submit rechazado: faltan documentos obligatorios",
  "missingDocuments": ["RUT", "RNT", "Seguros de operación"]
}
```

Cuando el flag es **0** (default), el `send()` se comporta como antes (no valida). Esto es intencional para no bloquear QA in-flight — en dev hay 12 requests en estado `Created` que aún no submitieron, algunos sin los 7 documentos obligatorios.

**Documentos obligatorios reales en BD** (`RequestProviderDocumentType` con `mandatory=true`):
1. Cámara de comercio
2. RUT
3. RNT (Registro Nacional de Turismo)
4. Cédula del representante legal
5. Seguros de operación
6. Contrato de mandato (firmado)
7. Contrato de vinculación (firmado)

**Cómo activar** cuando Luis diga que dev está listo:
```bash
PUT /api/v1/config/KYB_REQUIRE_MANDATORY_DOCS
Body: {"value": {"value": 1}, "description": "Política RN-045 activa"}
```
(requiere token ADMIN, sin re-deploy)

⚠️ **Nota histórica**: hay providers en `Approved` con menos de 7 documentos obligatorios (aprobados manualmente antes de esta validación). El flag ON solo afecta a **nuevos submits**; los históricos quedan como están. Si Luis quiere re-verificar el histórico, es un item aparte.

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
**Reglas obligatorias**:
- Formato: JPEG, PNG o WebP
- Tamaño ≤ `GALLERY_MAX_SIZE_MB` (default 1)

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

- **Cancelaciones por lluvia**: Cuando se crea un reporte de activida maritima (DIMAR) con bandera roja, un job debe buscar y cancelar todas las reservas donde la categoria y la subcategoria coincida con la del reporte, y que el dia de la reserva (`tour.reservationDate`) sea igual o este dentro de la fecha de inicio y fin del reporte. al cancelar cada reserva se debe crear un credito con el monto de la reserva y a nombre del turista de la reserva cancelada.

📌 **Roadmap**: DIMAR envía un **PDF diario** con el reporte. Se creará un servicio que lea automáticamente el PDF y genere el `MaritimActivityReport`. Luis está revisando si DIMAR expone API o link estable.

### RN-055 — Provider Decline: cancelación automática + crédito al turista (rediseño Luis 2026-07-23)
Cuando un provider avisa que no puede atender una reserva ya pagada (aviso de última hora), el sistema procesa la cancelación automáticamente. **No hay reasignación manual del ADMIN** (rediseño confirmado por Luis en issue #193 el 2026-07-23; la propuesta original de "reasignar a otro provider" fue descartada por complejidad de precio/payout/capacidad).

**Flujo para el TURISTA**:
1. El provider marca la reserva como "no puedo atender" (endpoint decline). Se persiste `reservation.provider_declined_at` con timestamp.
2. **Automáticamente** el sistema cancela la reserva y genera un `Credit` a favor del turista con el monto original (reusa la lógica de RN-054 / BE-23 — cancelación retroactiva con crédito).
3. **Email al turista** con:
   - Notificación de la cancelación por parte del provider.
   - Confirmación del crédito generado y su valor.
   - **Lista de tours alternativos** de la misma subcategoría del tour cancelado, invitándolo a usar el crédito.
4. El turista decide:
   - **Reservar otro tour** de su preferencia usando el crédito.
   - **Solicitar reembolso** del dinero (flujo manual con el equipo de operaciones).

**Flujo para el PROVEEDOR**:
1. Del payout del proveedor se **excluyen** las reservas donde `reservation.provider_declined_at IS NOT NULL` (el proveedor no cobra por reservas que declinó).
2. Roadmap: **penalizaciones al provider** según ventana temporal del decline:
   - **≥ 48h antes** del tour: sin multa económica. Crédito al turista del 100%.
   - **24h – 48h antes**: cobro del 50% de la comisión Tourya. Descenso leve en las búsquedas por 7 días. Crédito al turista del 100% + cupón adicional del 5% (disculpas).
   - **< 24h antes**: cobro del 100% de la comisión. Descenso en las búsquedas por 15 días. Crédito al turista del 100% + cupón adicional del 10%.
   - **NO_SHOW** (provider no llegó ni declinó): cobro de la comisión completa + multa fija (ej. USD 20). Suspensión de la cuenta por 7 días o expulsión definitiva si es reincidente. Crédito al turista del 100% + cupón adicional del 20%.

**Estados válidos para el decline**: reserva no terminal (no `CANCELED`/`NO_SHOW`/`DELIVERED`).

**Impacto técnico** (tracked en backlog como **BE-24**; FE-13 queda cancelado porque ya no hay UI de reasignación):

**Fase 1 (BE-24 core, arrancable ya)**:
- Migración: `provider_declined_at TIMESTAMPTZ NULL` en `reservation`.
- Endpoint: `PUT /provider/reservations/{id}/decline` — el provider marca la reserva. Trigger interno: cancela + crea crédito (reusa `MaritimeAlertEventListener` / BE-23) + envía email.
- Ajustar `ProviderPayoutOrderService`: excluir reservas con `provider_declined_at IS NOT NULL` del payout.
- Email template nueva `provider-declined-notification.html` con listado de tours alternativos (query por `sub_category` = tour cancelado, disponibilidad futura).

**Fase 2 (roadmap penalizaciones, futuro)**:
- Sistema completo de multas económicas según ventana temporal (48h/24-48h/<24h/no-show).
- Ajuste automático de comisión al calcular payout de la reserva declinada.
- Métrica de reputación del provider (descenso en búsquedas + suspensiones).
- Cupones de disculpas al turista adicionales al crédito 100%.

**Talla**: Fase 1 = M (~4-6 h); Fase 2 = XL (roadmap sin timeline definido).

### RN-056 — Ventana temporal para confirmar reserva (TC-007 #194, Luis 2026-07-23)
✅ El PROVIDER (y `PROVIDER_OPERATOR`) solo puede marcar una reserva como `DELIVERED` (confirmarla / escanear el QR) **el mismo día** del tour (`reservation.reservationDate.toLocalDate()`). Antes o después, el endpoint responde `400` con mensaje `"Only can confirm reservation on the tour day (tourDate=X, today=Y)"` y el botón "Confirmar" queda oculto en la UI.

**Motivación** (Luis TC-007): no se puede validar que el turista asistió si el tour aún no ha ocurrido, y confirmar después del día genera confusión con el flujo `NO_SHOW` (que `PendingReservationNoShowJob` ejecuta automáticamente a las 7am Bogotá del día siguiente).

**Zona horaria**: `America/Bogota`. Antes el helper `computeCanConfirmReservation` usaba `LocalDate.now()` sin zona — bug latente en el borde del día si el server está en UTC.

**Implementación**:
- **Backend** — `ReservationService.consumeReservation` (línea 845): guard temporal que compara `reservation.getReservationDate().toLocalDate()` con `LocalDate.now(BOGOTA)`. Si difieren, `IllegalStateException` → 400.
- **Backend** — `ReservationService.computeCanConfirmReservation` (línea 1358): usa `LocalDate.now(BOGOTA)` para el flag `canConfirmReservation` del response.
- **Frontend** — el HTML ya respeta `canConfirmReservation` via `booking-management-config.service.ts:300`. Fix colateral: `mapProviderReservationToBooking` y `mapClientReservationToBooking` deben copiar el flag (antes solo `mapReservationToBooking` para modal de detalles lo copiaba, por eso el botón aparecía en la tabla de la lista).
- **Mobile** — el mismo endpoint valida al escanear QR. No requiere cambio de código.

**Aplica a**: PROVIDER y `PROVIDER_OPERATOR` — ambos tienen el permiso "Confirmar reservas (QR)" según matriz doc 03:175.

---

## Cosas que no son reglas, pero son inferencias importantes

### Reservas pueden compartir Payment
✅ Un `Payment` puede tener múltiples `Reservation`s (un solo pago para varios tours del carrito). 

### Direcciones con google maps
✅ en la direccion del tour y en la direccion del hospedaje en el checkout hay campos donde muestra las ubicaciones de google maps. actualmente muchas direcciones no se encuentran. por ejemplo no se encuentra ningun hotel en san andres islas. 

### Comisión de la pasarela (Wompi)
✅ Wompi cobra comisión por transacción (~2.99% + IVA). **Decisión inicial**: esta comisión es **absorbida por Tourya** (sale del `slotPercentageTourya`). Se revisará más adelante si el costo lo asume el proveedor.

### Cupones y códigos de descuento (roadmap)
No hay módulo de cupones ni códigos de descuento hoy. Los overrides de precio sirven para promociones puntuales, pero no hay UI ni concepto de "cupón". 📌 **En roadmap**: implementar cupones / códigos de descuento.

### Push notifications (roadmap)
No hay integración con Firebase Cloud Messaging u otras. Las notificaciones son solo email. 📌 **Roadmap** — evaluar FCM para mobile (avisos de reserva confirmada, recordatorios, cambios de estado, crédito por expirar).
