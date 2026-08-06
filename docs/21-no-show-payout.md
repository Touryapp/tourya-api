# 21 — Penalización por "No Show" e inclusión en el payout al proveedor

> Propuesta funcional y técnica. Sigue el formato de los docs 01–18, 19 y 20. Numeración confirmada: este documento es el **21**. Numeración de reglas de negocio (`RN-Nxx`) es referencial.

---

## Resumen

Hoy, cuando un turista **no se presenta** el día del tour, el job `PendingReservationNoShowJob` (ya funciona, **no se toca**) pasa la reserva de `PENDING`/`RESCHEDULING` a `NO_SHOW` si la `scheduleDate` ya pasó. Lo que falta es la **consecuencia comercial** de ese estado:

1. El operador define, **por tour**, un **`porcentajeNoShow`** (0–100) — qué porcentaje de su `providerPrice` cobra igual, aunque el turista no se haya presentado.
2. Cada reserva que cae en `NO_SHOW` calcula y guarda un **`noShowPenalty`** = `providerPrice × (porcentajeNoShow / 100)`.
3. El job de payout (`ProviderPayoutOrderJob`) debe incluir, además de las reservas `COMPLETED` (como ya hace), las reservas en `NO_SHOW` — tomando `providerPrice` para las primeras y `noShowPenalty` para las segundas.
4. En el panel de ADMIN, cada orden de pago debe mostrar **nombre y documento del proveedor**.

---

## Decisiones de negocio (confirmadas)

- **`porcentajeNoShow` vive a nivel `Tour`** (confirmado) — cada tour define su propio porcentaje, mismo criterio que la política de cancelación (`TourCancellationPolicy`). El operador lo define al crear/editar su tour.
- Acepta valores entre **0 y 100** (porcentaje).
- `NoShowPenalty = providerPrice × (porcentajeNoShow / 100)` — ejemplo: `providerPrice = $100.000`, `porcentajeNoShow = 50` → `NoShowPenalty = $50.000`.
- El job `PendingReservationNoShowJob` **no cambia** — solo se le agrega, en el mismo momento en que transiciona una reserva a `NO_SHOW`, el cálculo y guardado de `NoShowPenalty`.
- El job `ProviderPayoutOrderJob` debe incluir reservas `COMPLETED` **y** `NO_SHOW` en la misma orden de pago — usando `providerPrice` o `NoShowPenalty` según corresponda.
- **La comisión de Tourya no cambia con el `NO_SHOW` (confirmado)**: sigue siendo `shoppingTotalPrice - providerPrice`, igual que en cualquier reserva `COMPLETED`. Lo que sí cambia es que el operador solo recibe `NoShowPenalty` en vez de `providerPrice` completo — y **la diferencia (`providerPrice - NoShowPenalty`) se devuelve al turista como crédito**, no la retiene Tourya. Ver **RN-N07**.
- El proveedor se notifica **por correo** cuando una de sus reservas pasa a `NO_SHOW` — ver **RN-N08**.
- El panel de ADMIN debe mostrar, en cada orden de pago, el **nombre y documento** del `Provider`.

---

## Actores involucrados

| Actor | Rol en esta funcionalidad |
|-------|---------------------------|
| **PROVIDER** | Define `porcentajeNoShow` al crear/editar su tour. Recibe el payout correspondiente (completo o penalizado) según el resultado de cada reserva. |
| **Sistema (jobs)** | `PendingReservationNoShowJob` marca reservas como `NO_SHOW` (sin cambios) y ahora también calcula `NoShowPenalty`. `ProviderPayoutOrderJob` arma las órdenes de pago incluyendo ambos tipos de reserva. |
| **ADMIN** | Ve las órdenes de pago con nombre y documento del proveedor, sube comprobantes y marca como pagadas (flujo ya existente, sin cambios). |
| **Turista (USER)** | No participa directamente — no hay reembolso ni crédito asociado al `NO_SHOW` en el alcance de este documento (ver preguntas abiertas). |

---

## Entidades — extensiones a lo existente

### Extensión a `Tour`

| Campo nuevo | Tipo | Notas |
|-------------|------|-------|
| `porcentajeNoShow` | Integer/BigDecimal, `0`–`100`, nullable | Definido por el `PROVIDER` al crear/editar el tour. Si es `null` o `0`, no hay penalización — el `NO_SHOW` de ese tour no genera pago al proveedor (comportamiento equivalente al actual, antes de esta funcionalidad). Validación de rango (`@Min(0) @Max(100)`), mismo mecanismo que el resto de validaciones de DTO (ver [12 — Seguridad y autenticación](12-seguridad-y-auth.md)). |

### Extensión a `Reservation`

| Campo nuevo | Tipo | Notas |
|-------------|------|-------|
| `providerPrice` | BigDecimal | **Confirmado: ya existe** en el modelo de `Reservation` — el monto total que le corresponde al proveedor por esa reserva específica. Este flujo lo reutiliza tal cual, sin cambios. |
| `noShowPenalty` | BigDecimal, nullable | Calculado y persistido **en el momento en que `PendingReservationNoShowJob` transiciona la reserva a `NO_SHOW`** (no antes, no después) — ver **RN-N02**. |

