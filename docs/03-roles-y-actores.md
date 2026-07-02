# 03 — Roles y actores

Actores humanos y sistemas que interactúan con Tourya, con sus responsabilidades y permisos.

---

## Actores humanos

### 1. Turista (`USER`)

✅ Rol por defecto al registrarse en la plataforma.

**Quién es**: persona que busca y reserva experiencias turísticas.

**Acceso**:
- Web app (Angular): rutas públicas + `/home`, `/tours`, `/cart`, `/profile`, etc.
- Mobile app (MAUI): TabBar "tourist" — Explorar, Carrito, Mis Viajes, Perfil.

**Puede**:
- Buscar tours con filtros (categoría, fecha, precio, duración, tags, ubicación).
- Ver detalle de tour (galería, descripción, itinerario, FAQ, reseñas).
- Agregar al carrito y hacer checkout con Wompi.
- Pagar con tarjeta + opcionalmente con créditos acumulados.
- Ver sus reservas con QR.
- Cancelar / reagendar reservas (según política del tour).
- Dejar reseñas con fotos (tras la entrega).
- Transferir créditos a otros turistas (por documento).
- Gestionar wishlist.
- Solicitar convertirse en PROVIDER (KYB). esto solo si como proveedor va a tener el mismo tipo y número de documento.

**No puede**:
- Crear tours.
- Ver datos de otros usuarios.
- Acceder al panel de proveedor o backoffice.

---

### 2. Operador titular (`PROVIDER`)

✅ Asignado automáticamente al usuario cuando su `RequestProvider` es aprobado por un ADMIN.

**Quién es**: dueño / representante legal de un negocio turístico (con RNT).

**Acceso**:
- Web app: `/providers/provider-panel`, gestión de tours, schedules, reservas, payouts, reseñas, sub-usuarios.
- Mobile app: TabBar "provider" — Dashboard, Mis Tours, Reservaciones, QR Scanner, Reseñas.

**Puede**:
- Todo lo que puede un USER (mantiene el rol USER también).
- Crear y editar sus tours (con descripciones, galería, atracciones, FAQ, itinerario, políticas de cancelación).
- Enviar tours a aprobación (`PUT /tour/user/submitTourById/{id}`).
- Crear configuraciones de horario (`POST /tour-schedules/config`) con slots y precios por tipo de persona.
- Definir `providerPrice` por slot (NO ve la comisión Tourya).
- Ver y gestionar las reservas de sus tours.
- Marcar reservas como entregadas (escaneando QR o manualmente).
- Responder reseñas.
- Ver sus payout orders y comprobantes de pago.
- Crear sub-usuarios (operarios) con contraseña temporal.
- Asignar tours específicos a sus sub-usuarios (operarios).
- Definir un "tour principal" para cada sub-usuario.
- Resetear la contraseña de sus sub-usuarios.
- Subir documentos KYB para el RequestProvider.

**No puede**:
- Ver datos de OTROS proveedores (filtros del backend hacen scope por `providerId`).
- Aprobar otros proveedores.
- Asignar / modificar la comisión Tourya (`slotPercentageTourya`).
- Acceder al backoffice.

---

### 3. Sub-usuario del operador (`PROVIDER_OPERATOR`)

✅ Creado por el PROVIDER titular con `POST /provider/users`. Recibe contraseña temporal del titular.

**Quién es**: empleado del operador (recepcionista, guía, supervisor) que necesita acceder al panel de Tourya sin tener la cuenta del titular.

**Acceso**:
- Web app: portal del proveedor con vistas limitadas (reservas + acción confirmar (escanear QR)).
- Mobile app: TabBar "provider" — pero con acceso limitado a los tours asignados (escanear QR).

**Puede**:
- Iniciar sesión con la contraseña temporal (`POST /auth/authenticate` devuelve `mustChangePassword: true`).
- Cambiar su contraseña (`PATCH /users` con currentPassword + newPassword).
- Ver las reservas de los tours que tiene asignados.
- Confirmar reservas (marcar como `DELIVERED`).
- Escanear códigos QR de los turistas.

**No puede**:
- Acceder a los dashboard del operador
- Crear ni editar tours.
- Crear ni editar schedules.
- Ver / modificar precios.
- Ver / responder reseñas.
- Ver payouts del titular.
- Crear o modificar otros sub-usuarios.
- Acceder a tours NO asignados (scope automático por `provider_user_tour`).

El `PROVIDER_OPERATOR` solo puede ver las reservas de los tours que el tiene asignados y confirmar las reservas de forma manual o escaneando el QR.

---

### 4. Administrador (`ADMIN`)

✅ Asignación manual en BD (no hay endpoint público para asignar este rol).

**Quién es**: equipo de Tourya con permisos máximos.

**Acceso**:
- Web app: backoffice — todas las áreas.
- Mobile: no aplica (no hay vistas admin en mobile).

