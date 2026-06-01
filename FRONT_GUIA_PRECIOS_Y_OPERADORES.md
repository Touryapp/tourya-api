# Guía Front — Precios Tourya y Operadores de Proveedor

Documentación de los ajustes de API para integración en front (provider + backoffice + operador).

**Base URL:** `http://localhost:8088/api/v1` (o la de tu entorno)  
**Auth:** `Authorization: Bearer {token}` en rutas protegidas  
**Rutas públicas:** `/auth/**`, `/public/**`

---

## Variables sugeridas (Postman / front)

| Variable | Ejemplo |
|----------|---------|
| `baseUrl` | `http://localhost:8088/api/v1` |
| `tokenProvider` | JWT usuario rol `PROVIDER` |
| `tokenBackoffice` | JWT rol `ADMIN` o `BACKOFFICE_OPERATION` |
| `tokenOperator` | JWT rol `PROVIDER_OPERATOR` |
| `tokenTourist` | JWT rol `USER` |

---

## Migraciones DB (ejecutar antes de probar)

```text
database/migrations/043_add_tourya_percentage_fields.sql
database/migrations/044_provider_users_roles_and_tour_assignment.sql
```

Operadores: el proveedor define **`temporaryPassword`** al crear el usuario. En el primer login la API devuelve `mustChangePassword: true` y el operador usa **`PATCH /users`** (cambio de contraseña normal).

Migración: `database/migrations/045_add_must_change_password_to_user.sql`

---

# Parte 1 — Precios Tourya y desglose por tipo de turista

## 1.1 Conceptos para el front

| Concepto | Quién lo ve/edita | Descripción |
|----------|-------------------|-------------|
| `slotPorcentajeTourya` | Solo backoffice (GET) | **Puntos** en respuesta (ej. `15` = 15%). No va en POST/PUT/PATCH de config |
| `slotPercentageTourya` | Solo backoffice (PUT dedicados) | Mismo valor en **request** de los endpoints de % masivo/puntual |
| `providerPrice` | Proveedor | Lo que ingresa el proveedor por adulto/niño/bebé |
| `price` | Calculado por API | `providerPrice + (providerPrice × fracción del slot)` |

**Al crear slot nuevo:** el % del slot inicia en `0` hasta que backoffice lo asigne con los PUT de porcentaje.

**Por cada `ageType`:** fila `ADULT` / `CHILD` / `INFANT` con la misma fórmula.

### Reglas UI — proveedor (`PROVIDER` / `PROVIDER_OPERATOR`)

- Solo envía `providerPrice` (no `price`, no %).
- No ve `slotPorcentajeTourya` en GET.

### Reglas UI — backoffice (`ADMIN` / `BACKOFFICE_OPERATION`)

- Asigna % con `PUT /tour-schedules/tours/{tourId}/percentage` (rango de fechas) o `.../percentage/{slotId}`.
- Ve `slotPorcentajeTourya` en GET de schedules/config y en búsqueda si envía JWT backoffice.

---

## 1.2 Backoffice — asignar % Tourya en slots

### Por rango de fechas (todos los slots de schedules en el rango)

```bash
curl --location --request PUT "{{baseUrl}}/tour-schedules/tours/36/percentage" \
  --header "Authorization: Bearer {{tokenBackoffice}}" \
  --header "Content-Type: application/json" \
  --data "{\"slotPercentageTourya\":15,\"startDate\":\"2026-05-30\",\"endDate\":\"2026-06-02\"}"
```

### Un slot puntual

```bash
curl --location --request PUT "{{baseUrl}}/tour-schedules/tours/36/percentage/123" \
  --header "Authorization: Bearer {{tokenBackoffice}}" \
  --header "Content-Type: application/json" \
  --data "{\"slotPercentageTourya\":10}"
```

Respuesta: `{ "tourId", "slotsUpdated", "pricesRecalculated" }`.

**Eliminado:** `PUT /tour/admin/porcentajeTourya/{tourId}` y campo `porcentajeTourya` en tour.

