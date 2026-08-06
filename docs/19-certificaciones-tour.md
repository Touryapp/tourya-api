# 19 — Certificaciones del operador y del personal por tour

> Propuesta funcional y técnica. Sigue el formato de los docs 01–18 y 20. Numeración confirmada con Luis: este documento es el **19**. Numeración de reglas de negocio (`RN-Cxx`) es referencial.
>
> **v2.0** — reescritura completa: las certificaciones se dividen en **dos tipos independientes** (operador y personal), cada uno con su propio lugar de gestión.

---

## Resumen

Las certificaciones de Tourya se dividen en **dos tipos**, con alcance y lugar de gestión distintos:

1. **Certificaciones del operador** (`ProviderCertification`) — acreditaciones de la **empresa/operador en general**, no de un tour puntual (ej. ISO 9001, membresía PADI como centro de buceo, RNT, pólizas). Se gestionan desde una **nueva opción en el menú del panel del proveedor** ("Certificaciones del operador"), independiente de cualquier tour.

2. **Certificaciones del personal** (`TourCertification`) — acreditaciones de **quienes prestan el servicio en un tour específico** (ej. certificación de buceo de un guía, licencia de navegación del capitán). Se gestionan **dentro del wizard de creación/edición de cada tour**, porque aplican a ese tour en particular — un mismo operador puede tener distinto personal certificado según el tour.

Ambos tipos comparten el mismo mecanismo de fondo (ya definido en la v1 de este documento): **aprobación de ADMIN** (como KYB), **catálogo de tipos administrable**, y **vencimiento opcional** (algunas certificaciones vencen, otras no).

**El turista, al ver el detalle de un tour, ve ambos tipos**: las certificaciones del operador dueño del tour, y las certificaciones de personal específicas de ese tour — en dos secciones separadas.

---

## Decisiones de negocio (confirmadas)

- Requiere aprobación de `ADMIN` antes de mostrarse — igual criterio que KYB (ver [03 — Roles y actores](03-roles-y-actores.md)).
- Un tour (o un operador) puede tener **varias certificaciones**, cada una de un tipo distinto (catálogo administrable).
- La **fecha de vencimiento es opcional** — hay certificaciones que vencen (pólizas, permisos anuales) y otras que no vencen nunca (un curso certificado una sola vez). El diseño soporta ambos casos.
- **Separación operador/personal (nueva)**:
  - Certificaciones del **operador** → se gestionan en el panel del proveedor, fuera del contexto de un tour.
  - Certificaciones del **personal** → se gestionan dentro del wizard de cada tour, porque son específicas de ese tour.

---

## Actores involucrados

| Actor | Rol en esta funcionalidad |
|-------|---------------------------|
| **PROVIDER** | Sube certificaciones **propias** (operador) desde el nuevo menú del panel. Sube certificaciones **de personal** desde el wizard de cada tour. Ve el estado de ambas por separado. |
| **ADMIN** | Aprueba o rechaza certificaciones de ambos tipos. Administra el **catálogo de tipos de certificación** (`CertificationType`), indicando si cada tipo aplica a operador, a personal, o ambos. |
| **BACKOFFICE_OPERATION** | Puede **ver** el estado de ambos tipos, no puede aprobar/rechazar (mismo criterio que hoy con `providerPrice`). |
| **Turista (USER)** | Ve, en el detalle del tour, dos secciones: **"Certificaciones del operador"** y **"Certificaciones del personal del tour"**. En el card de búsqueda, ve un ícono genérico si el tour cuenta con al menos una certificación (de cualquiera de los dos tipos) aprobada y vigente. |

---

## Entidades

### `CertificationType` (catálogo administrable por ADMIN)

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `name` | TranslatedField | Ej. "ISO 9001", "Certificación PADI (centro de buceo)", "Licencia de navegación", "Certificación de buceo" |
| `code` | String, único | Slug interno |
| `scope` | Enum | **`PROVIDER`** (solo aplica a certificaciones del operador) o **`PERSONNEL`** (solo aplica a certificaciones de personal) — determina en qué formulario aparece disponible (ver **RN-C09**) |
| `isOther` | Boolean, default `false` | Si `true`, es la entrada especial **"Otro"** de ese `scope` (ver **RN-C09b**) — al seleccionarla, el formulario pide `customTypeName` |
| `iconKey` | String, nullable | Ícono de referencia si se quiere diferenciar por tipo |
| `status` | Enum | `ACTIVE`, `INACTIVE` |
| `createdAt` | DateTime | |

