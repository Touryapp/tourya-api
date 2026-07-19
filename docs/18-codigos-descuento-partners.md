# 18 — Códigos de descuento para partners B2B

> Propuesta funcional y técnica. Sigue el formato de los docs 01–17. Numeración de reglas de negocio (`RN-Bxx`) es referencial — ajustar al correlativo real del backlog.

---

## Resumen

Permitir que **partners B2B** (hoteles, aliados de alto tráfico — el mismo tipo de actor mencionado en el roadmap de convenios B2B de [01 — Visión y negocio](01-vision-y-negocio.md)) tengan un **código propio** con un **monto fijo asociado** (ej. $5.000 COP) que, al usarse en una reserva, beneficia de una de dos formas — **configurable por código**:

- **`CUSTOMER_DISCOUNT`**: el monto se descuenta del precio que paga el turista.
- **`PARTNER_COMMISSION`**: el precio del turista no cambia; el monto se **acumula a favor del partner**, quien lo puede cobrar en un pago mensual (a fin de mes), igual que hoy el operador recibe sus payouts.

**Decisión de negocio (confirmada)**:
- En ambos modos, el costo lo **asume Tourya**, reduciendo su comisión (`slotPercentageTourya`) en esa venta — nunca se toca. El `providerPrice` del operador y su payout **nunca cambian**.
- En v1, los partners **no tienen login ni portal propio**; eso queda en el **roadmap**. Los códigos los crea y administra el equipo de **backoffice/ADMIN**, igual que hoy se gestiona la comisión Tourya. La distribución del código es física/manual por el partner (QR en el counter del hotel, tarjeta impresa, WhatsApp, etc.).
- **Vigencia por defecto**: si al crear el código no se especifica `validUntil`, se asume **1 año** desde la creación.

---

## Actores involucrados

| Actor | Rol en esta funcionalidad |
|-------|---------------------------|
| **Partner** (hotel, aliado) | Entidad de negocio nueva. No es un actor humano con login en v1 — es un registro administrativo. 📌 **Roadmap**: portal self-service para que el partner vea sus propios códigos, usos y comisión acumulada sin pasar por backoffice. **No entra en esta v1.** |
| **ADMIN / BACKOFFICE_OPERATION** | Crea el `Partner`, crea y gestiona sus `DiscountCode`s, ve reportes de uso/costo, y marca como pagados los `PartnerPayoutOrder` (ver más abajo). Mismo nivel de permiso que hoy tienen sobre `slotPercentageTourya` (ver [03 — Roles y actores](03-roles-y-actores.md)). |
| **Turista (USER)** | Ingresa el código en el carrito/checkout. No ve quién es el partner necesariamente, y en el modo `PARTNER_COMMISSION` tampoco ve ningún cambio en su precio. |
| **Proveedor (PROVIDER)** | No interactúa con esta funcionalidad. `providerPrice` y payout no cambian en ningún modo. |

---

## Entidades nuevas

### `Partner`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `name` | String | Ej. "Hotel Sunset San Andrés" |
| `type` | Enum | `HOTEL`, `BUSINESS`, `OTHER` |
| `contactName`, `contactEmail`, `contactPhone` | String | Datos de contacto comercial |
| `bankAccountInfo` | String/JSON | Necesario solo si el partner tiene algún código en modo `PARTNER_COMMISSION` — para poder pagarle a fin de mes |
| `status` | Enum | `ACTIVE`, `INACTIVE` |
| `createdAt`, `createdBy` | — | Auditoría |

### `DiscountCode`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `code` | String, único | Alfanumérico, normalizado a mayúsculas (ej. `SUNSET5K`) |
| `partnerId` | FK → `Partner` | |
| `beneficiaryType` | Enum | **`CUSTOMER_DISCOUNT`** (el monto se descuenta al turista) o **`PARTNER_COMMISSION`** (el monto se acumula a favor del partner, pagadero a fin de mes). Se define al crear el código — ver **RN-B01**. |
| `discountAmount` | BigDecimal | Monto fijo en COP (ej. `5000`). **No es porcentaje.** Su efecto depende de `beneficiaryType`. |
| `status` | Enum | `ACTIVE`, `PAUSED`, `EXPIRED`, `EXHAUSTED` |
| `validFrom`, `validUntil` | DateTime | Vigencia. Si no se especifica `validUntil` al crear, se asume **`validFrom` + 1 año** (ver **RN-B10**). |
| `maxTotalUses` | Integer, nullable | `null` = ilimitado |
| `maxUsesPerUser` | Integer, default `1` | Evita abuso por el mismo turista |
| `minPurchaseAmount` | BigDecimal, nullable | Monto mínimo de la reserva para aplicar el código |
| `applicableTourIds` | Long[], nullable | `null` = aplica a todos los tours |
| `applicableCategoryIds` | Long[], nullable | Alternativa a restringir por tour específico |
| `applicationScope` | Enum | `PER_RESERVATION` (se aplica a cada reserva del carrito) o `PER_CART` (una sola vez por checkout). Se define al crear el código — ver **RN-B04**. |
| `maxReservationsPerRedemption` | Integer, nullable | Solo aplica si `applicationScope = PER_RESERVATION`; tope de reservas del mismo carrito a las que se les aplica el descuento |
| `createdAt`, `createdBy` | — | Auditoría |

