# 14 — Análisis Gap Web vs Mobile

Comparativa funcional entre la web (Angular 19) y la app móvil (MAUI Android). Este documento define **qué debe existir en cada plataforma y por qué** — para evitar duplicar trabajo que no aporta valor.

---

## Posición estratégica

> En el doc [01 — Visión y negocio](01-vision-y-negocio.md) se planteó como meta que "Android y iOS deben brindar la misma funcionalidad que la web". Este documento propone **matizar esa meta**: **no todas las funciones deben duplicarse** — cada plataforma sirve a un contexto de uso distinto.

### Principio guía

**La app móvil debe capitalizar lo que solo el móvil hace bien**; para el resto, la web es más eficiente en desarrollo, mantenimiento y experiencia del usuario.

**Por qué no duplicar todo**:

| Argumento | Detalle |
|-----------|---------|
| **Costo de desarrollo** | Un feature en mobile toma 2-3× el tiempo que en web (múltiples pantallas, gestión de estado, ciclos de release, testing en dispositivos reales) |
| **Mantenibilidad** | Duplicar UI = duplicar bugs, duplicar pruebas, duplicar QA |
| **UX apropiada** | Formularios largos con muchos campos (crear un tour completo, configurar KYB) son deficientes en móvil |
| **Frecuencia de uso** | Backoffice y configuración pesada se usan pocas veces al mes; no vale la fricción de desarrollarlas para móvil |
| **Benchmarks del sector** | Airbnb, Booking, GetYourGuide, Viator NO duplican en mobile todo el panel del host/operador — mantienen mobile enfocado en compra + fulfillment |

---

## Contextos de uso

Cada rol tiene un contexto de uso distinto y una plataforma primaria natural:

| Rol | Contexto primario | Plataforma primaria | Plataforma secundaria |
|-----|-------------------|---------------------|-----------------------|
| **Turista (USER)** | En movimiento, en la calle, en el hotel, en el sitio turístico | **Móvil** | Web (para búsqueda pesada / pre-viaje) |
| **Operario (PROVIDER_OPERATOR)** | En el sitio, en el bote, atendiendo grupos | **Móvil** | — |
| **Operador titular (PROVIDER)** | En oficina, con tiempo, atendiendo negocio | **Web** | Móvil (para consultas rápidas / notificaciones) |
| **Backoffice (ADMIN / BACKOFFICE_OP)** | En oficina, tareas administrativas | **Web** | — |

---

## Matriz Web vs Mobile por funcionalidad

Convenciones:
- ✅ Debe existir.
- ❌ No debe existir.
- 🎯 Es la plataforma primaria (mejor experiencia esperada).
- ⚡ Nice-to-have (existe hoy o se puede considerar en roadmap, sin ser bloqueante).

### Turista (USER)

| Funcionalidad | Web | Mobile | Justificación |
|---------------|:---:|:------:|---------------|
| Registro / login (email + social) | ✅ | ✅ 🎯 | Turista suele registrarse en el momento con el móvil |
| Buscar tours (filtros pesados) | ✅ 🎯 | ✅ | Filtrado avanzado más cómodo en web; mobile con filtros simplificados |
| Ver detalle de tour + galería | ✅ | ✅ 🎯 | Consumo visual — móvil es ideal |
| Agregar al carrito | ✅ | ✅ | Ambos |
| Checkout + Wompi | ✅ | ✅ 🎯 | En el momento, con el móvil |
| Ver mis reservas + QR | ✅ | ✅ 🎯 | El QR se muestra en el sitio → mobile obligatorio |
| Cancelar reserva | ✅ | ✅ | Ambos |
| Reagendar reserva | ✅ | ✅ | Ambos |
| Dejar reseña (con fotos) | ✅ | ✅ 🎯 | Móvil ideal (cámara nativa, subida directa) |
| Ver / usar créditos | ✅ | ✅ | Ambos |
| **Transferir créditos** | ✅ | ⚡ | Es acción esporádica; no bloqueante en móvil |
| **Wishlist** | ✅ | ✅ | Sí en mobile para "guardar para luego" |
| **Perfil turista (foto, documento, dirección)** | ✅ | ✅ | Móvil ideal para subir foto de documento con cámara |
| **Solicitar convertirse en Provider (KYB)** | ✅ 🎯 | ⚡ | Formulario largo con muchos documentos → mejor en web |