### Extensión a `AccountPayable`

| Campo | Cambio |
|-------|--------|
| `amount` | Sin cambio de tipo, pero ahora su origen depende del estado de la reserva que lo generó — ver **RN-N03**. |
| `sourceReservationStatus` | Enum `COMPLETED` / `NO_SHOW` — **incluido en esta versión (confirmado)**. Permite a ADMIN/BACKOFFICE_OPERATION distinguir, en reportes, cuánto del payout vino de tours efectivamente realizados vs. de penalizaciones por no-show. |

---

## Reglas de negocio

- **RN-N01 — Definición del porcentaje (confirmado)**: `Tour.porcentajeNoShow` lo define el `PROVIDER` al crear/editar su tour, con rango `0`–`100`. Es opcional — un tour sin este valor definido no genera `NoShowPenalty` (equivale a `0%`).

- **RN-N02 — Cálculo y persistencia de `NoShowPenalty`**: en el mismo momento en que `PendingReservationNoShowJob` (sin cambios en su lógica de selección, ver Resumen) transiciona una `Reservation` de `PENDING`/`RESCHEDULING` a `NO_SHOW`, el sistema calcula:

  ```
  NoShowPenalty = Reservation.providerPrice × (Tour.porcentajeNoShow / 100)
  ```

  y lo persiste en `Reservation.noShowPenalty`. Se calcula **una sola vez, en ese momento** — usa el `porcentajeNoShow` vigente en ese instante; si el operador lo cambia después, no afecta reservas ya marcadas `NO_SHOW` (mismo criterio de "congelar" que ya usa el sistema para precios de reservas confirmadas).

- **RN-N03 — Inclusión en `AccountPayable`**: cuando una reserva pasa a `COMPLETED` **o** a `NO_SHOW`, se crea (o actualiza) su `AccountPayable` correspondiente, con:

  ```
  amount = Reservation.providerPrice        si status = COMPLETED
  amount = Reservation.noShowPenalty         si status = NO_SHOW
  ```

  El resto de la lógica de `AccountPayable` (buffer de `paymentAvailableDate`, etc.) **no cambia** — aplica igual para ambos casos.

- **RN-N04 — `ProviderPayoutOrderJob` incluye ambos estados (confirmado)**: la query que arma cada `ProviderPayoutOrder` (ver [06 — Flujo 7](06-flujos-y-eventos.md)) se amplía para incluir `AccountPayable`s cuya reserva de origen esté en `COMPLETED` **o** `NO_SHOW` — antes solo consideraba `COMPLETED`. El `amountTotal` de la orden es la suma de ambos tipos, sin distinción en el total, pero **sí distinguible por reserva individual** dentro del detalle (`ProviderPayoutOrderReservation`), útil para que el proveedor entienda su comprobante.

- **RN-N05 — Sin cambios al job de detección de No Show**: `PendingReservationNoShowJob` sigue exactamente igual — mismo criterio de selección (`scheduleDate < hoy` sobre `PENDING`/`RESCHEDULING`). Esta funcionalidad solo agrega el cálculo de `NoShowPenalty` como una consecuencia adicional de esa transición, no cambia cuáles reservas se marcan `NO_SHOW`.

- **RN-N06 — Visibilidad de proveedor en órdenes de pago (confirmado)**: la respuesta de `ProviderPayoutOrder` para `ADMIN` incluye `providerName` y `providerDocumentNumber` (con su `providerDocumentType`, ej. NIT/Cédula) — datos ya existentes en `Provider`/`RequestProvider`, solo se exponen en este endpoint/pantalla.

- **RN-N07 — Crédito al turista por la diferencia (confirmado)**: la comisión de Tourya **no cambia** con el `NO_SHOW` — sigue siendo `Reservation.shoppingTotalPrice - Reservation.providerPrice`, igual que en cualquier reserva `COMPLETED`. Como el operador solo recibe `NoShowPenalty` (no el `providerPrice` completo), la diferencia se devuelve al turista como crédito:

  ```
  creditAmount = Reservation.providerPrice - Reservation.noShowPenalty
  ```

  Este crédito se genera **en el mismo momento** en que se calcula `NoShowPenalty` (**RN-N02**), reutilizando la entidad `Credit` ya existente (mismo patrón que la cancelación — ver [06 — Flujo 6](06-flujos-y-eventos.md)): `userId` = turista de la reserva, `amount = creditAmount`, `status = CREATED`, `expirationDate = now + 1 año`.

  **Verificación con el ejemplo**: `shoppingTotalPrice = $120.000`, `providerPrice = $100.000` (comisión Tourya = $20.000, fija), `porcentajeNoShow = 50%` → `noShowPenalty = $50.000`. El operador recibe $50.000, Tourya mantiene sus $20.000 de siempre, y el turista recibe $50.000 de crédito. Suma: `$50.000 + $20.000 + $50.000 = $120.000` ✔ — cuadra con lo que el turista pagó originalmente.

