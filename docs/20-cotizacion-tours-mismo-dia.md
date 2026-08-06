# 20 — Cotización en línea de tours para el mismo día (RFQ)

> Propuesta funcional y técnica. Sigue el formato de los docs 01–18. **Numeración confirmada con Luis**: el 19 queda reservado para la propuesta de certificaciones (aún no cerrada) y este documento es el **20**. Numeración de reglas de negocio (`RN-Qxx`) es referencial.
>
> **Estado**: 📍 **Roadmap de producto** — confirmado con Luis que esta funcionalidad **no entra en el lanzamiento inicial**. Sale en una versión posterior, **después de la salida en vivo**. Este documento queda listo como especificación para cuando se priorice, no como trabajo inmediato.
>
> **v2.2** — se corrige el criterio de cierre definitivo de una oferta: rechazar una contrapropuesta **no** cierra la oferta, solo la devuelve a su estado original. Se define reportería futura con las métricas ya recomendadas.

---

## Resumen

Funcionalidad tipo **RFQ (Request for Quote)** para tours del mismo día, mencionada en tu roadmap como el módulo *"Hot Sale / Black Friday"* ([01 — Visión y negocio](01-vision-y-negocio.md)):

1. El turista define **ciudad + subcategoría + viajeros** (desglosados por tipo: adultos, niños, etc.).
2. La plataforma envía **push notification** a los operadores cuyos tours califiquen (ciudad, subcategoría, disponibilidad hoy, tour aprobado).
3. Cada operador elegible **oferta un precio** (`offeredPrice`) sobre uno de sus tours calificados — precio ad hoc, válido solo para esa cotización, que **no modifica** el precio configurado del slot/schedule.
4. El sistema calcula el **precio al cliente** aplicando el `percentageTourya` del tour sobre `offeredPrice`.
5. El cliente ve todas las ofertas recibidas y, por cada una, puede **aceptarla directamente** (inicia pago) o **contraproponerle un nuevo precio** (a una o varias ofertas a la vez).
6. Si hay contrapropuesta, el operador correspondiente puede **aceptarla o rechazarla** — sin más vueltas de negociación.
7. **El primer operador que acepte** (ya sea la oferta original por el cliente, o una contrapropuesta) **se queda con el negocio**; las demás ofertas de esa solicitud quedan invalidadas.
8. El cliente paga; se crea la reserva en `TEMPORAL` con el mismo **hold de 15 minutos** ya implementado — si no paga a tiempo, se cancela; si paga a tiempo, sigue el flujo estándar de confirmación.

---

## Decisiones de negocio (confirmadas en esta versión)

- **`offeredPrice` = lo que el operador quiere recibir** (equivalente a su `providerPrice`), no el precio final al cliente. Es un precio **ad hoc**, exclusivo de esa cotización puntual — **no toca** el precio configurado del slot/schedule del tour.
- El precio al cliente se calcula con el **`percentageTourya` del `Tour`** (el valor por defecto a nivel tour), **no** con el `slotPercentageTourya` de un slot/fecha específico:

  ```
  clientPrice = offeredPrice × (1 + Tour.percentageTourya / 100)
  ```

  Ejemplo: `Tour.percentageTourya = 20`, el operador ofrece `offeredPrice = $100.000` → el cliente ve `$120.000`.

