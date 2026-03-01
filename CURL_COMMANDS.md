# Comandos cURL - Funcionalidades Nuevas y Actualizadas

## 1. Reagendamiento de Reserva (Actualizado)

```bash
# PUT /api/v1/reservations/{reservationId}/reschedule
# Solo permite cambiar fecha y cantidades (no productId, productType, tourScheduleId, slotId)

curl --location 'http://localhost:8088/api/v1/reservations/123/reschedule' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN_HERE' \
--data '{
  "newDate": "2026-02-20",
  "configQuantity": [
    {
      "ageType": "ADULT",
      "quantity": 2
    },
    {
      "ageType": "CHILD",
      "quantity": 1
    }
  ]
}'
```

## 2. Obtener Todas las Reservas (Actualizado)

```bash
# GET /api/v1/reservations
# Ahora incluye canReschedule automáticamente y filtra por rol:
# - Cliente: solo sus reservas
# - Proveedor: reservas de sus tours
# - Back office: todas las reservas

# Ejemplo básico
curl --location 'http://localhost:8088/api/v1/reservations?page=0&size=10' \
--header 'Authorization: Bearer YOUR_TOKEN_HERE'

# Con filtros adicionales
curl --location 'http://localhost:8088/api/v1/reservations?page=0&size=10&status=PENDING&providerId=5&reservationId=123' \
--header 'Authorization: Bearer YOUR_TOKEN_HERE'
```

## 3. Obtener Todos los Créditos (Nuevo)

```bash
# GET /api/v1/credits
# - Usuario normal: solo sus créditos
# - Back office: todos los créditos

curl --location 'http://localhost:8088/api/v1/credits' \
--header 'Authorization: Bearer YOUR_TOKEN_HERE'
```

## 4. Crear Pago con Créditos (Actualizado)

### 4.1. Pago solo con Plataforma

```bash
# POST /api/v1/payment
# paymentType: PLATFORM
# amountPlatform debe ser igual al totalPrice

curl --location 'http://localhost:8088/api/v1/payment' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN_HERE' \
--data '{
  "transactionId": "TXN-123456",
  "transactionData": "{}",
  "paymentType": "PLATFORM",
  "amountPlatform": 150.00,
  "items": [
    {
      "shoppingCartItemId": 456,
      "serviceResponsible": {
        "name": "Juan Pérez",
        "email": "juan@example.com",
        "phone": "+1234567890"
      }
    }
  ],
  "payer": {
    "id": 1,
    "name": "María García",
    "email": "maria@example.com",
    "phone": "+0987654321",
    "documentType": "PASSPORT",
    "documentNumber": "AB123456"
  }
}'
```

### 4.2. Pago solo con Crédito (un solo crédito)

```bash
# POST /api/v1/payment
# paymentType: CREDIT
# amountCredit debe ser igual al totalPrice
# amountPlatform debe ser 0 o null

curl --location 'http://localhost:8088/api/v1/payment' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN_HERE' \
--data '{
  "transactionId": "TXN-123457",
  "transactionData": "{}",
  "paymentType": "CREDIT",
  "amountCredit": 150.00,
  "amountPlatform": 0,
  "creditData": {
    "creditIds": [789]
  },
  "items": [
    {
      "shoppingCartItemId": 456,
      "serviceResponsible": {
        "name": "Juan Pérez",
        "email": "juan@example.com",
        "phone": "+1234567890"
      }
    }
  ],
  "payer": {
    "id": 1,
    "name": "María García",
    "email": "maria@example.com",
    "phone": "+0987654321",
    "documentType": "PASSPORT",
    "documentNumber": "AB123456"
  }
}'
```

### 4.3. Pago solo con Crédito (múltiples créditos)

```bash
# POST /api/v1/payment
# paymentType: CREDIT
# Consume múltiples créditos: primero el de mayor valor en su totalidad,
# luego el siguiente si sobra dinero

curl --location 'http://localhost:8088/api/v1/payment' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN_HERE' \
--data '{
  "transactionId": "TXN-123458",
  "transactionData": "{}",
  "paymentType": "CREDIT",
  "amountCredit": 250.00,
  "amountPlatform": 0,
  "creditData": {
    "creditIds": [789, 790, 791]
  },
  "items": [
    {
      "shoppingCartItemId": 456,
      "serviceResponsible": {
        "name": "Juan Pérez",
        "email": "juan@example.com",
        "phone": "+1234567890"
      }
    }
  ],
  "payer": {
    "id": 1,
    "name": "María García",
    "email": "maria@example.com",
    "phone": "+0987654321",
    "documentType": "PASSPORT",
    "documentNumber": "AB123456"
  }
}'
```

### 4.4. Pago con Crédito + Plataforma (múltiples créditos)

```bash
# POST /api/v1/payment
# paymentType: CREDIT_AND_PLATFORM
# amountCredit + amountPlatform debe ser igual al totalPrice
# Consume múltiples créditos si es necesario

curl --location 'http://localhost:8088/api/v1/payment' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_TOKEN_HERE' \
--data '{
  "transactionId": "TXN-123459",
  "transactionData": "{}",
  "paymentType": "CREDIT_AND_PLATFORM",
  "amountCredit": 200.00,
  "amountPlatform": 50.00,
  "creditData": {
    "creditIds": [789, 790]
  },
  "items": [
    {
      "shoppingCartItemId": 456,
      "serviceResponsible": {
        "name": "Juan Pérez",
        "email": "juan@example.com",
        "phone": "+1234567890"
      }
    }
  ],
  "payer": {
    "id": 1,
    "name": "María García",
    "email": "maria@example.com",
    "phone": "+0987654321",
    "documentType": "PASSPORT",
    "documentNumber": "AB123456"
  }
}'
```

## Notas Importantes

1. **Validación de Montos**: La suma de `amountCredit + amountPlatform` debe ser igual al `totalPrice` del carrito.

2. **Consumo de Múltiples Créditos**:
   - Los créditos se consumen en orden de mayor a menor valor
   - Primero se consume el crédito de mayor valor en su totalidad
   - Si sobra dinero, se consume el siguiente crédito y se actualiza su monto
   - Si falla el consumo de cualquier crédito, todo el proceso de pago y creación de reserva falla (rollback automático)

3. **Tipos de Pago**:
   - `PLATFORM`: Solo plataforma, `amountPlatform` = `totalPrice`, `amountCredit` = 0 o null
   - `CREDIT`: Solo crédito, `amountCredit` = `totalPrice`, `amountPlatform` = 0 o null
   - `CREDIT_AND_PLATFORM`: Ambos, `amountCredit + amountPlatform` = `totalPrice`

4. **Manejo de Errores**: Si el consumo de créditos falla por cualquier motivo (crédito no encontrado, expirado, monto insuficiente, etc.), todo el proceso de pago y creación de reserva se revierte automáticamente gracias a `@Transactional`.