### `ProviderCertification` (certificaciones del operador)

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `providerId` | FK → `Provider` | |
| `certificationTypeId` | FK → `CertificationType` | Debe tener `scope = PROVIDER` |
| `customTypeName` | String, nullable | Obligatorio **solo si** `certificationTypeId` apunta a la entrada `isOther = true` — el operador escribe el nombre real de su certificación |
| `fileUrl` | String | Documento subido (GCS/S3, mismo `IStorageService` que el resto de documentos) |
| `issuedDate` | Date, nullable | |
| `expirationDate` | Date, nullable | Opcional — ver decisión de negocio |
| `status` | Enum | `PENDING`, `APPROVED`, `REJECTED`, `EXPIRED` |
| `rejectionReason` | String, nullable | |
| `reviewedBy` | FK → `User` (ADMIN), nullable | |
| `reviewedAt` | DateTime, nullable | |
| `createdAt` | DateTime | |

### `TourCertification` (certificaciones del personal, específicas de un tour)

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | Long | PK |
| `tourId` | FK → `Tour` | |
| `certificationTypeId` | FK → `CertificationType` | Debe tener `scope = PERSONNEL` |
| `customTypeName` | String, nullable | Obligatorio **solo si** `certificationTypeId` apunta a la entrada `isOther = true` |
| `staffName` | String, nullable | Nombre de la persona que porta la certificación (ej. "Capitán Andrés Pérez"). **Confirmado**: es texto libre — no se vincula a un `PROVIDER_OPERATOR` (sub-usuario) del sistema, la certificación queda asociada al tour, no a una cuenta de usuario. |
| `fileUrl` | String | |
| `issuedDate` | Date, nullable | |
| `expirationDate` | Date, nullable | Opcional |
| `status` | Enum | `PENDING`, `APPROVED`, `REJECTED`, `EXPIRED` |
| `rejectionReason` | String, nullable | |
| `reviewedBy` | FK → `User` (ADMIN), nullable | |
| `reviewedAt` | DateTime, nullable | |
| `createdAt` | DateTime | |

---

## Catálogo inicial sugerido de `CertificationType`

A pedido de Luis, propongo un catálogo inicial por `scope`, para no salir sin ninguna opción cargada. **Esto es una sugerencia de partida — debe validarse con el equipo de operaciones antes del lanzamiento**, especialmente las normativas colombianas específicas.

### `scope = PROVIDER` (certificaciones del operador)

| Certificación | Nota |
|---|---|
| ISO 9001 (Gestión de calidad) | Internacional |
| ISO 14001 (Gestión ambiental) | Internacional |
| PADI (centro de buceo) | Internacional — acreditación del operador como centro, no de una persona |
| SSI — Scuba Schools International (centro de buceo) | Internacional, alternativa a PADI |
| RNT — Registro Nacional de Turismo | Colombia, ya mencionado como requisito legal en [01 — Visión y negocio](01-vision-y-negocio.md) |
| NTS-TS — Norma Técnica Sectorial de Turismo Sostenible | Colombia |
| Travelife | Internacional, sostenibilidad turística |
| Biosphere Tourism | Internacional, sostenibilidad turística |
| **Otro** | `isOther = true` — el operador escribe el nombre en `customTypeName` |

### `scope = PERSONNEL` (certificaciones del personal)

| Certificación | Nota |
|---|---|
| Certificación de buceo (Open Water / Advanced / Divemaster) | PADI/SSI a nivel individual, no del operador |
| Licencia de navegación / Patente de mando | DIMAR — capitanes/pilotos de embarcaciones |
| Primeros auxilios / RCP | General |
| Guía de turismo certificado | Registro Nacional de Guías de Turismo, Colombia |
| Curso de rescate acuático | Aplica a tours marítimos/acuáticos |
| **Otro** | `isOther = true` — el operador escribe el nombre en `customTypeName` |