### `DiscountCodeRedemption`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `discountCodeId` | FK → `DiscountCode` | |
| `userId` | FK → `User` (turista) | |
| `reservationId` | FK → `Reservation` | Una fila por cada reserva descontada |
| `amountApplied` | BigDecimal | Normalmente = `discountAmount`, salvo que se haya capado (ver RN-B03) |
| `redeemedAt` | DateTime | |

> Esta tabla es la que le permite a backoffice responder, en cualquier momento: *"¿cuánto le ha costado a Tourya el convenio con el Hotel Sunset este mes?"* — insumo clave para renegociar el acuerdo B2B, y además es el insumo directo del payout mensual en modo `PARTNER_COMMISSION` (ver abajo).

### `PartnerPayoutOrder` (solo aplica a redenciones en modo `PARTNER_COMMISSION`)

Análogo a `ProviderPayoutOrder` ([04 — Entidades de dominio](04-entidades-dominio.md)), pero con corte **mensual** en vez de martes/viernes.

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `partnerId` | FK → `Partner` | |
| `payDate` | Date | Corte + 5 días (ver **RN-B09**) |
| `status` | Enum | `PENDING`, `PAID` |
| `amountTotal` | BigDecimal | Suma de `amountApplied` de las redenciones `PARTNER_COMMISSION` incluidas |
| `createdAt` | DateTime | |

### `PartnerPayoutOrderRedemption`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `partnerPayoutOrderId` | FK → `PartnerPayoutOrder` | |
| `discountCodeRedemptionId` | FK → `DiscountCodeRedemption` | Vincula cada uso individual a la orden de pago que lo cubrió |

### `PartnerPayoutAttachment`

Igual patrón que `ProviderPayoutAttachment`: comprobante subido por backoffice al marcar la orden como pagada.

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `partnerPayoutOrderId` | FK → `PartnerPayoutOrder` | |
| `fileUrl` | String | URL en GCS/S3 |
| `uploadedAt` | DateTime | |

---

## Reglas de negocio

- **RN-B01 — Fondeo y modo de beneficio (confirmado)**: cada `DiscountCode` define un `beneficiaryType` al crearse:
  - **`CUSTOMER_DISCOUNT`**: el `discountAmount` se resta del `price` final que paga el turista. El costo se contabiliza como reducción de la comisión Tourya de esa venta.
  - **`PARTNER_COMMISSION`**: el turista paga el `price` completo, sin cambios. El `discountAmount` se registra como monto **a favor del partner**, acumulado para su pago mensual (ver **RN-B09**). El costo también se contabiliza como reducción de la comisión Tourya de esa venta — solo cambia *quién* recibe el monto (el turista como descuento, o el partner como comisión), no de dónde sale.
  
  En ambos modos, el `providerPrice` y el payout al proveedor **nunca se ven afectados**.

- **RN-B02 — Validación al aplicar el código**: el sistema valida, en este orden, que el código: (1) exista y esté `ACTIVE`, (2) esté dentro de `validFrom`/`validUntil`, (3) no haya superado `maxTotalUses`, (4) el usuario no haya superado `maxUsesPerUser`, (5) la reserva cumpla `minPurchaseAmount` si aplica, (6) el tour/categoría esté dentro de `applicableTourIds`/`applicableCategoryIds` si están definidos.

- **RN-B03 — Tope al monto de comisión disponible (confirmado)**: si `discountAmount` es mayor que la comisión Tourya de esa reserva (`price - providerPrice`), el monto efectivo (`amountApplied`) se capa a ese monto de comisión — aplica igual en ambos `beneficiaryType`. **Tourya nunca asume margen negativo en una transacción individual**: el monto máximo posible en una reserva es exactamente lo que Tourya iba a retener por comisión en esa venta.

- **RN-B04 — Alcance configurable por código (confirmado)**: `applicationScope` **no es fijo** — cada `DiscountCode` define, al crearse, si es:
  - `PER_RESERVATION`: se aplica a cada reserva del carrito que cumpla las validaciones (con tope opcional vía `maxReservationsPerRedemption`), o
  - `PER_CART`: se aplica **una sola vez** por checkout, sin importar cuántas reservas tenga el carrito (el `discountAmount` se resta una vez del total, priorizando la reserva con mayor comisión disponible para maximizar el monto que RN-B03 permite cubrir).

  Esto le da al equipo comercial flexibilidad para negociar distinto con cada partner (ej. un código "por reserva" para incentivar reservar varios tours, o "por checkout" para un descuento de bienvenida único).