**Puede**:
- Aprobar / pre-aprobar / marcar incompletos / cancelar `RequestProvider`s.
- Activar / desactivar / eliminar proveedores.
- Aceptar / devolver / cancelar tours (`PUT /tour/admin/acceptTourById/{id}`, etc.).
- Listar todos los usuarios, bloquear / desbloquear.
- Crear categorías y subcategorías de tours.
- Crear tipos de documento KYB.
- Crear / gestionar reportes marítimos (DIMAR).
- Asignar la comisión Tourya (`PUT /tour-schedules/tours/{tourId}/percentage`).
- Subir comprobantes de pago a payout orders y marcarlas como pagadas.
- Cancelar reservas por lluvia (`PUT /reservations/{id}/cancel/rain`).
- Ver reseñas en cualquier estado (PENDING / PUBLISHED / CANCELED).
- Acceso completo a actuator (Spring Boot Admin endpoints).

solo puede haber en la plataforma un usuario con el rol de ADMIN. ADMIN y Super ADMIN es lo mismo.

---

### 5. Backoffice Operación (`BACKOFFICE_OPERATION`)

✅ Rol mencionado en `FRONT_GUIA_PRECIOS_Y_OPERADORES.md` y en endpoints de comisión.

**Quién es**: equipo de operaciones de Tourya (no necesariamente admin completo).

**Acceso**:
- Web app: vistas de backoffice (precios, comisiones, payouts).

**Puede**:
- Lo mismo que ADMIN para fines de gestión de comisión y precios:
  - `PUT /tour-schedules/tours/{tourId}/percentage` (% por rango de fechas)
  - `PUT /tour-schedules/tours/{tourId}/percentage/{slotId}` (% puntual)
  - Ver `slotPorcentajeTourya` en respuestas (PROVIDER no lo ve)
- Ver listados completos de reservas, proveedores, tours.
- Gestionar payout orders (subir comprobantes).

📌 PENDIENTE LUIS — ¿`BACKOFFICE_OPERATION` y `ADMIN` se diferencian en qué exactamente? `BACKOFFICE_OPERATION` puede solo gestionar los % de las comisiones para cada tour aprobado y subir los pagos de las ordenes de pago. `ADMIN` puede hacer tanto lo que puede el usuario BACKOFFICE_OPERATION y adicional puede aprobar solicitudes de proveedores (Request_Provider) y puedo aprobar tours. ¿Hay otros roles de backoffice (ej. `BACKOFFICE_FINANCE`, `BACKOFFICE_CONTENT`)? esos son los unicos tipos de roles para el Backoffice en este momento

---

## Matriz de permisos

| Acción | USER | PROVIDER | PROVIDER_OPERATOR | BACKOFFICE_OP | ADMIN |
|--------|:----:|:--------:|:----------------:|:-------------:|:-----:|
| Registro / login | ✅ | ✅ | ✅* | ✅ | ✅ |
| Buscar tours | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reservar tour | ✅ | ✅ | ❌ | ❌ | ❌ |
| Pagar con Wompi | ✅ | ✅ | ❌ | ❌ | ❌ |
| Ver mis reservas | ✅ | ✅ | ❌ | ❌ | ❌ |
| Cancelar mi reserva | ✅ | ✅ | ❌ | ❌ | ❌ |
| Dejar reseña | ✅ | ✅ | ❌ | ❌ | ❌ |
| Transferir crédito | ✅ | ✅ | ❌ | ❌ | ❌ |
| Crear tour | ❌ | ✅ | ❌ | ❌ | ❌ |
| Crear schedule + precios | ❌ | ✅ | ❌ | ❌ | ❌ |
| Ver `providerPrice` | ❌ | ✅ | ❌ | ✅ | ✅ |
| Ver `slotPercentageTourya` | ❌ | ❌ | ❌ | ✅ | ✅ |
| Asignar comisión Tourya | ❌ | ❌ | ❌ | ✅ | ✅ |
| Crear sub-operadores | ❌ | ✅ | ❌ | ❌ | ❌ |
| Confirmar reservas (QR) | ❌ | ✅ | ✅ | ❌ | ❌ |
| Responder reseña | ❌ | ✅ | ❌ | ❌ | ❌ |
| Ver payouts propios | ❌ | ✅ | ❌ | ❌ | ❌ |
| Subir comprobante payout | ❌ | ❌ | ❌ | ✅ | ✅ |
| Aprobar KYB | ❌ | ❌ | ❌ | ❌ | ✅ |
| Aceptar / devolver tour | ❌ | ❌ | ❌ | ❌ | ✅ |
| Cancelar por lluvia | ❌ | ❌ | ❌ | ❌ | ✅ |
| Reporte DIMAR | ❌ | ❌ | ❌ | ✅ | ✅ |
| Bloquear usuarios | ❌ | ✅ | ❌ | ❌ | ✅ |
| Ver reseñas pendientes | ❌ | ✅ | ❌ | ❌ | ✅ |

*El PROVIDER_OPERATOR usa el mismo login que cualquier User, pero su contraseña inicial la define el titular.

