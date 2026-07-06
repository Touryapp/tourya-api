# 14 — Alcance funcional Web vs Mobile

Este documento define **qué debe existir en cada plataforma** (web Angular 19 vs app móvil MAUI Android) para Tourya.

> **Actualización 2026-07-06**: se reescribió con el **alcance definido por Luis** vía WhatsApp. La versión inicial del doc (basada en benchmarks de Airbnb/Booking) proponía dejar la creación de tour, la configuración de horarios y la gestión de operarios como **web-only**. Luis corrigió con un contexto de negocio importante: **en San Andrés la mayoría de los operadores no son "dueños de empresa de oficina", están en la calle** (lancheros, buzos, guías de manglar). Por eso el mobile debe cubrir mucho más que solo fulfillment.

---

## Contexto de negocio (aportado por Luis)

- Los operadores turísticos objetivo de Tourya en San Andrés son **microempresarios operativos** que trabajan en campo (playa, mar, tour en marcha).
- La mayoría **no tiene oficina**, ni computador dedicado. Su herramienta principal es el celular.
- Esto invalida el argumento genérico "el operador titular está en oficina" — en Tourya, el operador titular también está en la calle.

**Consecuencia**: la app móvil debe cubrir **el ciclo operativo completo del proveedor**, no solo consulta de reservas.

---

## Contextos de uso por rol

| Rol | Contexto primario | Plataforma primaria | Plataforma secundaria |
|-----|-------------------|---------------------|-----------------------|
| **Turista (USER)** | En movimiento, en el hotel, en el sitio turístico | **Móvil** | Web (para búsqueda extensa / pre-viaje) |
| **Operario (PROVIDER_OPERATOR)** | En el sitio, en el bote, atendiendo grupos | **Móvil** | — |
| **Operador titular (PROVIDER)** | En la calle, en el bote, mismo contexto que el operario | **Móvil** | Web (opcional, para revisiones más pausadas) |
| **Backoffice (ADMIN / BACKOFFICE_OPERATION)** | En oficina, tareas administrativas | **Web** | — |

---

## Alcance funcional (definido por Luis)

### Turista

La app móvil para el turista debe permitir:

1. **Autenticarse** con:
   - Correo + contraseña
   - Facebook
   - Google (Gmail)
2. **Buscar el tour** con filtros, con ayuda del **Agente IA "Travel Concierge"** (ver [16 — Agentes IA](16-agentes-ia.md)).
3. **Consultar disponibilidad** del tour.
4. **Adjuntar el tour al carrito**.
5. **Pagar con Wompi**.
6. **Ver sus reservas** (incluyendo QR).
7. **Ver / gestionar su lista de deseos** (wishlist).
8. **Gestionar sus créditos** (ver saldo, usarlos en compra, transferirlos).

### Proveedor (operador titular)

La app móvil para el proveedor debe permitir:

1. **Crear tour** (incluyendo todos los datos: descripciones multilingües, atracciones, incluye/no incluye, itinerario, FAQ, políticas de cancelación, galería).
2. **Gestionar el schedule** de los tours aprobados (crear plantillas de horario, definir precios por `ageType`).
3. **Gestionar sus reservas** (ver, filtrar, confirmar por escaneo QR).
4. **Responder reseñas**.
5. **Crear operarios** (`PROVIDER_OPERATOR`) — con contraseña temporal, asignar tours, definir tour principal.

### Operario (`PROVIDER_OPERATOR`)

La app móvil para el operario solo permite:

1. **Gestionar reservas** (ver reservas de los tours asignados, confirmar por escaneo QR).

Es el rol más acotado — móvil como única herramienta y foco 100% en fulfillment.

### Backoffice / ADMIN

El backoffice **NO** se porta a la app móvil. Es exclusivamente web:
- Aprobar `RequestProvider` (KYB).
- Aprobar tours.
- Ajustar % de comisión Tourya por slot / rango de fechas.
- Ver reportes financieros y operativos.
- Subir comprobantes de payouts y marcarlos como pagados.
- Gestionar reportes DIMAR.

---

## Matriz Web vs Mobile (resumen ejecutivo)

Convenciones:
- ✅ Debe existir.
- ❌ No debe existir.
- 🎯 Plataforma primaria (mejor experiencia esperada).
- ⚡ Existe / puede considerarse, pero no es prioritario.