- **RN-C01 — Carga de certificación del operador**: el `PROVIDER` sube una `ProviderCertification` desde la nueva sección **"Certificaciones del operador"** del panel (web y mobile) — **no ligada a ningún tour puntual**, aplica a toda la empresa. Selecciona un `certificationTypeId` con `scope = PROVIDER`, adjunta archivo, `issuedDate`/`expirationDate` opcionales. Queda en `PENDING`.

- **RN-C01b — Carga de certificación de personal**: el `PROVIDER` sube una `TourCertification` desde una **nueva sección dentro del wizard de creación/edición de cada tour** — queda ligada a ese `tourId` específico. Selecciona un `certificationTypeId` con `scope = PERSONNEL`. Queda en `PENDING`.

- **RN-C02 — Aprobación exclusiva de ADMIN**: solo `ADMIN` aprueba o rechaza (con `rejectionReason`) certificaciones de **ambos tipos** — igual que KYB. `BACKOFFICE_OPERATION` puede consultar, no aprobar.

- **RN-C03 — Condición para mostrar el ícono (revisado)**: un tour muestra el ícono de "certificado" en el card de búsqueda si se cumple **al menos una** de estas condiciones:
  - El `Provider` dueño del tour tiene al menos una `ProviderCertification` con `status = APPROVED` y (`expirationDate IS NULL` o `expirationDate >= hoy`), **o**
  - El tour tiene al menos una `TourCertification` (personal) con `status = APPROVED` y vigente en las mismas condiciones.
  
  El ícono sigue siendo **genérico** — no distingue si es por certificación de operador o de personal; el detalle completo vive en `TourDetailPage`.

- **RN-C04 — Múltiples certificaciones, ambos catálogos independientes**: un operador puede tener varias `ProviderCertification` de distintos tipos, y cada uno de sus tours puede tener, por separado, varias `TourCertification` (personal) de distintos tipos. No hay límite ni relación obligatoria entre ambos catálogos.

- **RN-C05 — Vencimiento automático**: un job nocturno (`CertificationExpiryJob`, mismo horario que `ReservationCancellationFlagsJob` — `0 5 0 * * *`, 5 AM Bogotá) recorre **ambas tablas** (`ProviderCertification` y `TourCertification`) y marca `EXPIRED` toda certificación `APPROVED` cuya `expirationDate` esté definida y sea `< hoy`. Las certificaciones sin `expirationDate` (no vencen) nunca son tocadas por este job.

- **RN-C06 — Re-subida tras rechazo o vencimiento**: si una certificación (de cualquiera de los dos tipos) queda `REJECTED` o `EXPIRED`, se puede subir una nueva versión, que entra como un nuevo registro en `PENDING` — se mantiene el histórico para auditoría.

- **RN-C07 — Notificación de vencimiento próximo (confirmado: roadmap)**: queda fuera del alcance de esta versión — se implementará en una **versión posterior**, no bloqueante para el lanzamiento de esta funcionalidad.

- **RN-C08 — Catálogo de tipos**: solo `ADMIN` crea/edita/desactiva `CertificationType`s, definiendo su `scope` (`PROVIDER` o `PERSONNEL`) al crearlo. Desactivar un tipo no afecta certificaciones ya aprobadas con ese tipo, solo evita que se seleccione en nuevas cargas.

- **RN-C09 — El catálogo restringe dónde se puede usar cada tipo**: al cargar una `ProviderCertification`, el formulario solo lista `CertificationType`s con `scope = PROVIDER`. Al cargar una `TourCertification` (personal), solo lista los de `scope = PERSONNEL`. Evita que, por ejemplo, se intente cargar "ISO 9001" (un tipo de operador) como una certificación de personal.