---

## 1.3 Proveedor — guardar slots y precios

El proveedor **no** envía `price` ni `slotPorcentajeTourya`.

### Crear configuración de schedule

```bash
curl --location --request POST "{{baseUrl}}/tour-schedules/config?isTemplate=false" \
  --header "Authorization: Bearer {{tokenProvider}}" \
  --header "Content-Type: application/json" \
  --data "{
    \"tourId\": 1,
    \"label\": \"Horario estándar\",
    \"daysOfWeek\": [\"MONDAY\", \"TUESDAY\", \"WEDNESDAY\", \"THURSDAY\", \"FRIDAY\", \"SATURDAY\", \"SUNDAY\"],
    \"slots\": [
      {
        \"startTime\": \"09:00:00\",
        \"endTime\": \"12:00:00\",
        \"capacity\": 20,
        \"prices\": [
          { \"ageType\": \"ADULT\", \"providerPrice\": 100000 },
          { \"ageType\": \"CHILD\", \"providerPrice\": 50000 },
          { \"ageType\": \"INFANT\", \"providerPrice\": 0 }
        ]
      }
    ]
  }"
```

**Ejemplo con % tour = 0.15:**  
- Adulto: venta = 100000 × 1.15 = **115000**  
- Niño: venta = **57500**

### Actualizar configuración existente

```bash
curl --location --request PUT "{{baseUrl}}/tour-schedules/config/10" \
  --header "Authorization: Bearer {{tokenProvider}}" \
  --header "Content-Type: application/json" \
  --data "{
    \"tourId\": 1,
    \"label\": \"Horario actualizado\",
    \"daysOfWeek\": [\"MONDAY\", \"FRIDAY\"],
    \"slots\": [
      {
        \"id\": 25,
        \"startTime\": \"09:00:00\",
        \"endTime\": \"12:00:00\",
        \"capacity\": 25,
        \"prices\": [
          { \"id\": 40, \"ageType\": \"ADULT\", \"providerPrice\": 120000 },
          { \"id\": 41, \"ageType\": \"CHILD\", \"providerPrice\": 60000 }
        ]
      }
    ]
  }"
```

### Consultar config (proveedor no ve el %)

```bash
curl --location "{{baseUrl}}/tour-schedules/config/10" \
  --header "Authorization: Bearer {{tokenProvider}}"
```

Respuesta slot (proveedor): **sin** `slotPorcentajeTourya`.  
Cada precio: `price` (venta), `providerPrice`, `ageType`.

---

## 1.4 Carrito — desglose por `ageType`

### Agregar ítem al carrito

```bash
curl --location --request POST "{{baseUrl}}/shopping-cart/items" \
  --header "Authorization: Bearer {{tokenTourist}}" \
  --header "Content-Type: application/json" \
  --data "{
    \"productId\": 1,
    \"productType\": \"TOUR\",
    \"tourScheduleId\": 5,
    \"slotId\": 25,
    \"scheduleDate\": \"2026-06-15\",
    \"details\": [
      { \"ageType\": \"ADULT\", \"quantity\": 2 },
      { \"ageType\": \"CHILD\", \"quantity\": 1 }
    ]
  }"
```

### Ver carrito

```bash
curl --location "{{baseUrl}}/shopping-cart/user" \
  --header "Authorization: Bearer {{tokenTourist}}"
```

**Campos relevantes en respuesta:**

```json
{
  "totalAmount": 287500,
  "providerTotalAmount": 250000,
  "items": [
    {
      "totalPrice": 287500,
      "providerTotalPrice": 250000,
      "details": [
        {
          "ageType": "ADULT",
          "quantity": 2,
          "unitPrice": 115000,
          "providerUnitPrice": 100000,
          "totalPrice": 230000,
          "providerTotalPrice": 200000
        },
        {
          "ageType": "CHILD",
          "quantity": 1,
          "unitPrice": 57500,
          "providerUnitPrice": 50000,
          "totalPrice": 57500,
          "providerTotalPrice": 50000
        }
      ]
    }
  ]
}
```