- Si el tour se vende **por tipo de viajero** (adulto/niño con precios distintos), el operador arma su oferta ingresando un precio unitario por cada tipo que el cliente pidió, y `offeredPrice` es la suma. Si el tour se vende **como grupo con precio plano**, el operador ingresa un único `offeredPrice` total.
- El cliente puede **aceptar directamente** una oferta (sin necesidad de que el operador confirme de nuevo — la oferta ya era firme), o **contraproponer** un nuevo precio sobre una o varias ofertas a la vez.
- Si el cliente contrapropone, el operador solo puede **aceptar o rechazar** esa contrapropuesta puntual — no hay una segunda vuelta. **Si el operador rechaza, la oferta NO queda cerrada** — vuelve a su estado original (`PENDING`, con el `offeredPrice`/`clientPrice` que envió inicialmente), y el cliente puede seguir interactuando con ella (aceptarla tal cual, o volver a contraproponer).
- Una oferta queda **cerrada definitivamente solo por 3 motivos**: (1) el cliente selecciona otra oferta distinta, (2) se agota la ventana de negociación de 15 minutos, o (3) la disponibilidad del proveedor ya no alcanza para cumplirla.
- **El primer operador que acepte se queda con el negocio** — si el cliente contrapropuso a varias ofertas en paralelo, gana quien acepte primero.
- El **operador puede editar o retirar su oferta original** mientras siga `PENDING` (antes de que el cliente la acepte o contraproponga sobre ella).
- Un operador puede enviar **varias ofertas a la misma solicitud si tiene varios tours distintos que califican** (ej. el cliente pide 4 viajeros para un carro playero; si el operador tiene un carro playero para 4 **y** otro para 6, puede ofertar con ambos — la capacidad del slot solo necesita ser suficiente, no exacta). El cliente decide cuál le conviene.
- **Dos ventanas de tiempo distintas, no una sola**:
  1. **Ventana de negociación**: desde que el cliente crea la `TourQuoteRequest`, hay un plazo — **configurable por ADMIN, con 15 minutos por defecto** — para llegar a un acuerdo (una oferta aceptada directamente, o el operador acepta una contrapropuesta). Si se agota sin acuerdo, la solicitud se cierra (`EXPIRED`).
  2. **Ventana de pago**: una vez cerrado el acuerdo, el cliente tiene los **15 minutos ya definidos actualmente** (`tourya.reservations.holdMinutes`, ver [06 — Flujo 5](06-flujos-y-eventos.md)) para pagar — sin cambios sobre lo ya implementado.
- **Fórmula de la contrapropuesta (inversa a la de la oferta)**: `clientCounterPrice` es un precio **final al cliente** (a diferencia de `offeredPrice`, que es lo que el operador quiere recibir). Al operador se le muestra cuánto le quedaría **quitándole el porcentaje Tourya**:

  ```
  impliedProviderAmountForCounter = clientCounterPrice / (1 + Tour.percentageTourya / 100)
  ```

  Ejemplo: `Tour.percentageTourya = 20`, el cliente contrapropone `clientCounterPrice = $120.000` → al operador se le muestra que recibiría `$120.000 / 1.20 = $100.000`.

---

## Actores involucrados

| Actor | Rol en esta funcionalidad |
|-------|---------------------------|
| **Turista (USER)** | Crea la solicitud (ciudad, subcategoría, viajeros por tipo). Recibe ofertas, las acepta o contraoferta, paga. |
| **PROVIDER** | Recibe push cuando alguno de sus tours califica. Oferta un precio ad hoc. Puede aceptar/rechazar contrapropuestas del cliente. Debe mantener su schedule de hoy (cupos) actualizado — si no, no califica (ver **RN-Q02**). |
| **PROVIDER_OPERATOR** | No participa — ofertar precio es una decisión reservada al titular, mismo criterio que fijar precios de schedule (ver [03 — Roles y actores](03-roles-y-actores.md)). |
| **ADMIN / BACKOFFICE_OPERATION** | No participa en la negociación. Ver preguntas abiertas sobre reportería. |

---

## Entidades

### `TourQuoteRequest`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `userId` | FK → `User` (turista) | |
| `cityId` / `cityName` | — | Misma taxonomía de ubicación que la búsqueda estándar |
| `subCategoryId` | FK | |
| `serviceDate` | Date | Siempre "hoy" en esta versión; se modela genérico por si se extiende a futuro |
| `travelerDetails` | `[{ageType, quantity}]` | Mismo patrón que `ShoppingCartItemDetail` (ADULT/CHILD/BABY/ANY + cantidad) |
| `status` | Enum | `OPEN`, `COMPLETED`, `EXPIRED`, `CANCELED` |
| `expiresAt` | DateTime | `createdAt + TOUR_QUOTE_REQUEST_WINDOW_MINUTES` (config administrable por ADMIN, default 15 min — ver sección de Configuración) |
| `selectedOfferId` | FK → `TourQuoteOffer`, nullable | Se completa cuando una oferta gana el negocio |
| `createdAt` | DateTime | |