---

## Sistemas externos (actores no humanos)

### Wompi (pasarela de pagos)
- **Rol**: confirma transacciones de pago.
- **Cómo interactúa**:
  - Frontend abre el widget Wompi con una referencia firmada (`/reference/generate` la genera).
  - Tourya recibe el `transactionId` del frontend y lo guarda en `Payment.transaction_id`.
- ⚠️ **NO hay webhook de Wompi implementado**: la confirmación es client-side. Si el front falla, el pago queda en limbo.
- 📌 PENDIENTE LUIS — confirmar si el cliente ha pedido webhook server-side de Wompi. Si es mejor hacerlo por Webhook de wompi entonces avanzar con esa integración. no debemos tener evitar al maximo problemas con los pagos.

### Firebase Authentication (Google + Facebook)
- **Rol**: autenticación social.
- **Cómo interactúa**:
  - Frontend abre popup Firebase, obtiene `firebaseUid`.
  - Frontend envía `{firstname, lastname, email, uuidSocial}` a `POST /auth/social-auth`.
- ⚠️ **El backend NO valida el token** — solo confía en el `uuidSocial`. Vulnerabilidad documentada en `security-remediation-plan.md`.
- 📌 Propuesta para reemplazar Firebase por validación directa con Google/Facebook (Token Exchange) en `social-login-google-facebook.md`.

### Gmail Workspace SMTP Relay
- **Rol**: envío de emails (activación, recuperación, notificaciones).
- **Cómo interactúa**: Spring Mail vía `smtp-relay.gmail.com:587` con STARTTLS.
- ✅ Cuenta de servicio: `noreply@wass.com.co` (configurada por Luis).

### Google Cloud Storage (GCS)
- **Rol**: almacenamiento de archivos (fotos de tour, comprobantes, documentos KYB).
- **Cómo interactúa**: `GcsStorageService` implementa `IStorageService`.
- ⚠️ El service account key (`tourya-dev-sa-key.json`) está commiteado en el repo. Vulnerabilidad documentada.

### AWS S3 (legacy)
- **Rol**: almacenamiento de archivos en la versión AWS (ambiente legacy).
- ✅ Aún soportado vía `S3Service` (alternativa de `IStorageService`), seleccionable por property `storage.provider`.

### DIMAR (Dirección General Marítima)
- **Rol**: autoridad marítima de Colombia.
- **Cómo interactúa con Tourya**:
  - No hay integración directa.
  - El equipo backoffice ingresa manualmente "Maritime Activity Reports" (`POST /maritime-activity-reports`) que sirven de soporte para cancelar reservas por mal tiempo.
- 📌 PENDIENTE LUIS — ¿hay roadmap para integrar API DIMAR directamente? actualmente Dimar envia un PDF diariamente con el reporte. Se podria crear un servicio que lea la informacion del archivo y cree el reporte maritimo diario. voy a revisar si DIMAR tiene un API o un link donde se genere el archivo y podamos ver como integrarlo.

### Banco / sistema de pagos del operador
- **Rol**: receptor de los payouts.
- **Cómo interactúa con Tourya**:
  - No hay integración bancaria.
  - El backoffice hace la transferencia manualmente, sube el comprobante (`POST /provider/payout-orders/admin/{orderId}/proof`) y marca como pagada.
- 📌 PENDIENTE LUIS — ¿hay planes de integrar Bre-B / nequi / transferencia automatizada? se debe revisar si WOMPI y MERCADOPAGO tienen APIS para generar los pagos, pero que tenga total seguridad y trasabilidad del pago. revisar si es mejor que el pago lo haga un agente con trazabilidad y con reglas de hacer los pagos hasta cierto monto.   

---

## Mapa de relaciones

```
                  ┌──────────────┐
                  │   ADMIN /    │
                  │ BACKOFFICE   │ ←──── aprueba KYB, ajusta comisión, ve todo
                  └──────┬───────┘
                         │ aprueba
                         ▼
                  ┌──────────────┐         ┌─────────────────┐
                  │   PROVIDER   │ ←─crea─ │ RequestProvider │
                  │  (titular)   │         └─────────────────┘
                  └──────┬───────┘
                  crea   │
              ┌──────────┴────────┐
              ▼                   ▼
    ┌─────────────────┐   ┌──────────────────┐
    │ Tours y         │   │ PROVIDER_        │
    │ Schedules       │   │ OPERATOR         │ (subusuario)
    └────────┬────────┘   └─────────┬────────┘
             │ publica             │ atiende
             ▼                     ▼
       ┌──────────────────────────────────┐
       │           USER (turista)          │ ←─ se registra solo
       │  busca, reserva, paga (Wompi),   │
       │  califica, cancela, transfiere   │
       └──────────────────────────────────┘
                       │
                       ▼
       ┌──────────────────────────────────┐
       │             Wompi                 │
       │       (procesa el pago)          │
       └──────────────────────────────────┘
```
