# Payment API Test cURL Commands

## Crear Pago Exitoso (genera QR automáticamente en S3)

```bash
curl --location 'http://localhost:8088/api/v1/payment' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJmdWxsTmFtZSI6IkRvbm5hbHkgR2ltZW5leiIsInN1YiI6ImRvbm5hbHkuZ2ltZW5lekBnbWFpbC5jb20iLCJpYXQiOjE3NTkxNjk2NTAsImV4cCI6MTc1OTI1NjA1MCwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIlBST1ZJREVSIl19.KKH1z-bc-sJai1lbZWmTZLeOpEnmyfFDyh45GJHCGQGCTiNdP-YNTMJaVe-KVIvX' \
--data-raw '{
    "transactionId": "TXN_123456789",
    "transactionData": "{\"amount\":150.00,\"currency\":\"COP\",\"method\":\"CREDIT_CARD\"}",
    "items": [
        {
            "shoppingCartItemId": 11,
            "serviceResponsible": {
                "name": "María García",
                "email": "maria@tourya.com",
                "phone": "+57 300 123 4567"
            }
        }
    ],
    "payer": {
        "name": "Juan Pérez",
        "email": "juan@email.com",
        "id": 1001,
        "phone": "+57 300 123 4567",
        "documentType": "CC",
        "documentNumber": "12345678"
    }
}'
```

### Respuesta Esperada:

```json
{
    "paymentId": 1,
    "transactionId": "TXN_123456789",
    "transactionData": "{\"amount\":150.00,\"currency\":\"COP\",\"method\":\"CREDIT_CARD\"}",
    "reservation": {
        "reservationId": 1,
        "paymentId": 1,
        "qrUrl": "https://tu-bucket.s3.amazonaws.com/reservations/1/reservation_1_qr.png",
        "reservationDate": "2024-01-16T09:00:00",
        "deliveryStatus": "PENDING",
        "createdDate": "2024-01-15T10:30:00",
        "items": [
            {
                "shoppingCartItemId": 11,
                "serviceResponsible": {
                    "name": "María García",
                    "email": "maria@tourya.com",
                    "phone": 3001234567
                }
            }
        ],
        "lastModifiedDate": "2024-01-15T10:30:00",
        "createdBy": null,
        "lastModifiedBy": null
    },
    "payer": {
        "name": "Juan Pérez",
        "email": "juan@email.com",
        "id": 1001,
        "phone": "+57 300 123 4567",
        "documentType": "CC",
        "documentNumber": "12345678"
    },
    "createdDate": "2024-01-15T10:30:00",
    "lastModifiedDate": "2024-01-15T10:30:00",
    "createdBy": null,
    "lastModifiedBy": null
}
```

## 🎨 Características del QR Generado

### **Colores Personalizados:**
- **Color del QR:** `#1b6475` (azul verdoso de Tourya)
- **Fondo:** Blanco
- **Logo:** Centrado en el QR (60x60 píxeles)

### **Funcionalidad del QR:**
- **Redirección:** Al escanear el QR, redirige a `http://44.203.38.85:8080/home`
- **Parámetros incluidos:**
  - `reservationId`: ID de la reserva
  - `paymentId`: ID del pago
  - `transactionId`: ID de la transacción
  - `payer`: Nombre del pagador
  - `email`: Email del pagador
  - `reservationDate`: Fecha de la reserva
  - `status`: Estado de entrega

### **Logo en el QR:**
El sistema busca el logo en las siguientes ubicaciones:
- `src/main/resources/static/images/logo.png`
- `src/main/resources/static/images/tourya-logo.png`
- `src/main/resources/images/logo.png`
- `src/main/resources/logo.png`

**Si no encuentra el logo, generará automáticamente un logo de respaldo con la letra "T" en un círculo.**

### **Ejemplo de URL generada:**
```
http://44.203.38.85:8080/home?reservationId=1&paymentId=1&transactionId=TXN_123456789&payer=Juan%20P%C3%A9rez&email=juan%40email.com&reservationDate=2024-01-16T09:00:00&status=PENDING
```

## Obtener Pago por ID

```bash
curl --location 'http://localhost:8088/api/v1/payment/1' \
--header 'Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJmdWxsTmFtZSI6IkRvbm5hbHkgR2ltZW5leiIsInN1YiI6ImRvbm5hbHkuZ2ltZW5lekBnbWFpbC5jb20iLCJpYXQiOjE3NTkxNjk2NTAsImV4cCI6MTc1OTI1NjA1MCwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIlBST1ZJREVSIl19.KKH1z-bc-sJai1lbZWmTZLeOpEnmyfFDyh45GJHCGQGCTiNdP-YNTMJaVe-KVIvX'
```

## Obtener Pago por Transaction ID