- **RN-B05 — Compatibilidad con créditos (confirmado)**: aplica cuando `beneficiaryType = CUSTOMER_DISCOUNT`. Un código de descuento y los créditos del turista **sí se pueden combinar** en el mismo pago. Orden de aplicación: (1) se resta el `discountAmount` (topado por RN-B03) del `price` de catálogo, (2) sobre ese nuevo total, el turista puede cubrir con créditos + Wompi, igual que en el flujo normal de pago ([06 — Flujo 5](06-flujos-y-eventos.md)). No aplica en `PARTNER_COMMISSION` porque ahí el precio del turista no cambia.

- **RN-B06 — Un código por reserva**: no se permite aplicar más de un `DiscountCode` a la misma reserva.

- **RN-B07 — Cancelaciones**: si se cancela una reserva que usó un código:
  - En `CUSTOMER_DISCOUNT`: el crédito generado (ver [06 — Flujo 6](06-flujos-y-eventos.md)) se calcula sobre el `totalAmount` **ya con el descuento aplicado** — el turista no recupera más de lo que efectivamente pagó.
  - En `PARTNER_COMMISSION`: la `DiscountCodeRedemption` correspondiente se marca `VOIDED` y se excluye del siguiente `PartnerPayoutOrder` — el partner no cobra comisión sobre una reserva que no se concretó. Si ya estaba incluida en una orden `PAID`, se descuenta del siguiente corte mensual.

- **RN-B08 — Gestión exclusiva de backoffice**: solo `ADMIN` y `BACKOFFICE_OPERATION` pueden crear/editar/pausar `Partner`s y `DiscountCode`s — mismo nivel de permiso que hoy tienen sobre `slotPercentageTourya`.

- **RN-B09 — Payout mensual al partner (confirmado)**: para códigos `PARTNER_COMMISSION`, un job (`PartnerPayoutOrderJob`) hace **corte el último día calendario de cada mes**, agrupando todas las `DiscountCodeRedemption` no facturadas aún de ese partner en un `PartnerPayoutOrder` con `status = PENDING`. **El pago se realiza 5 días después de la fecha de corte.** No hay monto mínimo: se paga el total generado, sin importar qué tan pequeño sea. Backoffice sube el comprobante de transferencia y marca la orden `PAID`, igual que hoy con `ProviderPayoutOrder` (ver [06 — Flujo 7](06-flujos-y-eventos.md)).

- **RN-B10 — Vigencia por defecto**: si al crear un `DiscountCode` no se especifica `validUntil`, el sistema asigna automáticamente `validFrom + 1 año`.

- **RN-B11 — Un partner puede combinar modos (confirmado)**: un mismo `Partner` puede tener, al mismo tiempo, códigos `CUSTOMER_DISCOUNT`, códigos `PARTNER_COMMISSION`, o ambos — no hay exclusividad a nivel de partner. El modo se define por código individual (`DiscountCode.beneficiaryType`), no por partner.

---

## Flujo propuesto

### Alta de partner y código (backoffice)

1. `POST /admin/partners` — crea el `Partner` (nombre, tipo, contacto).
2. `POST /admin/partners/{partnerId}/discount-codes` — crea el `DiscountCode` con monto, vigencia, topes y restricciones.
3. Backoffice comparte el código con el partner por fuera de la plataforma (el partner lo entrega a sus huéspedes: QR en el counter, tarjeta, WhatsApp, etc.).

### Aplicación en checkout (turista)

1. En `CartPage`/`CheckoutPage` (web y mobile), se agrega un campo **"¿Tienes un código?"**.
2. `POST /shopping-cart/apply-discount-code` — body `{ code }`. Corre las validaciones de **RN-B02**.
3. Si es válido, el backend determina el `beneficiaryType` del código:
   - `CUSTOMER_DISCOUNT`: recalcula el total del carrito aplicando **RN-B03/B04** y devuelve el nuevo `totalAmount` desglosado (subtotal, descuento, total a pagar).
   - `PARTNER_COMMISSION`: el `totalAmount` **no cambia**; el backend solo confirma que el código quedó asociado al carrito para generar la comisión al partner en el pago.
4. El flujo de checkout continúa igual que en [06 — Flujo 5](06-flujos-y-eventos.md): se crean los `Reservation` en `TEMPORAL`, se abre Wompi, se confirma el pago.
5. Al confirmar el pago, se crea una fila en `DiscountCodeRedemption` por cada reserva cubierta, y se incrementa `usesCount` del código — independientemente del modo.

### Payout mensual al partner (solo códigos `PARTNER_COMMISSION`)