- **RN-N08 — Notificación al proveedor por correo (confirmado)**: cuando una reserva del proveedor pasa a `NO_SHOW`, se dispara un email (vía `@Async`, mismo patrón que el resto de notificaciones — ver [06 — Resumen de eventos async](06-flujos-y-eventos.md) y [11 — Integraciones, SMTP](11-integraciones.md)) informándole del cambio de estado y el monto (`NoShowPenalty`) que recibirá por esa reserva en su próximo payout.

---

## Flujo propuesto

### Detección de No Show y cálculo de la penalización (extiende lo existente)

1. `PendingReservationNoShowJob` corre igual que hoy — sin cambios de cron ni de criterio.
2. Por cada reserva que transiciona a `NO_SHOW`:
   - Calcula y persiste `noShowPenalty` (**RN-N02**).
   - Crea/actualiza su `AccountPayable` con `amount = noShowPenalty` y `sourceReservationStatus = NO_SHOW` (**RN-N03**).
   - Genera el `Credit` a favor del turista por la diferencia (**RN-N07**).
   - Dispara el email al proveedor (**RN-N08**).

### Payout al proveedor (extiende Flujo 7 existente)

1. `ProviderPayoutOrderJob` corre igual que hoy (`0 0 7 ? * MON,THU`).
2. La query a `AccountPayable` ahora trae reservas `COMPLETED` **y** `NO_SHOW` (**RN-N04**).
3. Arma `ProviderPayoutOrder` + `ProviderPayoutOrderReservation` igual que hoy, con el monto correspondiente ya resuelto en `AccountPayable.amount`.
4. Backoffice/ADMIN sigue subiendo el comprobante y marcando `PAID` — sin cambios.

### Panel de ADMIN — órdenes de pago

1. La pantalla de órdenes de pago agrega columnas/encabezado: **nombre del proveedor** y **documento del proveedor** (**RN-N06**), por cada `ProviderPayoutOrder`.

---

## Cambios a flujos y entidades existentes

| Entidad/Flujo existente | Cambio necesario |
|---|---|
| `Tour` | Agregar `porcentajeNoShow` (nullable, 0–100). |
| `Reservation` | Agregar `noShowPenalty` (nullable, calculado). `providerPrice` ya existe, sin cambios. |
| `Credit` | Sin cambios de estructura — se reutiliza tal cual, con un nuevo origen (`NO_SHOW`) además de cancelación. |
| `AccountPayable` | Su `amount` ahora depende del estado de origen (`COMPLETED`/`NO_SHOW`); se agrega `sourceReservationStatus`. |
| Resumen de eventos async — [06](06-flujos-y-eventos.md) | Agregar `sendEmail(no-show)` disparado por la transición a `NO_SHOW`. |
| Flujo 7 (Payout al proveedor) — [06](06-flujos-y-eventos.md) | La query de `ProviderPayoutOrderJob` se amplía para incluir `NO_SHOW` además de `COMPLETED`. |
| Panel de ADMIN — órdenes de pago | Mostrar nombre y documento del proveedor por cada orden. |
| `TourFormPage` / wizard web — [10](10-mobile-spec.md) | Agregar campo `porcentajeNoShow` en el paso de información básica o junto a la política de cancelación, con validación 0–100. |

---

## Preguntas abiertas

No quedan preguntas de diseño pendientes — las 4 se resolvieron con Luis:

- **`Reservation.providerPrice`** → confirmado, ya existe en el modelo, se reutiliza tal cual.
- **La diferencia `providerPrice - NoShowPenalty`** → confirmado, se devuelve al turista como `Credit`; la comisión de Tourya no cambia (**RN-N07**).
- **`sourceReservationStatus` en `AccountPayable`** → confirmado, incluido en esta versión.
- **Notificación al proveedor** → confirmado, por correo (**RN-N08**).

---

## Changelog del documento

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 2026-07-29 | Versión inicial. `Tour.porcentajeNoShow` (definido por el operador, 0–100), cálculo y persistencia de `Reservation.noShowPenalty` al momento de la transición a `NO_SHOW` (sin tocar `PendingReservationNoShowJob`), inclusión de reservas `NO_SHOW` en `ProviderPayoutOrderJob` junto a `COMPLETED`, y visibilidad de nombre/documento del proveedor en el panel de ADMIN para órdenes de pago. |
| 1.1 | 2026-07-29 | Confirmado con Luis: `Reservation.providerPrice` ya existe (se reutiliza sin cambios). Se agrega **RN-N07**: la comisión de Tourya no cambia con el `NO_SHOW`; la diferencia entre `providerPrice` y `noShowPenalty` se devuelve al turista como `Credit` (mismo mecanismo que cancelaciones). Se confirma `sourceReservationStatus` en `AccountPayable` para esta versión. Se agrega **RN-N08**: notificación por correo al proveedor cuando una reserva suya pasa a `NO_SHOW`. |