### `TourQuoteOffer`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `quoteRequestId` | FK → `TourQuoteRequest` | |
| `tourId` | FK → `Tour` | |
| `providerId` | FK → `Provider` | |
| `tourScheduleId` / `slotId` | FK | Slot de **hoy** usado para acreditar disponibilidad — su precio de catálogo no se toca |
| `pricingMode` | Enum | `PER_TRAVELER_TYPE` (precio unitario por tipo de viajero) o `FLAT` (precio único total) |
| `offeredPrice` | BigDecimal | Lo que el operador quiere recibir. Si `PER_TRAVELER_TYPE`, es la suma de `TourQuoteOfferDetail` |
| `clientPrice` | BigDecimal, calculado | `offeredPrice × (1 + Tour.percentageTourya / 100)` — ver **RN-Q04** |
| `clientCounterPrice` | BigDecimal, nullable | Precio **final al cliente** que el cliente contrapropone sobre esta oferta puntual (a diferencia de `offeredPrice`, que es lo que el operador quiere recibir) |
| `impliedProviderAmountForCounter` | BigDecimal, calculado, nullable | Solo si hay `clientCounterPrice`: `clientCounterPrice / (1 + Tour.percentageTourya / 100)` — lo que el operador vería que recibiría si acepta la contrapropuesta (ver **RN-Q04b**) |
| `status` | Enum | `PENDING`, `CLIENT_COUNTERED`, `ACCEPTED_BY_CLIENT`, `ACCEPTED_BY_PROVIDER`, `CLOSED`, `WITHDRAWN`, `EXPIRED` |
| `closedReason` | Enum, nullable | Solo si `status = CLOSED`: `OTHER_OFFER_SELECTED`, `WINDOW_EXPIRED`, `NO_AVAILABILITY` — ver **RN-Q07b** |
| `createdAt`, `respondedAt` | DateTime | |

### `TourQuoteOfferDetail` (solo si `pricingMode = PER_TRAVELER_TYPE`)

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `quoteOfferId` | FK → `TourQuoteOffer` | |
| `ageType` | Enum | ADULT/CHILD/BABY/ANY, debe corresponder a un `ageType` presente en `travelerDetails` de la solicitud |
| `quantity` | Integer | |
| `unitPrice` | BigDecimal | Precio que el operador quiere recibir por viajero de ese tipo |

### Extensión a `Reservation` (existente)

| Campo nuevo | Tipo | Notas |
|-------------|------|-------|
| `originQuoteOfferId` | FK → `TourQuoteOffer`, nullable | `totalAmount` = `clientPrice` (o `clientCounterPrice` si el trato se cerró por esa vía) de la oferta ganadora — no se recalcula por catálogo. |

### Dependencia: `Tour.percentageTourya` (confirmado, ya existe)

El `ADMIN` define el `percentageTourya` **al aprobar el tour** (`PUT /tour/admin/acceptTourById/{id}`) — es un campo que ya existe en el modelo de `Tour`. Es el valor por defecto usado en este flujo (**RN-Q04**), distinto del `slotPercentageTourya` que backoffice ajusta por slot/fecha para el catálogo estándar.

### Configuración: ventana de negociación (nueva)

Se agrega un parámetro de configuración administrable, siguiendo el mismo patrón que `AUTH_RATE_LIMIT_ENABLED`/`AUTH_LOCKOUT_ENABLED` (tabla `app_config`, ver [12 — Seguridad y autenticación](12-seguridad-y-auth.md)):

| Config key | Default | Notas |
|------------|---------|-------|
| `TOUR_QUOTE_REQUEST_WINDOW_MINUTES` | `15` | Minutos desde `TourQuoteRequest.createdAt` hasta `expiresAt`. Editable por `ADMIN` vía `PUT /config/TOUR_QUOTE_REQUEST_WINDOW_MINUTES`, sin necesidad de re-deploy — igual que los flags de seguridad ya implementados. |

La ventana de **pago** (una vez cerrado el acuerdo) sigue siendo la ya existente, `tourya.reservations.holdMinutes` (hoy 15 min) — **no se toca**.

---

## Reglas de negocio

- **RN-Q01 — Notificación push obligatoria**: al crear una `TourQuoteRequest`, el sistema identifica los tours elegibles (**RN-Q02**) y envía de inmediato una **notificación push** (FCM, ya implementado — ver [11 — Integraciones](11-integraciones.md)) a cada `PROVIDER` dueño de esos tours. No es opcional: es el único mecanismo de descubrimiento del operador en este flujo.