**UI compra:** mostrar líneas por `ageType` con precio venta; opcional columna “precio proveedor” en panel interno.

---

## 1.5 Pago y reserva — desglose en respuesta

### Crear pago (confirma reservas)

```bash
curl --location --request POST "{{baseUrl}}/payment" \
  --header "Authorization: Bearer {{tokenTourist}}" \
  --header "Content-Type: application/json" \
  --data "{
    \"transactionId\": \"txn-test-001\",
    \"reservationIds\": [101, 102],
    \"payerName\": \"Cliente Test\",
    \"payerEmail\": \"cliente@test.com\",
    \"payerPhone\": \"3001234567\",
    \"payerDocumentType\": \"CC\",
    \"payerDocumentNumber\": \"123456789\"
  }"
```

En cada `reservations[]` del `PaymentResponse`:

```json
{
  "reservationId": 101,
  "price": 287500.0,
  "providerTotalAmount": 250000,
  "travellers": "2 ADULTs, 1 CHILD",
  "priceBreakdown": [
    {
      "ageType": "ADULT",
      "quantity": 2,
      "unitPrice": 115000,
      "providerUnitPrice": 100000,
      "subtotal": 230000,
      "providerSubtotal": 200000
    },
    {
      "ageType": "CHILD",
      "quantity": 1,
      "unitPrice": 57500,
      "providerUnitPrice": 50000,
      "subtotal": 57500,
      "providerSubtotal": 50000
    }
  ]
}
```

### Detalle público de booking

```bash
curl --location "{{baseUrl}}/public/bookings/101"
```

Incluye `priceBreakdown` y `providerTotalAmount` igual que arriba.

**Pagos a proveedor:** usar `providerUnitPrice` / `providerSubtotal` / `providerTotalAmount`, no el precio de venta.

---

# Parte 2 — Operadores del proveedor (contraseña temporal)

## 2.1 Roles

| Rol | Uso en front |
|-----|----------------|
| `PROVIDER` | Titular; gestiona operadores, tours, precios |
| `PROVIDER_OPERATOR` | Solo portal del proveedor: reservas + confirmar |
| `ADMIN` / `BACKOFFICE_OPERATION` | Backoffice Tourya (incl. % Tourya) |

El operador **no** usa `/auth/register`. El proveedor crea la cuenta con contraseña temporal.

---

## 2.2 Proveedor — crear operador

El proveedor define `temporaryPassword` (mín. 8 caracteres) y se la comunica al operador por el canal que prefiera.

```bash
curl --location --request POST "{{baseUrl}}/provider/users" \
  --header "Authorization: Bearer {{tokenProvider}}" \
  --header "Content-Type: application/json" \
  --data "{
    \"firstname\": \"Ana\",
    \"lastname\": \"Operadora\",
    \"email\": \"ana.operadora@ejemplo.com\",
    \"temporaryPassword\": \"ClaveTemp123\",
    \"tourIds\": [1, 2, 5],
    \"principalTourId\": 1
  }"
```

**Respuesta:**

```json
{
  "providerUserId": 3,
  "userId": 45,
  "email": "ana.operadora@ejemplo.com",
  "fullName": "Ana Operadora",
  "isPrimary": false,
  "accountEnabled": true,
  "mustChangePassword": true,
  "tours": [...]
}
```

### Listar operadores

```bash
curl --location "{{baseUrl}}/provider/users" \
  --header "Authorization: Bearer {{tokenProvider}}"
```

### Restablecer contraseña temporal

```bash
curl --location --request PUT "{{baseUrl}}/provider/users/3/reset-password" \
  --header "Authorization: Bearer {{tokenProvider}}" \
  --header "Content-Type: application/json" \
  --data "{ \"temporaryPassword\": \"OtraClaveTemp99\" }"
```

---

## 2.3 Operador — login y cambio de contraseña

### Login (`POST /auth/authenticate`)