```bash
curl --location 'http://localhost:8088/api/v1/payment/transaction/TXN_123456789' \
--header 'Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJmdWxsTmFtZSI6IkRvbm5hbHkgR2ltZW5leiIsInN1YiI6ImRvbm5hbHkuZ2ltZW5lekBnbWFpbC5jb20iLCJpYXQiOjE3NTkxNjk2NTAsImV4cCI6MTc1OTI1NjA1MCwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIlBST1ZJREVSIl19.KKH1z-bc-sJai1lbZWmTZLeOpEnmyfFDyh45GJHCGQGCTiNdP-YNTMJaVe-KVIvX'
```

## Obtener URL de Imagen QR (desde S3)

```bash
curl --location 'http://localhost:8088/api/v1/payment/qr/[URL_QR_DESDE_S3]' \
--header 'Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJmdWxsTmFtZSI6IkRvbm5hbHkgR2ltZW5leiIsInN1YiI6ImRvbm5hbHkuZ2ltZW5lekBnbWFpbC5jb20iLCJpYXQiOjE3NTkxNjk2NTAsImV4cCI6MTc1OTI1NjA1MCwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIlBST1ZJREVSIl19.KKH1z-bc-sJai1lbZWmTZLeOpEnmyfFDyh45GJHCGQGCTiNdP-YNTMJaVe-KVIvX'
```

## Obtener Reserva por ID

```bash
curl --location 'http://localhost:8088/api/v1/reservations/1' \
--header 'Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJmdWxsTmFtZSI6IkRvbm5hbHkgR2ltZW5leiIsInN1YiI6ImRvbm5hbHkuZ2ltZW5lekBnbWFpbC5jb20iLCJpYXQiOjE3NTkxNjk2NTAsImV4cCI6MTc1OTI1NjA1MCwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIlBST1ZJREVSIl19.KKH1z-bc-sJai1lbZWmTZLeOpEnmyfFDyh45GJHCGQGCTiNdP-YNTMJaVe-KVIvX'
```

## Regenerar Código QR de una Reserva

```bash
curl --location --request POST 'http://localhost:8088/api/v1/reservations/1/qr/regenerate' \
--header 'Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJmdWxsTmFtZSI6IkRvbm5hbHkgR2ltZW5leiIsInN1YiI6ImRvbm5hbHkuZ2ltZW5lekBnbWFpbC5jb20iLCJpYXQiOjE3NTkxNjk2NTAsImV4cCI6MTc1OTI1NjA1MCwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIlBST1ZJREVSIl19.KKH1z-bc-sJai1lbZWmTZLeOpEnmyfFDyh45GJHCGQGCTiNdP-YNTMJaVe-KVIvX'
```

## Eliminar Código QR de una Reserva

```bash
curl --location --request DELETE 'http://localhost:8088/api/v1/reservations/1/qr' \
--header 'Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJmdWxsTmFtZSI6IkRvbm5hbHkgR2ltZW5leiIsInN1YiI6ImRvbm5hbHkuZ2ltZW5lekBnbWFpbC5jb20iLCJpYXQiOjE3NTkxNjk2NTAsImV4cCI6MTc1OTI1NjA1MCwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIlBST1ZJREVSIl19.KKH1z-bc-sJai1lbZWmTZLeOpEnmyfFDyh45GJHCGQGCTiNdP-YNTMJaVe-KVIvX'
```

## Casos de Error

### Item del carrito no encontrado
```bash
curl --location 'http://localhost:8088/api/v1/payment' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer [tu-token]' \
--data-raw '{
    "transactionId": "TXN_ERROR_001",
    "transactionData": "{\"amount\":150.00,\"currency\":\"COP\",\"method\":\"CREDIT_CARD\"}",
    "items": [
        {
            "shoppingCartItemId": 99999,
            "serviceResponsible": {
                "name": "María García",
                "email": "maria@tourya.com",
                "phone": "+57 300 123 4567"
            }
        }
    ],
    "payer": {
        "name": "Juan Pérez",
        "email": "juan@email.com",
        "id": 1001,
        "phone": "+57 300 123 4567",
        "documentType": "CC",
        "documentNumber": "12345678"
    }
}'
```

### Datos de entrada inválidos
```bash
curl --location 'http://localhost:8088/api/v1/payment' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer [tu-token]' \
--data-raw '{
    "transactionId": "",
    "transactionData": "invalid-json",
    "items": [],
    "payer": {
        "name": "",
        "email": "invalid-email",
        "id": -1
    }
}'
```

## Flujo Completo de Prueba

1. **Crear pago** - Esto automáticamente genera la reserva y sube el QR a S3
2. **Obtener pago** - Verificar que la respuesta incluya la URL del QR
3. **Obtener reserva** - Verificar que la reserva tenga la URL del QR
4. **Regenerar QR** - Si necesitas actualizar el QR
5. **Eliminar QR** - Si necesitas limpiar el QR de S3