- **RN-Q02 — Elegibilidad de un tour**: un tour califica para recibir el push y ofertar si: (a) pertenece a la ciudad y subcategoría solicitadas, (b) `Tour.status = ACCEPTED` (aprobado), (c) tiene un `TourSchedule` de **hoy** con `availability` suficiente para el total de viajeros solicitado (`Σ travelerDetails.quantity`).

- **RN-Q03 — Construcción de la oferta del operador**: el operador elige `pricingMode` al ofertar:
  - `PER_TRAVELER_TYPE`: ingresa un `unitPrice` por cada `ageType` presente en `travelerDetails` de la solicitud. `offeredPrice = Σ(unitPrice × quantity)`.
  - `FLAT`: ingresa un único `offeredPrice` total.
  
  En ambos casos, `offeredPrice` es un precio **ad hoc**: no modifica `TourScheduleConfigPrice` ni ningún valor de catálogo del slot usado para acreditar disponibilidad.

- **RN-Q04 — Cálculo del precio al cliente (confirmado)**: `clientPrice = offeredPrice × (1 + Tour.percentageTourya / 100)` — usa el porcentaje **del tour**, no el del slot específico. `Tour.percentageTourya` lo define el `ADMIN` al aprobar el tour — ya existe como campo (ver **Dependencia**, arriba).

- **RN-Q04b — Cálculo del monto para el operador ante una contrapropuesta (confirmado)**: a diferencia de `offeredPrice`, `clientCounterPrice` es un precio **final al cliente**. El sistema le muestra al operador el monto que recibiría aplicando la fórmula inversa:

  ```
  impliedProviderAmountForCounter = clientCounterPrice / (1 + Tour.percentageTourya / 100)
  ```

- **RN-Q05 — Visibilidad para el cliente**: el cliente solo ve las `TourQuoteOffer` en `PENDING` o `CLIENT_COUNTERED` de su solicitud, con el tour, su calificación (rating existente) y `clientPrice`.

- **RN-Q06 — Decisión del cliente sobre cada oferta**: sobre cada oferta visible (`PENDING` o `CLIENT_COUNTERED`), el cliente puede:
  - **Aceptarla directamente** (`status → ACCEPTED_BY_CLIENT`) → dispara de inmediato el hold de 15 minutos (**RN-Q09**), sin necesidad de una confirmación adicional del operador (la oferta ya era firme).
  - **Contraproponer** un `clientCounterPrice`, sobre una o varias ofertas a la vez (`status → CLIENT_COUNTERED` en cada una). Si el operador rechaza esa contrapropuesta, la oferta vuelve a `PENDING` (**RN-Q07**) y el cliente puede volver a intentar sobre ella — no hay límite de rondas mientras la oferta siga abierta (no `CLOSED`/`WITHDRAWN`/`EXPIRED`).

- **RN-Q07 — Decisión del operador sobre la contrapropuesta (corregido)**: el operador de una oferta en `CLIENT_COUNTERED` puede:
  - **Aceptarla** (`status → ACCEPTED_BY_PROVIDER`) → gana el negocio, dispara **RN-Q08/RN-Q09**.
  - **Rechazarla** → la oferta **NO se cierra**. Vuelve a `status = PENDING`, se limpian `clientCounterPrice` e `impliedProviderAmountForCounter`, y queda exactamente como el operador la envió originalmente (`offeredPrice`/`clientPrice` sin cambios). El cliente puede seguir interactuando con ella — aceptarla tal cual o volver a contraproponer con otro monto.

- **RN-Q07b — Los únicos 3 motivos de cierre definitivo (confirmado)**: una `TourQuoteOffer` pasa a `status = CLOSED` (con su `closedReason`) **solo** cuando ocurre alguno de estos tres eventos — nunca por un simple rechazo de contrapropuesta:
  1. **`OTHER_OFFER_SELECTED`** — el cliente elige otra oferta distinta dentro de la misma `TourQuoteRequest` (ver **RN-Q08**).
  2. **`WINDOW_EXPIRED`** — se agota la ventana de negociación (`TOUR_QUOTE_REQUEST_WINDOW_MINUTES`, **RN-Q13**) sin que esa oferta haya sido seleccionada.
  3. **`NO_AVAILABILITY`** — la disponibilidad del proveedor para ese slot ya no alcanza para cumplir la oferta (por ejemplo, se agotó por otra reserva estándar del catálogo mientras la cotización seguía abierta). Se valida en dos momentos: (a) de forma perezosa, cada vez que el cliente consulta `GET /tours/quote-requests/{id}/offers` (las ofertas sin disponibilidad ya no se listan como interactuables, se marcan `CLOSED`), y (b) de forma obligatoria justo antes de confirmar cualquier aceptación (`accept` o `accept-counter`), para evitar comprometer un cupo que ya no existe.