- **RN-C09b — Opción "Otro" (confirmado)**: cada `scope` tiene una entrada especial `isOther = true` ("Otro") en el catálogo. Si el operador la selecciona (porque su certificación real no está en la lista), el formulario exige `customTypeName` (texto libre). ADMIN revisa el archivo y el nombre declarado igual que cualquier otra solicitud (**RN-C02**); si el tipo se repite con frecuencia, ADMIN puede más adelante formalizarlo como una entrada propia del catálogo (proceso manual, fuera de esta funcionalidad).

- **RN-C10 — Filtro de búsqueda "solo tours certificados" (confirmado)**: en el panel de filtros de la búsqueda del turista (columna izquierda, junto a categoría/precio/duración/tags), se agrega un filtro **"Solo tours certificados"**. Por **defecto está desactivado** — la búsqueda muestra todos los tours, certificados o no. Al activarlo, `SearchTourScheduleFullResponse` se filtra por `hasCertification = true` (**RN-C03**).

---

## Flujo propuesto

### Carga de certificación del operador

1. Nueva opción de menú en el panel del proveedor (web y mobile): **"Certificaciones del operador"**.
2. `POST /provider/certifications` (multipart, JWT PROVIDER) — body: `certificationTypeId` (scope=PROVIDER), archivo, `issuedDate`/`expirationDate` opcionales.
3. Crea `ProviderCertification` en `PENDING`.

### Carga de certificación de personal

1. Dentro del wizard de tour (`TourFormPage` en mobile, wizard equivalente en web — ver [10 — Mobile spec](10-mobile-spec.md)), nuevo paso **"Certificaciones del personal"**.
2. `POST /tour/{tourId}/certifications` (multipart, JWT PROVIDER) — body: `certificationTypeId` (scope=PERSONNEL), `staffName` (opcional), archivo, `issuedDate`/`expirationDate` opcionales.
3. Crea `TourCertification` en `PENDING`, ligada a ese tour.

### Revisión (ADMIN) — pantalla unificada

1. Nueva opción en el **menú de ADMIN**: **"Certificaciones"**.
2. Lista **todas** las certificaciones pendientes (y opcionalmente también las ya resueltas, con filtro de estado), **de ambos tipos en una sola tabla**, con las columnas:
   - **Nombre del operador** (`Provider.name`, siempre presente).
   - **Nombre del tour** — solo si es una certificación de personal (`TourCertification`); si es del operador (`ProviderCertification`), esta columna va vacía o con un guion.
   - **Nombre de la certificación** — `CertificationType.name`, o `customTypeName` si el tipo es "Otro" (**RN-C09b**).
   - **Acciones**: `Aprobar` / `Rechazar` (con motivo).
3. `PUT /admin/certifications/provider/{id}/approve` / `reject` (body `rejectionReason` si aplica) — para las del operador.
4. `PUT /admin/certifications/tour/{id}/approve` / `reject` — para las de personal.
5. Backend: un único endpoint de listado (`GET /admin/certifications`) que hace `UNION` de ambas tablas para alimentar la tabla de la pantalla, ya que se muestran juntas.

### Job nocturno de vencimiento

`CertificationExpiryJob` corre diariamente a las 5 AM Bogotá, cubriendo ambas tablas (**RN-C05**).

### Visualización

1. `SearchTourScheduleFullResponse` agrega `hasCertification: boolean`, calculado según **RN-C03** (considera ambas fuentes).
2. El card de búsqueda muestra el ícono genérico si `hasCertification = true`.
3. En `TourDetailPage`, se agregan **dos secciones**:
   - **"Certificaciones del operador"** — lista las `ProviderCertification` `APPROVED` y vigentes del `Provider` dueño del tour.
   - **"Certificaciones del personal"** — lista las `TourCertification` `APPROVED` y vigentes de ese tour puntual (con `staffName` si está definido).
   
   Las `PENDING`, `REJECTED` o `EXPIRED` de ambos tipos **no se muestran al turista**, solo al operador en su propio panel.

---

## Cambios a flujos y entidades existentes