```bash
curl --location --request POST "{{baseUrl}}/auth/authenticate" \
  --header "Content-Type: application/json" \
  --data "{
    \"email\": \"ana.operadora@ejemplo.com\",
    \"password\": \"ClaveTemp123\"
  }"
```

Si es primer acceso (o tras reset del proveedor), la respuesta incluye:

```json
{
  "token": "...",
  "mustChangePassword": true,
  "roleList": [{ "name": "PROVIDER_OPERATOR" }]
}
```

**UI:** si `mustChangePassword === true`, mostrar pantalla de cambio de contraseña antes del resto del portal (puede usar el mismo componente que otros usuarios).

### Cambiar contraseña (`PATCH /users`) — flujo normal

Requiere JWT del login anterior.

```bash
curl --location --request PATCH "{{baseUrl}}/users" \
  --header "Authorization: Bearer {{tokenOperator}}" \
  --header "Content-Type: application/json" \
  --data "{
    \"currentPassword\": \"ClaveTemp123\",
    \"newPassword\": \"MiClaveSegura123\",
    \"confirmationPassword\": \"MiClaveSegura123\"
  }"
```

Tras éxito, `mustChangePassword` queda en `false` en el siguiente login.

---

## 2.4 Proveedor — actualizar operador y tour principal

### Actualizar nombre, tours y tour principal

```bash
curl --location --request PUT "{{baseUrl}}/provider/users/3" \
  --header "Authorization: Bearer {{tokenProvider}}" \
  --header "Content-Type: application/json" \
  --data "{
    \"firstname\": \"Ana María\",
    \"lastname\": \"Gómez\",
    \"tourIds\": [1, 3, 5],
    \"principalTourId\": 3
  }"
```

- Si envías `tourIds`, **reemplaza** todas las asignaciones.
- `principalTourId` debe estar en `tourIds` cuando envías la lista.

### Solo cambiar tour principal (sin cambiar lista de tours)

```bash
curl --location --request PUT "{{baseUrl}}/provider/users/3/principal-tour?tourId=5" \
  --header "Authorization: Bearer {{tokenProvider}}"
```

El operador debe estar ya asignado al tour `5`.

---

## 2.5 Operador — reservas del proveedor

Mismo endpoint que el proveedor titular; el backend filtra por `provider_id` del usuario logueado.

```bash
curl --location "{{baseUrl}}/reservations?page=0&size=10&status=PENDING" \
  --header "Authorization: Bearer {{tokenOperator}}"
```

Roles permitidos: `PROVIDER`, `PROVIDER_OPERATOR`.  
Backoffice puede pasar `providerId` opcional.

**UI operador:** solo listado de reservas + acción confirmar (según permisos existentes del flujo de delivery).

---

# Resumen rápido para pantallas

| Pantalla | Endpoints clave | Notas |
|----------|-----------------|-------|
| Backoffice % tour | `PUT /tour/admin/porcentajeTourya/{id}` | Decimal 0.15 = 15% |
| Provider precios slot | `POST/PUT /tour-schedules/config` | Solo `providerPrice` por `ageType` |
| Checkout / carrito | `GET /shopping-cart/user` | `items[].details[]` por tipo |
| Confirmación compra | `POST /payment` | `priceBreakdown` en cada reserva |
| Crear operador | `POST /provider/users` | Con `temporaryPassword` |
| Operador cambiar clave | `PATCH /users` | Tras login si `mustChangePassword` |
| Reset clave temporal | `PUT /provider/users/{id}/reset-password` | Solo proveedor titular |
| Gestionar operadores | `GET/PUT /provider/users` | `principal-tour`, `tours[].isPrincipal` |

---

# Errores frecuentes

| Mensaje | Causa |
|---------|--------|
| `providerPrice is required for ageType` | Proveedor no envió `providerPrice` |
| `Email already registered` | Email ya existe al invitar |
| `Wrong password` | Clave actual incorrecta en PATCH /users |
| `principalTourId must be included in tourIds` | Tour principal no está en la lista al actualizar |

---

*Generado para integración front — Tourya API.*