- **RN-Q08 — El primero en aceptar se queda con el negocio (confirmado)**: apenas una oferta de una `TourQuoteRequest` pasa a `ACCEPTED_BY_CLIENT` o `ACCEPTED_BY_PROVIDER`, el sistema, en la misma transacción:
  1. Marca `TourQuoteRequest.status = COMPLETED`, `selectedOfferId` = esa oferta.
  2. Pasa automáticamente a `status = CLOSED` (`closedReason = OTHER_OFFER_SELECTED`) el resto de `TourQuoteOffer` `PENDING`/`CLIENT_COUNTERED` de esa solicitud — incluida cualquier contrapropuesta que otro operador intente aceptar después de este punto.
  
  ⚠️ **Nota de implementación**: es una condición de carrera real (dos operadores podrían intentar aceptar casi simultáneamente). Debe resolverse con una operación atómica a nivel de `TourQuoteRequest` (ej. `UPDATE ... WHERE status = 'OPEN'` condicional, o `SELECT ... FOR UPDATE`), para garantizar que solo una oferta gane incluso bajo concurrencia.

- **RN-Q09 — Hold de 15 minutos (confirmado, reutiliza lo existente)**: al ganar el negocio (**RN-Q08**), el sistema agrega el tour/slot al carrito del cliente con `clientPrice` (o `clientCounterPrice` si se cerró por esa vía) como precio fijo, y continúa el [Flujo 5 existente](06-flujos-y-eventos.md): `Reservation` en `TEMPORAL`, `expiresAt = now + 15 min`.
  - Si el cliente **no paga** dentro de los 15 minutos → la reserva se cancela (libera el hold), mismo comportamiento que ya ejecuta `TemporalReservationExpiryJob` hoy.
  - Si el cliente **paga a tiempo** → sigue el flujo estándar: `deliveryStatus` `PENDING → CONFIRMED`, se genera el QR, email de confirmación — sin cambios sobre lo ya implementado.

- **RN-Q10 — La disponibilidad no se bloquea al ofertar**: igual que en el flujo estándar, `availability` del slot no se descuenta al ofertar ni al contraproponer — solo se valida en el momento (**RN-Q02c**). El descuento real ocurre al crear el hold (**RN-Q09**).

- **RN-Q11 — Varias ofertas por operador, si tiene varios tours que califican (confirmado)**: un operador puede enviar una oferta independiente por cada uno de sus tours que califique (**RN-Q02**) para la misma `TourQuoteRequest`. Ejemplo: el cliente pide 4 viajeros para un tour de carro playero; si el operador tiene un carro playero con capacidad para 4 **y** otro con capacidad para 6, ambos califican (la capacidad solo necesita ser **suficiente**, no exacta — ver RN-Q02c) y puede ofertar con los dos. El cliente decide cuál le conviene.

- **RN-Q12 — Edición y retiro de la oferta original (confirmado)**: mientras una `TourQuoteOffer` siga `PENDING` (el cliente aún no la aceptó ni contrapropuso sobre ella), el operador puede **editarla** (cambiar `offeredPrice`/detalle por `ageType`) o **retirarla** (`status → WITHDRAWN`). Una vez que el cliente reacciona (acepta o contrapropone), la oferta ya no se puede editar ni retirar.

- **RN-Q13 — Ventana de negociación configurable (confirmado)**: el plazo de `TourQuoteRequest.expiresAt` no está fijo en 15 minutos en el código — es el valor del config `TOUR_QUOTE_REQUEST_WINDOW_MINUTES` (default 15), editable por `ADMIN` sin necesidad de re-deploy, igual que los flags de seguridad ya implementados (`AUTH_RATE_LIMIT_PER_MINUTE`, etc. — ver [12 — Seguridad y autenticación](12-seguridad-y-auth.md)). Esta ventana es **independiente** de la ventana de pago post-acuerdo (`tourya.reservations.holdMinutes`), que no cambia.

---

## Flujo propuesto