### Operador titular (PROVIDER)

| Funcionalidad | Web | Mobile | Justificación |
|---------------|:---:|:------:|---------------|
| Dashboard (ingresos, tours, KPIs) | ✅ 🎯 | ✅ | Consulta rápida en móvil; análisis profundo en web |
| **Crear tour (wizard 6+ pasos)** | ✅ 🎯 | ❌ | 12+ campos multilingües, galería, itinerario, FAQ, políticas → inviable en móvil |
| **Editar tour** | ✅ 🎯 | ❌ | Idem |
| **Crear plantilla de horarios** | ✅ 🎯 | ❌ | Configuración compleja con días de semana, slots, precios por ageType |
| **Configurar precios (`providerPrice` por slot)** | ✅ 🎯 | ❌ | Configuración pesada; se hace una vez al arrancar el tour |
| **Ver / gestionar reservas** | ✅ | ✅ 🎯 | Mobile crítico para operador en campo |
| **Confirmar reserva (QR scanner)** | ⚡ | ✅ 🎯 | **Mobile obligatorio** — cámara para escanear QR |
| **Marcar reserva manualmente como entregada** | ✅ | ✅ | Ambos |
| **Ver / responder reseñas** | ✅ 🎯 | ⚡ | Puede escribirse mejor en web; respuestas rápidas en móvil ok |
| **Ver payouts + comprobantes** | ✅ 🎯 | ✅ | Consulta rápida en móvil, detalles y descarga en web |
| **Crear operarios (`PROVIDER_OPERATOR`)** | ✅ 🎯 | ❌ | Acción esporádica de gestión de equipo → web |
| **Editar / reasignar operarios** | ✅ 🎯 | ❌ | Idem |
| **Resetear password de operarios** | ✅ 🎯 | ❌ | Idem |
| **Panel KYB / documentos** | ✅ 🎯 | ⚡ | Subir docs desde móvil sí; formulario largo en web |
| **Notificaciones push** (nueva reserva, cancelación) | ❌ | ✅ 🎯 | **Ventaja natural del mobile** |
| **Modo campo / offline** (ver reservas del día sin señal) | ❌ | ✅ 🎯 | **Ventaja natural del mobile** — pendiente en roadmap |

### Operario (PROVIDER_OPERATOR)

| Funcionalidad | Web | Mobile | Justificación |
|---------------|:---:|:------:|---------------|
| Login (con clave temporal, obligar cambio) | ✅ | ✅ 🎯 | Móvil primario — el operario trabaja en campo |
| **Ver reservas de tours asignados** | ✅ | ✅ 🎯 | Ideal en móvil |
| **Escanear QR** | ❌ | ✅ 🎯 | **Solo mobile** — cámara nativa |
| **Confirmar reserva manualmente** | ✅ | ✅ 🎯 | Idem |
| **Notificaciones de nueva reserva** | ❌ | ✅ 🎯 | Push |
| **Ver "próximas reservas del día"** | ⚡ | ✅ 🎯 | Widget / home mobile |

> El operario es un usuario **casi 100% mobile-first**.

### Backoffice / ADMIN

| Funcionalidad | Web | Mobile | Justificación |
|---------------|:---:|:------:|---------------|
| Todo el backoffice | ✅ 🎯 | ❌ | Aprobar proveedores, aprobar tours, ver reportes, ajustar comisiones, subir comprobantes de pago, gestionar reportes DIMAR → **web-only** |

> El backoffice **NUNCA** debe portarse a móvil. Es trabajo de escritorio con tablas, filtros y decisiones pesadas.

---

## Funcionalidades que solo hacen sentido en Mobile

Estas son las oportunidades de **valor incremental** de la app móvil — es donde vale la pena invertir en desarrollo mobile:

