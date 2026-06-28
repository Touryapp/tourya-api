# 02 — Glosario

Diccionario de términos del dominio de Tourya. Si un término ya está usado en código (entidad, campo, enum), se anota su nombre técnico entre paréntesis.

> Convención: los términos están agrupados por área. Cuando un término del cliente difiere del nombre en código, se anota como **sinónimo**.

---

## Negocio

### Tourya
La plataforma misma. Marketplace de experiencias turísticas en Colombia. Cobra comisión por transacción a los operadores.

### Operador / Proveedor (`Provider`)
Empresa o persona que publica tours y experiencias en Tourya. Tiene RNT, datos legales y bancarios. Recibe payouts periódicos por sus ventas. **Sinónimos del cliente**: "operador turístico", "prestador de servicios".

### Operador del proveedor / Sub-usuario (`ProviderUser`, rol `PROVIDER_OPERATOR`)
Sub-usuario que el operador titular crea para que su equipo (recepcionista, guía, supervisor) acceda al panel sin compartir contraseña del titular. Puede tener tours específicos asignados y un "tour principal". **Sinónimo**: "operario".

### Tour (`Tour`)
Producto principal de Tourya: una experiencia turística vendible (paseo en bote, tour gastronómico, city tour, etc.). Tiene categoría, descripciones multilingües, galería, atracciones, FAQ, itinerario, políticas de cancelación.

### Estado del tour (`TourStatusEnum`)
Ciclo de vida del tour: `CREATED` → `SUBMITTED` (proveedor lo envía a revisión) → `ACCEPTED` (admin lo aprueba) / `RETURNED` (devuelto para correcciones) / `CANCELLED` (admin lo cancela).

### Categoría / Subcategoría (`TourCategory`, `subCategory` enum)
Clasificación del tour (Aventura, Cultura, Comida, Familias, etc.). Una categoría puede tener varias subcategorías.

### Tag (`tour_tag`, `tags`)
Etiqueta secundaria que enriquece la búsqueda. Hay 11 categorías de tags: Actividades acuáticas, Naturaleza, Playas, Aventura, Cultura, Comida, Familias, Romántica, Momentos del día, Logística, Nocturnos.

### Atracción principal (`TourMainAttraction`)
Texto destacado que aparece en la ficha del tour como gancho de venta (ej. "Avistamiento de delfines garantizado").

### Incluye / No incluye (`TourIncludesExcludes`, `type` enum)
Lista de qué cubre el precio del tour y qué no (ej. incluye: transporte, almuerzo; no incluye: bebidas alcohólicas).

### Itinerario (`TourItinerary`)
Plan paso a paso del tour, con día/hora, título y descripción de cada paso.

### FAQ (`TourFaq`)
Preguntas frecuentes del tour, con pregunta y respuesta en los 3 idiomas.

### Política de cancelación (`TourCancellationPolicy`, `cancellationPolicyType` enum)
Reglas que rigen los reembolsos y reagendamientos del tour: hasta cuántos días antes se permite cancelar, qué porcentaje se devuelve, etc. Puede permitir **reagendamiento por lluvia** (`allowsRainRefund`) — relevante en tours marítimos.

### RNT (Registro Nacional de Turismo)
✅ Número de registro obligatorio para operadores turísticos en Colombia. Tourya lo guarda en `provider.rnt`. Es información pública.

### DIMAR
✅ Dirección General Marítima de Colombia. Tourya tiene un módulo (`MaritimActivityReport`) para que el backoffice registre reportes diarios de actividad marítima — útil para cancelaciones por mal tiempo y cumplimiento.

---

## Horarios y disponibilidad

### Schedule (`TourSchedule`)
Instancia concreta de un tour en una fecha específica (ej. "Tour de Islas del Rosario el 2026-07-15"). Tiene capacidad, slots y estado.

### Configuración de horario / Plantilla (`TourScheduleConfig`)
Plantilla reutilizable que define los días de la semana y franjas horarias en que se ofrece un tour. Al guardarse, genera automáticamente los `TourSchedule` correspondientes.