| Entidad/Flujo existente | Cambio necesario |
|---|---|
| `Provider` | No requiere campo nuevo directo — relación 1-a-muchos vía `ProviderCertification.providerId`. |
| `Tour` | Sin cambio directo — relación 1-a-muchos vía `TourCertification.tourId` (ya definida en v1, ahora con alcance exclusivo a personal). |
| `SearchTourScheduleFullResponse` | Agregar `hasCertification: boolean` (calculado, considera ambas tablas). |
| Búsqueda del turista (filtros, columna izquierda) | Agregar filtro **"Solo tours certificados"** (checkbox/toggle), desactivado por defecto (**RN-C10**). |
| Menú de ADMIN (backoffice) | Nueva opción **"Certificaciones"** — pantalla unificada de aprobación (ver Flujo, arriba). |
| Flujo 2 (Creación de tour) — [06](06-flujos-y-eventos.md) | Agregar el paso de carga de certificaciones de personal dentro del wizard; no bloquea `submitTourById`. |
| Resumen de jobs scheduled — [06](06-flujos-y-eventos.md) | Agregar `CertificationExpiryJob` (cron `0 5 0 * * *`). |
| Matriz de permisos — [03](03-roles-y-actores.md) | Agregar filas: "Subir certificaciones del operador" → `PROVIDER`; "Subir certificaciones de personal (por tour)" → `PROVIDER`; "Aprobar/rechazar certificaciones (ambos tipos)" → `ADMIN`; "Crear tipos de certificación" → `ADMIN`. |
| Panel del proveedor (web/mobile) | Nueva opción de menú **"Certificaciones del operador"**, independiente del flujo de tours. |
| `TourFormPage` / wizard web — [10](10-mobile-spec.md) | Nuevo paso **"Certificaciones del personal"** dentro del wizard de tour. |
| `ProviderToursPage` / panel del proveedor | Mostrar badge de estado de certificaciones de personal por tour; agregar badge de estado de certificaciones del operador en el dashboard general. |

---

## Preguntas abiertas

No quedan preguntas abiertas. La última, heredada de la v1, quedó resuelta:

- **Requisito para publicar el tour** → **confirmado: no es requisito**. Las certificaciones son **opcionales** — hay tours que no requieren ninguna. Un tour puede pasar a `ACCEPTED` y venderse normalmente sin tener ninguna `ProviderCertification` ni `TourCertification` aprobada; el ícono de "certificado" (**RN-C03**) simplemente no aparece en ese caso. La aprobación de tours ([06 — Flujo 2](06-flujos-y-eventos.md)) y la aprobación de certificaciones son procesos **completamente independientes**.

---

## Changelog del documento

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 2026-07-23 | Versión inicial. Certificaciones por tour con aprobación de ADMIN (como KYB), múltiples tipos por tour, vencimiento automático vía job nocturno, e ícono genérico en el card de búsqueda. |
| 1.1 | 2026-07-23 | La fecha de vencimiento pasa de obligatoria a opcional — hay certificaciones que no vencen. |
| 2.0 | 2026-07-29 | Reescritura completa: las certificaciones se dividen en dos tipos independientes — **del operador** (`ProviderCertification`, gestionadas desde un nuevo menú del panel del proveedor, sin ligar a un tour) y **del personal** (`TourCertification`, gestionadas dentro del wizard de cada tour). El catálogo `CertificationType` ahora tiene `scope` (`PROVIDER`/`PERSONNEL`) para restringir dónde se puede usar cada tipo (RN-C09). El turista ve ambos tipos en el detalle del tour, en secciones separadas. El ícono de búsqueda sigue siendo genérico, considerando ambas fuentes. |
| 2.1 | 2026-07-29 | Confirmado con Luis: `staffName` queda como texto libre sin vincular a sub-usuario. Notificación de vencimiento pasa al roadmap (RN-C07). Se agrega catálogo inicial sugerido de `CertificationType` por `scope`, con opción **"Otro"** (`isOther` + `customTypeName`, RN-C09b) para certificaciones no listadas. Se agrega filtro de búsqueda "Solo tours certificados" en el panel de filtros, desactivado por defecto (RN-C10). Se rediseña la revisión de ADMIN como una **pantalla unificada** ("Certificaciones" en el menú de ADMIN) con columnas de operador, tour (si aplica), nombre de la certificación y acciones — reemplaza el diseño anterior de pestañas separadas. |