### Paso 1 — Turista solicita cotización

1. Nueva sección "Cotizar tour de hoy" (web y mobile).
2. Selecciona ciudad, subcategoría, y el desglose de viajeros por tipo (`travelerDetails`).
3. `POST /tours/quote-requests` (JWT USER) crea la `TourQuoteRequest` en `OPEN`.
4. El backend dispara **RN-Q01**: identifica tours elegibles y envía push a sus operadores.

### Paso 2 — Operadores ofertan

1. El operador recibe el push y abre la pantalla **"Cotizaciones activas"** (nueva, mobile prioritario dado el push — ver [10 — Mobile spec](10-mobile-spec.md)), con cuenta regresiva de la ventana de negociación (**RN-Q13**).
2. Elige uno de sus tours calificados y arma su oferta:
   - `POST /provider/quote-offers` (body: `quoteRequestId`, `tourId`, `tourScheduleId`, `pricingMode`, `offeredPrice` o el detalle por `ageType`) → crea `TourQuoteOffer` `PENDING`, con `clientPrice` calculado (**RN-Q04**).
   - Si tiene otro tour que también califica, puede repetir el paso (**RN-Q11**).
3. Mientras la oferta siga `PENDING`, el operador puede:
   - `PUT /provider/quote-offers/{offerId}` (editar `offeredPrice`/detalle) — **RN-Q12**.
   - `DELETE /provider/quote-offers/{offerId}` (retirar, `status → WITHDRAWN`) — **RN-Q12**.

### Paso 3 — Cliente decide sobre cada oferta

1. `GET /tours/quote-requests/{id}/offers` (JWT USER) — devuelve las ofertas visibles (**RN-Q05**).
2. Por cada oferta, el cliente puede:
   - `PUT /tours/quote-requests/{id}/offers/{offerId}/accept` → **RN-Q06** (acepta directo) → dispara **RN-Q08/RN-Q09**.
   - `PUT /tours/quote-requests/{id}/offers/{offerId}/counter` (body: `clientCounterPrice`) → **RN-Q06** (contraoferta) → `status = CLIENT_COUNTERED`, notifica al operador.

### Paso 4 — Operador responde a la contrapropuesta

1. `PUT /provider/quote-offers/{offerId}/accept-counter` → **RN-Q07/RN-Q08/RN-Q09** (gana el negocio).
2. `PUT /provider/quote-offers/{offerId}/reject-counter` → **RN-Q07**: vuelve a `status = PENDING` con el `offeredPrice` original, **no se cierra**. El cliente puede volver a los pasos del Paso 3 sobre esta misma oferta.

### Paso 5 — Pago y expiración

1. Igual que el [Flujo 5 existente](06-flujos-y-eventos.md): Wompi, confirmación, QR.
2. `TourQuoteExpiryJob` (cada 60s, junto a `TemporalReservationExpiryJob`) marca `EXPIRED` toda `TourQuoteRequest` `OPEN` vencida (según `TOUR_QUOTE_REQUEST_WINDOW_MINUTES`, **RN-Q13**) y sus ofertas `PENDING`/`CLIENT_COUNTERED` sin resolver.

---

## Cambios a flujos y entidades existentes

| Entidad/Flujo existente | Cambio necesario |
|---|---|
| `Reservation` | Agregar `originQuoteOfferId` (nullable). |
| `Tour` | `percentageTourya` ya existe — lo asigna `ADMIN` al aprobar el tour. Sin cambios necesarios, solo confirmar que este flujo lo reutiliza tal cual. |
| `app_config` | Agregar `TOUR_QUOTE_REQUEST_WINDOW_MINUTES` (default 15), mismo patrón que los flags de seguridad — ver [12](12-seguridad-y-auth.md). |
| Flujo 4 (Búsqueda y carrito) — [06](06-flujos-y-eventos.md) | Este RFQ es un camino paralelo, no reemplaza la búsqueda estándar. |
| Flujo 5 (Checkout y pago) — [06](06-flujos-y-eventos.md) | Sin cambios en el mecanismo de hold/pago; solo cambia el origen del precio. |
| Resumen de jobs scheduled — [06](06-flujos-y-eventos.md) | Agregar `TourQuoteExpiryJob` (cada 60s). |
| Matriz de permisos — [03](03-roles-y-actores.md) | Agregar fila: "Ofertar/responder cotizaciones" → solo `PROVIDER`. |
| Push notifications (FCM) — [11](11-integraciones.md) | Nuevo evento: notificar `TourQuoteRequest` calificada a los `PROVIDER` elegibles (**RN-Q01**). |
| Paneles de proveedor (web/mobile) | Nueva pantalla "Cotizaciones activas", con temporizador de 15 min y flujo de oferta/contraoferta. |
| Búsqueda del turista (web/mobile) | Nueva sección "Cotizar tour de hoy" (formulario de ciudad + subcategoría + viajeros por tipo). |