### Turista (USER)

| Funcionalidad | Web | Mobile |
|---------------|:---:|:------:|
| Autenticación email + Facebook + Google | ✅ | ✅ 🎯 |
| Buscar tours (con Travel Concierge IA) | ✅ | ✅ 🎯 |
| Consultar disponibilidad | ✅ | ✅ |
| Carrito | ✅ | ✅ |
| Checkout + Wompi | ✅ | ✅ 🎯 |
| Ver mis reservas + QR | ✅ | ✅ 🎯 |
| Cancelar reserva | ✅ | ✅ |
| Reagendar reserva | ✅ | ✅ |
| Dejar reseña (con fotos) | ✅ | ✅ 🎯 |
| Wishlist | ✅ | ✅ |
| Créditos (ver, usar, transferir) | ✅ | ✅ |
| Perfil turista | ✅ | ✅ |

### Proveedor (PROVIDER)

| Funcionalidad | Web | Mobile |
|---------------|:---:|:------:|
| Login | ✅ | ✅ 🎯 |
| Dashboard (KPIs, ingresos, reservas del día) | ✅ | ✅ 🎯 |
| **Crear tour** (wizard multi-step) | ✅ | ✅ 🎯 |
| Editar tour | ✅ | ✅ |
| **Gestionar schedule** (plantillas, slots, precios) | ✅ | ✅ 🎯 |
| Gestionar reservas | ✅ | ✅ 🎯 |
| Escanear QR | ⚡ | ✅ 🎯 |
| Confirmar reserva manualmente | ✅ | ✅ |
| Ver reseñas | ✅ | ✅ |
| **Responder reseñas** | ✅ | ✅ |
| Ver payouts + comprobantes | ✅ | ⚡ |
| **Crear operarios (`PROVIDER_OPERATOR`)** | ✅ | ✅ |
| Editar / reasignar operarios | ✅ | ✅ |
| Resetear password de operarios | ✅ | ✅ |
| Panel KYB / documentos | ✅ | ✅ |

### Operario (`PROVIDER_OPERATOR`)

| Funcionalidad | Web | Mobile |
|---------------|:---:|:------:|
| Login con clave temporal + cambio | ✅ | ✅ 🎯 |
| Ver reservas de tours asignados | ✅ | ✅ 🎯 |
| **Escanear QR** | ❌ | ✅ 🎯 |
| Confirmar reserva manualmente | ✅ | ✅ |
| Ver "próximas reservas del día" | ⚡ | ✅ 🎯 |

### Backoffice / ADMIN

| Funcionalidad | Web | Mobile |
|---------------|:---:|:------:|
| Aprobar KYB | ✅ 🎯 | ❌ |
| Aprobar tours | ✅ 🎯 | ❌ |
| Ajustar % comisión Tourya | ✅ 🎯 | ❌ |
| Ver reportes | ✅ 🎯 | ❌ |
| Subir comprobantes de payout | ✅ 🎯 | ❌ |
| Reportes DIMAR | ✅ 🎯 | ❌ |

---

## Funcionalidades donde el mobile agrega valor incremental

Además del alcance funcional definido, la app móvil debe capitalizar capacidades que solo el celular ofrece:

| Feature | Rol beneficiado | Justificación |
|---------|-----------------|---------------|
| **Escaneo QR** | Operario, Operador | Cámara nativa — confirmar reservas en sitio |
| **Notificaciones push** | Turista, Operador, Operario | Cerrar loop de comunicación (nueva reserva, cancelación, recordatorio) |
| **Cámara para reseñas** | Turista | Foto post-experiencia rápida |
| **Cámara para docs KYB / galería del tour** | Operador | Subir directamente desde el móvil |
| **Geolocalización** | Turista | "Tours cerca de mí", "cómo llegar al punto de encuentro" |
| **Deep-linking** | Turista | Compartir tours por WhatsApp con link que abre la app |
| **Wallet integration** (Apple Pay / Google Pay vía Wompi) | Turista | UX nativa mejor |
| **Modo offline** | Operario, Operador | Trabajar sin señal en la isla / bote |
| **Widget "próxima reserva"** | Turista, Operario | Ver QR / próxima reserva desde pantalla de bloqueo |

---

## Consideraciones de UX específicas para mobile

Dado que el proveedor gestionará flujos completos (crear tour, schedule) desde el celular, hay retos de UX que hay que resolver bien:

### Wizard de creación de tour en mobile
- Dividir en pasos cortos, uno por pantalla.
- Guardar borrador automáticamente entre pasos (no perder el trabajo si el operador cierra la app o pierde señal).
- Optimizar la subida de fotos de galería: comprimir automáticamente, subir en background, mostrar progreso.
- Considerar entrada por voz (dictado) para las descripciones largas.
- Ayudarse con el agente IA (traducción automática es → en, pt-BR + sugerencia de texto).

### Configuración de schedule en mobile
- Presentar el calendario con vistas de día / semana / mes.
- Simplificar la definición de precios: si el operador ya tiene tours similares, permitir "copiar precios de otro tour".
- Aprovechar el default `Tour.percentageTourya` para pre-calcular el `price` sin fricción para el operador.

### Gestión de operarios en mobile
- Formulario corto: nombre, apellido, email, tours asignados, tour principal, contraseña temporal.
- Compartir la contraseña temporal directamente por WhatsApp / SMS desde la app (deep-link).

---

## Requisitos técnicos derivados

Para que el alcance mobile funcione con la UX apropiada, se requiere:

| Requisito | Motivo |
|-----------|--------|
| **Compresión de imágenes en cliente** | Subir 7 fotos de 5 MB (galería) desde 4G |
| **Upload resumable / background** | No perder el upload si la app se pausa |
| **Autosave / drafts** | Wizard de tour largo → tolerar interrupciones |
| **Cache local (offline básico)** | Ver reservas del día sin señal |
| **Firebase Cloud Messaging (push)** | Notificar nuevas reservas |
| **Deep-linking universal** | Compartir tours + volver desde Wompi con confirmación |
| **Reintentos automáticos** | Escaneo QR fallido en zona con mala señal |
| **Traducción automática** (integrada con backend) | Que el operador solo ingrese español |
| **Integración con Travel Concierge (agente IA)** | Búsqueda asistida del turista — ver [16](16-agentes-ia.md) |

---

## Estado actual del código MAUI vs este alcance

Referencia detallada en [15 — Estado del MVP mobile](15-mvp-mobile-estado.md).

Resumen ejecutivo alineado con este alcance:

| Categoría | Cuenta | Ejemplos |
|-----------|:------:|----------|
| ✅ Ya construido y alineado | La mayoría | Auth, Explore, Detail, Cart, Checkout, MyTrips, Perfil (turista); Dashboard, Tours, Reservations, QR Scanner, KYB (proveedor); Tour Form, Schedule Template Form (proveedor) |
| ⚠️ Construido pero falta refinar UX mobile | Wizard tour, Schedule template | Requieren autosave, compresión de imágenes, upload en background |
| ❌ Falta construir | Reseña (crear con cámara), Wishlist, Créditos (UI dedicada), Crear operarios, Responder reseñas, Payouts | Servicios backend existen; falta UI mobile |
| ❌ Faltan features "solo mobile" | Push, geo, deep-link, offline, widget | Ninguna implementada |

---

## Backoffice (aclaración)

El **backoffice sigue siendo exclusivamente web**. No hay planes de portarlo a mobile.

Racional:
- Es un usuario de oficina (Tourya administra).
- Volúmenes de datos altos, requieren tablas grandes.
- Tareas administrativas complejas: aprobar KYB con revisión de documentos, ajustar comisiones por rango de fechas, ver reportes financieros.

---

## Referencias

- [01 — Visión y negocio](01-vision-y-negocio.md) — origen de la meta original.
- [10 — Mobile spec](10-mobile-spec.md) — estructura técnica actual del MAUI.
- [15 — Estado del MVP mobile](15-mvp-mobile-estado.md) — gap analysis granular.
- [16 — Agentes IA](16-agentes-ia.md) — Travel Concierge referenciado en el alcance del turista.

---

## Changelog del documento

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 2026-06-28 | Versión inicial — propuesta de matizar la iso-funcionalidad; recomendaba dejar crear tour, schedule y operarios como web-only |
| 2.0 | 2026-07-06 | Reescritura completa con alcance definido por Luis. Corrección del contexto: los operadores en San Andrés están en la calle, no en oficina → el mobile SÍ debe cubrir crear tour, schedule y operarios. Se mantiene backoffice como web-only |