| Feature | Rol | Justificación |
|---------|-----|---------------|
| **Escaneo QR** | Operario, Operador | Cámara nativa. Confirmar reservas en sitio |
| **Notificaciones push** | Turista, Operador, Operario | Cerrar el loop de comunicación |
| **Cámara para reseñas** | Turista | Foto natural y rápida post-experiencia |
| **Geolocalización** | Turista | Búsqueda "tours cerca de mí", "cómo llegar al punto de encuentro" |
| **Deep-linking** | Turista | Compartir tours por WhatsApp con link → abre app o web fallback |
| **Wallet integration (Apple Pay, Google Pay)** | Turista | En Wompi soportado; UX nativa mejor |
| **Widget de "próxima reserva"** | Turista, Operario | Ver el QR del tour de hoy desde la pantalla de bloqueo |
| **Modo offline** (reservas del día) | Operario | Trabajar sin señal en la isla / en el bote |
| **Escaneo OCR de documentos KYB** | Operador (onboarding) | Cámara para foto de RUT / cédula (opcional, útil pero no bloqueante) |
| **Compartir en redes sociales** | Turista | Compartir un tour reseñado |

---

## Recomendaciones

### Para el equipo de producto

1. **No perseguir la iso-funcionalidad como meta**. Aceptar que web y móvil se complementan.
2. **Priorizar en mobile solo las funcionalidades del cuadro "solo mobile"** de arriba — es ahí donde el móvil suma valor real.
3. **En roadmap mobile**: enfocar los próximos ciclos en:
   - Push notifications (FCM).
   - Geolocalización + búsqueda "cerca de mí".
   - Modo offline para operarios.
   - Deep-linking.
4. **NO invertir tiempo mobile en**:
   - Wizard de creación de tour.
   - Configuración de horarios y precios.
   - Panel de payouts detallado.
   - Gestión de sub-usuarios provider.
   - Backoffice.

### Para el equipo de desarrollo

- Si un feature vale ambos → siempre construir en web primero, después portar a mobile.
- Mobile debe consumir la **misma API** — nunca crear endpoints exclusivos móviles.
- Cada feature nuevo pregunta: "¿realmente aporta valor en el contexto mobile?" antes de invertir.

### Para el operador (transparencia comercial)

Comunicar claramente al operador:
- **Móvil es tu herramienta de campo**: confirmar reservas, ver la agenda del día, responder rápido.
- **Web es tu herramienta de oficina**: crear/editar tours, configurar precios, gestionar tu equipo, ver reportes.

---

## Matriz simplificada por rol y plataforma primaria

```
                    Web         Mobile      Ejemplo de tarea
                    ─────       ──────      ────────────────
Turista             🎯 pre-viaje 🎯 en-viaje  Buscar / comprar tour, ver QR
Operario             ❌         🎯 100%     Escanear QR
Operador (dueño)    🎯 config   ⚡ campo   Crear tour (web), confirmar (mobile)
Backoffice / ADMIN  🎯 100%     ❌         Aprobar tour, ajustar %
```

---

## Estado actual (código a 2026-06-28)

El mobile actual (MAUI) **YA está bien alineado** con esta propuesta:

✅ Cubre bien:
- Explorar / detalle / carrito / checkout / mis reservas / perfil (turista).
- Dashboard, tours, reservas, QR scanner, reseñas (provider).
- KYB registration + documents (provider).
- Tour form wizard + schedule template + schedule calendar (provider).

⚠️ Cubre parcialmente (candidatos a re-evaluar según esta propuesta):
- Wizard de tour completo → **cuestionable**: es funcionalmente pesado y en móvil la UX no es óptima. Podría restringirse a "editar tour existente" y forzar creación desde web.
- Configuración detallada de schedule → idem.

❌ No cubre (según esta propuesta, **no debe cubrirse** en mobile):
- Backoffice.
- Gestión de sub-usuarios provider.
- Transferencia de créditos (esporádica).
- Reset de password de operarios.

📌 **Faltantes que sí valdría la pena invertir** (según esta propuesta):
- Push notifications.
- Geolocalización.
- Deep-linking.
- Widget / atajo de "próxima reserva".
- Modo offline básico para operarios.

---

## Preguntas para alinear con Luis

1. ¿Acepta el cambio de meta ("iso-funcionalidad" → "cada plataforma para lo que hace mejor")?
2. ¿Alguno de los "No debe cubrirse en mobile" es innegociable por experiencia con operadores actuales?
3. Confirmar priorización de features **mobile-only** para el próximo roadmap (push, geo, offline).

---

## Referencias

- [01 — Visión y negocio](01-vision-y-negocio.md) — visión general y roadmap.
- [10 — Mobile spec](10-mobile-spec.md) — estructura técnica actual del MAUI.
- [09 — API design](09-api-design.md) — API compartida por ambas plataformas.