---

## Preguntas abiertas

No quedan preguntas de diseño pendientes. La última — reportería para backoffice — se resolvió aceptando la sugerencia planteada: cuando se priorice (este requerimiento completo queda en el **roadmap**, ver banner al inicio del documento), la reportería debe incluir como mínimo:

- Tasa de cierre (`TourQuoteRequest` `COMPLETED` vs. `EXPIRED`/`CANCELED`).
- Tiempo promedio desde la creación de la solicitud hasta el acuerdo (`COMPLETED`).
- Comparación entre el precio final acordado (`clientPrice` o `clientCounterPrice` de la oferta ganadora) y el precio de catálogo del mismo tour en esa fecha — para evaluar si este canal complementa o canibaliza las ventas normales.
- Desglose de motivos de cierre de ofertas no ganadoras (`closedReason`: cuántas por otra oferta seleccionada, cuántas por vencimiento de ventana, cuántas por falta de disponibilidad) — útil para detectar si el problema es de tiempo de respuesta de los operadores o de disponibilidad real.

---

## Changelog del documento

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 2026-07-29 | Versión inicial: turista propone precio guiado por histograma de mercado, individual/grupo con tamaños fijos, operador acepta o contraoferta (sin renegociación del cliente). |
| 2.0 | 2026-07-29 | Reescritura completa: se elimina el precio propuesto por el cliente y el histograma de mercado. Ahora los operadores ofertan primero (`offeredPrice`, vía push obligatorio), usando `pricingMode` (`PER_TRAVELER_TYPE` o `FLAT`) según cómo vendan el tour. El precio al cliente se calcula con `Tour.percentageTourya` (no el del slot). El cliente puede aceptar directo o contraproponer a una o varias ofertas; el operador solo acepta/rechaza la contrapropuesta; el primero en aceptar se queda con el negocio. Se mantiene el hold de pago de 15 minutos ya existente. |
| 2.1 | 2026-07-29 | Confirmado con Luis: `Tour.percentageTourya` ya existe (lo define ADMIN al aprobar el tour). Rechazo de contrapropuesta cierra la oferta definitivamente (RN-Q07). El operador puede editar/retirar su oferta mientras esté `PENDING` (RN-Q12, nuevo estado `WITHDRAWN`). Un operador puede ofertar con varios tours propios si más de uno califica (RN-Q11, con ejemplo). Se separan dos ventanas de tiempo: la ventana de negociación pasa a ser configurable por ADMIN vía `TOUR_QUOTE_REQUEST_WINDOW_MINUTES` (default 15 min, RN-Q13), independiente de la ventana de pago post-acuerdo que sigue igual (`tourya.reservations.holdMinutes`). Se agrega la fórmula inversa para la contrapropuesta del cliente (RN-Q04b): `impliedProviderAmountForCounter = clientCounterPrice / (1 + Tour.percentageTourya/100)`. Reportería backoffice queda confirmada como roadmap futuro, fuera de esta versión. |
| 2.2 | 2026-07-29 | Corrección importante: rechazar una contrapropuesta **ya no cierra la oferta** — vuelve a `PENDING` con sus valores originales, y el cliente puede seguir interactuando con ella. Se reemplaza el estado `REJECTED` por `CLOSED` + `closedReason`, con los **únicos 3 motivos válidos de cierre definitivo** (RN-Q07b): otra oferta seleccionada, vencimiento de la ventana de negociación, o pérdida de disponibilidad del proveedor. Se define reportería futura con las métricas ya recomendadas (aceptadas por Luis). Se confirma que **todo este requerimiento entra al roadmap de producto**, para una versión posterior al lanzamiento inicial — no es parte del MVP. Numeración de documento confirmada como **20** (19 queda para certificaciones). |