### Slot (`TourScheduleConfigSlot`)
Franja horaria dentro de un schedule (ej. "9:00 - 12:00"). Tiene capacidad máxima, reservas actuales, y precios por tipo de persona. **Sinónimo**: "turno".

### Tipo de persona / `ageType` (`AgeRangeConfig`, enum `ADULT` / `CHILD` / `INFANT`)
Rango de edad usado para diferenciar precios. Cada slot tiene un precio por tipo. **Sinónimo del cliente**: "adulto / niño / bebé".

### Override de precio (`TourSchedulePriceOverride`)
Ajuste puntual del precio de un slot en una fecha específica (ej. "el 31 de diciembre, el tour cuesta el doble"). No afecta el precio base del slot.

### Override de slot (`TourScheduleSlotOverride`)
Ajuste puntual de la **comisión Tourya** o de la capacidad de un slot en una fecha específica.

---

## Reservas y carrito

### Carrito (`ShoppingCart`)
Bolsa temporal del turista antes de pagar. Contiene `ShoppingCartItem`s (un item por tour/slot/fecha) con `ShoppingCartItemDetail`s (un detalle por `ageType`).

### Estado del carrito (`ShoppingCartStatusEnum`)
`ACTIVE` (en uso) → `PAID` (turista pagó) → `COMPLETED` (turista asistió) / `ABANDONED` (caducó).

### Hold temporal / Reserva temporal (`Reservation` con status `TEMPORAL`)
Cuando el turista inicia checkout, el sistema crea una "reserva temporal" que congela el slot por 15 minutos. Si no paga en ese tiempo, `TemporalReservationExpiryJob` (corre cada 60s) la libera.

### Reserva (`Reservation`)
Compromiso confirmado tras el pago. Tiene QR (`qrUrl`) para fulfillment, estado de entrega (`deliveryStatus`), fecha máxima de cancelación y reschedule.

### Estado de entrega (`deliveryStatus`)
`PENDING` (futuro) → `DELIVERED` (turista asistió, escaneó QR o el operador confirmó) / `CANCELED` (cancelada antes o cancelada por lluvia).

### QR de la reserva (`qrUrl`)
URL al QR generado al confirmar la reserva. Se escanea con la app móvil del proveedor (`QrScannerPage`) para marcar la reserva como `DELIVERED`. Formato del booking ID legible: `TB-{numericId}` (ej. `TB-250`).

### Booking
✅ Sinónimo de "reserva" (`Reservation`). Endpoints públicos lo usan: `/public/bookings/{bookingId}`.

### Persona responsable del servicio (`serviceResponsibleName/Email/Phone`)
Datos del contacto que asistirá efectivamente al tour (puede no ser el pagador). Útil cuando se reserva para terceros.

---

## Pagos y finanzas

### Wompi
Pasarela de pagos colombiana. Tourya usa el flujo de **Wompi Widget** (WebView en mobile, JS embebido en web). Genera referencias firmadas con `SHA256` (endpoint `/reference/generate`).

### Payment (`Payment`)
Registro de una transacción Wompi. Lleva `transactionId` (de Wompi), datos del pagador (`payerId`, `payerName`, `payerEmail`...), y monto pagado con crédito (`amountCredit`) si aplica.

### Crédito (`Credit`)
Saldo a favor del turista, generado por:
- Cancelación de reserva (refund según política).
- Cancelación por lluvia.
- Transferencia desde otro turista.

Tiene fecha de expiración (1 año desde creación). Puede usarse total o parcialmente en otra compra.

### Transferencia de crédito
Un turista puede ceder un crédito a otro turista, identificándolo por documento. Una vez transferido, no se puede revertir. Endpoint: `POST /credits/{creditId}/transfer`.

### Account Payable (`AccountPayable`)
Cuenta por pagar al operador. Una por reserva entregada (`DELIVERED`). Lleva el monto que Tourya le debe (`providerPrice × quantity`, sin la comisión).

### Payout Order (`ProviderPayoutOrder`)
"Orden de pago" semanal al operador. Agrupa varias `AccountPayable`s y se genera automáticamente lunes y jueves.

### Payout Date (`payDate`)
Fecha en que se efectúa el pago al operador. **Lunes** se paga el martes; **jueves** se paga el viernes.