1. `PartnerPayoutOrderJob` corre el último día de cada mes (ver **RN-B09**).
2. Agrupa por partner todas las `DiscountCodeRedemption` de tipo `PARTNER_COMMISSION` no incluidas aún en una orden, y crea un `PartnerPayoutOrder` en `PENDING`.
3. Backoffice hace la transferencia manual (mismo proceso que con proveedores), sube el comprobante (`PartnerPayoutAttachment`) y marca la orden `PAID`.

### Reportería (backoffice)

- Nueva vista: **"Partners y códigos"** — listado de partners, códigos activos (con su `beneficiaryType`), usos acumulados, **costo total para Tourya** por partner/periodo, y para los de tipo `PARTNER_COMMISSION`, el estado de sus `PartnerPayoutOrder`s. Esto alimenta directamente el dashboard financiero que ya está pendiente en el roadmap ([01 — Visión y negocio](01-vision-y-negocio.md), sección "Mejora del backoffice").

---

## Cambios a flujos y entidades existentes

| Entidad/Flujo existente | Cambio necesario |
|---|---|
| `ShoppingCart` / `ShoppingCartItem` | Agregar campo `appliedDiscountCodeId` (nullable) para persistir el código durante el carrito, antes de convertirse en reserva. |
| `Reservation` | Agregar campo `discountAmountApplied` (default 0) para trazabilidad del monto exacto descontado en esa reserva específica. |
| `Payment` | El `amount` confirmado ya incluye el descuento; no requiere cambio de estructura, solo que el cálculo previo lo contemple. |
| Flujo 5 (Checkout y pago) — [06](06-flujos-y-eventos.md) | Insertar el paso de aplicación/validación de código antes de "Crear hold temporal". |
| Flujo 6 (Cancelación) — [06](06-flujos-y-eventos.md) | El cálculo de `refundAmount` debe partir del `totalAmount` ya neto de descuento (no del `price` de catálogo). |
| Matriz de permisos — [03](03-roles-y-actores.md) | Agregar fila: "Crear/gestionar partners y códigos" → solo `BACKOFFICE_OPERATION` y `ADMIN`. |
| Resumen de jobs scheduled — [06](06-flujos-y-eventos.md) | Agregar `PartnerPayoutOrderJob` (cron mensual, último día del mes) a la tabla de jobs, junto a `ProviderPayoutOrderJob`. |

---

## Preguntas abiertas

Todas las preguntas de diseño quedaron resueltas con Luis:

- Alcance por reserva/checkout, combinación con créditos, tope de comisión → **RN-B03, RN-B04, RN-B05**.
- Portal self-service para partners → queda en el **roadmap**, no en v1.
- Vigencia por defecto → **1 año** (RN-B10).
- Convivencia con revenue share → resuelta con `beneficiaryType` (`CUSTOMER_DISCOUNT` / `PARTNER_COMMISSION`), ver **RN-B01**.
- Día de corte, fecha de pago y monto mínimo del payout mensual → **RN-B09**: corte último día del mes, pago 5 días después, sin monto mínimo.
- Exclusividad de modo por partner → **RN-B11**: un partner puede tener códigos de ambos tipos a la vez.

No quedan puntos pendientes de definición funcional. Los siguientes serían pasos de implementación (fuera del alcance de este documento): definir el endpoint exacto y el diseño de UI en backoffice para el módulo "Partners y códigos".

---

## Changelog del documento

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 2026-07-19 | Versión inicial. Propuesta de códigos de descuento fijo para partners B2B, financiados por Tourya vía reducción de comisión, gestión exclusiva desde backoffice. |
| 1.1 | 2026-07-19 | Confirmado con Luis: (1) el alcance `PER_RESERVATION`/`PER_CART` es configurable por código, no fijo; (2) el código sí se puede combinar con créditos del turista; (3) el descuento nunca puede superar la comisión Tourya disponible en la venta (RN-B03). |
| 1.2 | 2026-07-19 | Confirmado con Luis: portal self-service para partners queda en el roadmap (v1 = gestión manual desde backoffice); vigencia por defecto de 1 año (RN-B10). Se resuelve la convivencia con revenue share: se agrega `beneficiaryType` (`CUSTOMER_DISCOUNT` / `PARTNER_COMMISSION`) a `DiscountCode`, y se agregan las entidades `PartnerPayoutOrder`, `PartnerPayoutOrderRedemption` y `PartnerPayoutAttachment` para el pago mensual al partner en modo `PARTNER_COMMISSION` (RN-B09). |
| 1.3 | 2026-07-19 | Confirmado con Luis: corte de payout al partner el último día del mes, pago 5 días después (RN-B09); sin monto mínimo para generar la orden; un mismo partner puede combinar códigos `CUSTOMER_DISCOUNT` y `PARTNER_COMMISSION` (RN-B11). Documento sin preguntas de diseño pendientes. |