### Payout Available Date (`payout_available_date`)
Fecha desde la cual una reserva puede entrar al payout. Regla actual: `reservation_date + 2 días` (buffer para reclamos).

### Comprobante de pago (`ProviderPayoutAttachment`)
Archivo (PDF, imagen) que el backoffice sube al marcar una payout order como pagada.

### Comisión Tourya (`slotPercentageTourya`)
Porcentaje que Tourya cobra sobre el precio del proveedor. Se almacena como puntos (ej. `15` = 15%). Se asigna por slot y puede tener overrides puntuales.

### `providerPrice`
Precio que el operador define que quiere recibir. **NO** es lo que paga el turista. El turista paga `price = providerPrice × (1 + slotPercentageTourya / 100)`.

### `price`
Precio final de venta al turista (incluye la comisión Tourya). Es lo que se muestra en el marketplace.

---

## Reseñas

### Review (`Review`)
Reseña que un turista deja después de asistir a un tour. Tiene rating (1-5), comentario multilingüe, hasta 5 fotos, y motivo (`reasonId`).

### Estado de la review (`status`)
`PENDING` (en moderación) → `PUBLISHED` (visible) / `CANCELED` (rechazada). ✅ A partir de migración 040, las nuevas reviews se publican directamente (`PUBLISHED`).

### Response del proveedor (`ReviewAnswer`)
Respuesta del operador a una reseña. También multilingüe, con hasta 5 fotos adjuntas (`ReviewAnswerAttachment`).

### Motivo de la review (`review_reason` enum)
6 opciones predefinidas (ej. "Excelente experiencia", "No cumplió expectativas") para clasificar la reseña.

### Likes / Dislikes / Hearts
Reacciones de otros usuarios a una reseña, también disponibles en las respuestas. ❓ No es claro si están expuestas en UI.

---

## Onboarding (KYB)

### KYB (Know Your Business)
Proceso de validación del operador antes de publicar tours. En Tourya: `RequestProvider` lifecycle.

### Solicitud de proveedor (`RequestProvider`)
Formulario que un usuario USER llena para pedir convertirse en PROVIDER. Tiene estados: `DRAFT` → `SUBMITTED` → `PRE_APPROVED` / `INCOMPLETE` / `APPROVED` / `CANCELED`.

### Tipo de documento KYB (`RequestProviderDocumentType`)
Catálogo de documentos que el operador debe adjuntar (ej. RUT, certificación bancaria, RNT, cédula del representante legal). Algunos son obligatorios (`mandatory = true`).

### Galería de la solicitud (`RequestProviderGallery`)
Documentos cargados por el operador en su solicitud. Se guardan en GCS / S3.

---

## Usuarios y autenticación

### Usuario (`User`, tabla `_user`)
Cuenta de acceso a la plataforma. Tiene `email` (único), `password` (BCrypt), `roles[]`, y campos de auditoría.

### Rol (`Role`, `UserRoleType` enum)
Los roles del sistema:
- `USER` — turista (rol por defecto al registrarse).
- `PROVIDER` — operador titular (asignado al aprobar KYB).
- `PROVIDER_OPERATOR` — sub-usuario del operador.
- `ADMIN` — administrador de Tourya.
- `BACKOFFICE_OPERATION` — equipo de operación de Tourya (puede ajustar comisiones).

### JWT (JSON Web Token)
Token de sesión emitido por Tourya al autenticarse. Expira en 24 horas. Contiene `email` (subject), `fullName`, `authorities` (roles).

### `mustChangePassword`
Flag en `User` que indica si el usuario debe cambiar la contraseña en su próximo login. Se activa cuando el operador titular crea un sub-usuario con contraseña temporal, o cuando un operador resetea la contraseña.

### Token de activación (`Token`)
Código de 6 dígitos que se envía por email al registrarse, para activar la cuenta. Expira a los 15 minutos. Si caduca, se reenvía automáticamente al intentar activar.

### Perfil de turista (`TouristProfile`)
Datos extendidos del turista (foto, documento, dirección completa) — separado del `User` para no recargar el auth principal.

### Social Login
Hoy: vía Firebase Authentication (Google y Facebook providers). Frontend obtiene `firebaseUid` y lo envía al backend (`POST /auth/social-auth`). ⚠️ Sin validación de token real del proveedor. Hay propuesta para migrar a Token Exchange directo con Google/Facebook (ver `social-login-google-facebook.md`).

---

## Internacionalización (i18n)

### TranslatedField
Tipo personalizado (POJO + AttributeConverter JPA) que almacena un texto en 3 idiomas: `es` (obligatorio), `en` (opcional), `pt` (opcional). Se persiste como JSONB en PostgreSQL.

### Fallback
Si se pide un idioma que está vacío, el sistema devuelve automáticamente el español. Ejemplo: `tour.name.get("en")` → si `en=""`, devuelve `es`.

### Tabla JSONB
Las columnas que usan `TranslatedField` están definidas como `jsonb` en PostgreSQL, con índices GIN para búsquedas full-text y B-tree para búsquedas por idioma específico.

---

## Geografía

### Country / State / City (`Country`, `State`, `City`)
Catálogo de ubicaciones. Para Colombia: 32 departamentos (`State`) con sus municipios (`City`). El campo `state` en español = "departamento".

### Address del tour (`TourAddress`)
Punto geográfico del tour (con `latitude`, `longitude`). Un tour puede tener varios:
- Punto de encuentro
- Punto de finalización
- Punto de recogida

---

## Storage y archivos

### IStorageService
Abstracción de Tourya para guardar archivos (fotos, comprobantes, documentos KYB). Hay dos implementaciones:
- `S3Service` (AWS, legacy)
- `GcsStorageService` (Google Cloud Storage, actual en producción GCP)

Se elige por property `storage.provider`.

### GCS Bucket
Bucket de Google Cloud Storage donde Tourya guarda los archivos en producción.

---

## Jobs y procesos

### `TemporalReservationExpiryJob`
Cron cada 60 segundos. Libera reservas temporales que pasaron de 15 min sin pago.

### `ReservationCancellationFlagsJob`
Cron diario a las 5:00 AM (Bogotá). Desactiva `canCancel` / `canReschedule` en reservas cuyo deadline ya pasó.

### `ProviderPayoutOrderJob`
Cron lunes y jueves a las 7:00 AM (Bogotá). Crea las órdenes de pago a operadores.

---

## Términos técnicos

### BaseEntity
Clase base de JPA que aporta auditoría: `createdDate`, `lastModifiedDate`, `createdBy`, `lastModifiedBy`. La mayoría de entidades la extienden.

### Stored Procedure (SP)
Función PostgreSQL que ejecuta lógica compleja en el servidor de BD. Tourya tiene ~14 SPs de negocio, los más usados son `sp_get_tour_schedule_json` (búsqueda de tours) y `sp_get_provider_reservations` (panel de reservas del proveedor).

### Specification
Patrón de JPA para construir queries dinámicas. Tourya las usa en filtros de búsqueda complejos.

### Migration
Script SQL versionado en `database/migrations/`. Tourya va por la migración **061**. Se ejecutan manualmente (Hibernate `ddl-auto=none`).

---

## Términos a aclarar con Luis

📌 Algunos términos del código no están del todo claros, vale la pena confirmar con Luis:

- **¿"Operador" y "Operario" son lo mismo?** En el código: `PROVIDER` = titular, `PROVIDER_OPERATOR` = sub-usuario. En el negocio diario, ¿cómo se les dice?
- **"Tour principal" (`principalTourId`) del PROVIDER_OPERATOR**: ¿qué significa exactamente? El código sugiere que es el tour donde el operario tiene prioridad/asignación principal.
- **"Tourya percentage"** vs **"slot percentage Tourya"**: ¿son lo mismo o hay un % global vs % por slot?
- **`payout_status` en reservation** vs `status` en `provider_payout_order`: hay redundancia, vale aclarar.
- **`Reservation` vs `TourReservation`**: hay dos modelos de reserva en el código. El más nuevo (y usado) es `Reservation`. `TourReservation` parece legacy. Confirmar si se puede deprecar.
