# 15 — Estado del MVP mobile vs alcance

Análisis granular del código actual de `tourya-mobile` (MAUI Android) contra el alcance funcional definido en [14 — Web vs Mobile](14-gap-web-mobile.md).

> **Nota 2026-07-06**: este documento se actualizó tras la reescritura del doc 14 con el alcance definido por Luis. La categorización "construido pero cuestionable" se removió — las pantallas de crear tour, schedule template y schedule calendar **están alineadas con el alcance**.

> **Objetivo**: dimensionar qué falta, qué sobra y en qué invertir a continuación para tener una app coherente con la posición estratégica acordada.

---

## Contexto

- **Repo local**: `D:/Users/Usuario/source/repos/tourya/tourya-mobile/`
- **Último commit local**: `0bc518f` — 2026-04-10 — *"refactor: UI polish for Login, Profile, Cart, Reservation Detail"*
- **Rama**: `master` (única)
- ⚠️ **Sin remoto Git** — el código solo existe localmente. **MO-00 (crear repo GitHub) pospuesto por decisión operativa 2026-07-13 pero sigue siendo riesgo abierto.**
- **Stack**: .NET MAUI 10 (Android only), CommunityToolkit.Mvvm, Syncfusion 33.1.46, ZXing.Net.Maui.

### Cambios post-último-commit (no versionados)

Ninguno de estos cambios está en el repo — viven solo en el disco local:

- **2026-07-13 — MO-01**: `Constants.cs:5` → URL backend actualizada de `http://44.203.38.85:8088/api/v1/` (IP AWS EC2 legacy apagada) a `http://34.160.22.16/api/v1/` (LB GCP, coherente con el frontend web). Deuda: migrar a HTTPS cuando se agregue forwarding rule 443 al LB dev.
- **2026-07-13 — MO-20**: auditoría reveló que el 90% del "Responder reseñas desde ProviderReviewsPage" ya estaba implementado (UI + ViewModel + servicio). Fix del único bug latente: `ReviewService.ReplyToReviewAsync` mandaba `application/json` cuando el backend `PATCH /public/save/review/{reviewId}` consume `multipart/form-data`. Solución: nuevo `PatchMultipartAsync` en `ApiService` + refactor de `ReplyToReviewAsync` armando el multipart con part `reviewData`. Sin `answerFiles` (UI actual es solo texto).
- **2026-07-14 — MO-10**: nueva UI de crear reseña. `CreateReviewPage` + `CreateReviewViewModel` con rating 1-5 estrellas tappables, editor de comentario, `MediaPicker` para cámara/galería, máx 5 fotos, envío multipart con `reviewData` + `files[]`. Nuevo botón "Escribir reseña" en `ReservationDetailPage` visible solo si `DeliveryStatus = DELIVERED`. Ruta `create-review` registrada en `AppShell`. Permisos Android `READ_MEDIA_IMAGES` + `READ_EXTERNAL_STORAGE (maxSdk=32)`. Sin `partial void OnIsBusyChanged` (IsBusy vive en BaseViewModel) — se suscribe a `PropertyChanged` en el ctor.
- **2026-07-15 — MO-11**: nueva `WishlistPage` con lista paginada (pull-to-refresh + infinite scroll + empty state con emoji), botón remover con rollback si falla. Nuevo servicio `WishlistService` cliente de `GET/POST/DELETE /wishlist` + `POST /wishlist/search` (autenticados). Botón ❤️/🤍 en el header de `TourDetailPage` con toggle optimista (rollback en error, alert al usuario si 401). Nuevo overload `ApiService.DeleteAsync<T>(endpoint, body)` porque DELETE con body no es nativo de HttpClient. Registrado como 5to tab del tourist TabBar ("Deseos", posición 3 entre Carrito y Mis Viajes). Sin asset dedicado — corazón inline en el título del header.
- **2026-07-15 — MO-22 + MO-23 + MO-24 (combo operarios)**: bloque coherente que cierra el gap de "Crear operarios / editar / reset password" pedido explícitamente por Luis. Piezas: `ProviderOperatorService` (cliente de los 5 endpoints /provider/users*), `OperatorsPage` (lista con avatar, badge de estado, tour principal, FAB "+", ActionSheet en tap con opciones Editar/Reset), `OperatorFormPage` (crear/editar unificado, multi-select de tours con CheckBox + Picker de tour principal), `ResetOperatorPasswordPage` (form simple). MO-23 integrado via `Share.Default.RequestAsync` — al crear u operar reset, se ofrece compartir credenciales por WhatsApp/otras apps (share sheet nativo). Acceso desde nuevo tile "👥 Operarios" en el `DashboardPage` del provider (no se sumó tab porque los 5 tabs actuales ya están al límite). Sin nueva tab en el TabBar del provider.

---

## Inventario de lo construido

### Vistas (25 páginas .xaml)

**Auth (4)**: `LoginPage`, `RegisterPage`, `ForgotPasswordPage`, `RoleSelectorPage`.

**Turista (8)**: `ExplorePage`, `TourDetailPage`, `CartPage`, `CheckoutPage`, `PaymentConfirmationPage`, `MyTripsPage`, `ReservationDetailPage`, `ProfilePage`.

**Provider (13)**: `DashboardPage`, `ProviderToursPage`, `ProviderReservationsPage`, `QrScannerPage`, `ProviderReviewsPage`, `KybStatusPage`, `KybRegistrationPage`, `KybDocumentsPage`, `TourFormPage`, `TourSchedulesPage`, `ScheduleTemplatesPage`, `ScheduleTemplateFormPage`, `ScheduleCalendarPage`.

### Servicios (14)
`ApiService`, `AuthService`, `TourService`, `CartService`, `PaymentService`, `ReservationService`, `ReviewService`, `CreditService`, `ProviderService`, `TourManagementService`, `ScheduleService`, `KybService`, `ConnectivityService`, `I18nService`.

### ViewModels (26 + BaseViewModel)
Uno por cada vista + algunos compartidos como `MyToursViewModel`, `ReservationsViewModel`.

---

## Gap Analysis contra el doc 14

Se marcan las funcionalidades en 2 categorías:

- ✅ **Construido y alineado** con el alcance definido.
- ❌ **Falta construir** — el doc 14 lo pide y no está.
- ⚠️ **Construido pero requiere refinar UX mobile** — está pero necesita mejoras (autosave, compresión, offline, etc.).

### Turista (USER)

| Funcionalidad (doc 14) | Estado mobile | Categoría | Notas |
|-------------------------|:-------------:|:---------:|-------|
| Registro / login (email + Facebook + Google) | `LoginPage`, `RegisterPage` | ✅ | |
| Buscar tours con Travel Concierge (IA) | `ExplorePage` | ⚠️ parcial | Búsqueda con filtros existe, falta integración con agente IA — ver [16](16-agentes-ia.md) |
| Ver detalle de tour + galería | `TourDetailPage` | ✅ | |
| Agregar al carrito | `CartPage` | ✅ | |
| Checkout + Wompi WebView | `CheckoutPage`, `PaymentConfirmationPage`, `WompiHelper` | ✅ | |
| Ver mis reservas + QR | `MyTripsPage`, `ReservationDetailPage` | ✅ | |
| Cancelar reserva | En `ReservationDetailPage` | ✅ | |
| Reagendar reserva | En `ReservationDetailPage` (a verificar) | ⚠️ verificar | Confirmar que el flujo esté completo con las 3 casuísticas (igual/menor/mayor precio) |
| Dejar reseña (con fotos) | `CreateReviewPage`, `CreateReviewViewModel` | ✅ | MO-10 cerrado 2026-07-14. Cámara + galería, 5 fotos máx, multipart. Falta validar en device |
| Ver / gestionar créditos (usar, transferir) | `CreditService` | ⚠️ parcial | Servicio existe, pero falta UI dedicada de créditos (saldo, historial, transferir) |
| Wishlist (lista de deseos) | `WishlistPage`, `WishlistViewModel`, `WishlistService` | ✅ | MO-11 cerrado 2026-07-15. Tab dedicado + toggle ❤️ desde `TourDetailPage` |
| Perfil turista (foto, documento, dirección) | `ProfilePage` | ✅ | |
| Notificaciones push | ❌ | ❌ | No implementado |
| Geolocalización ("cerca de mí") | ❌ | ❌ | `Syncfusion.Maui.Maps` importado pero sin usar |
| Deep-linking | ❌ | ❌ | No implementado |

### Operador titular (PROVIDER)

| Funcionalidad (doc 14) | Estado mobile | Categoría | Notas |
|-------------------------|:-------------:|:---------:|-------|
| Dashboard (ingresos, tours, KPIs) | `DashboardPage`, `DashboardViewModel` | ✅ | |
| Crear tour (wizard) | `TourFormPage` (6+ steps) | ⚠️ refinar UX | Existe. Necesita: autosave/borradores, compresión de imágenes, upload en background, dictado de voz opcional |
| Editar tour | `TourFormPage` | ⚠️ refinar UX | Idem |
| Gestionar schedule (plantillas + slots + precios) | `ScheduleTemplateFormPage`, `ScheduleCalendarPage`, `TourSchedulesPage` | ⚠️ refinar UX | Existe. Vale la pena: vistas día/semana/mes claras, copiar precios de otro tour |
| Ver / gestionar reservas | `ProviderReservationsPage` | ✅ | |
| Confirmar reserva (QR scanner) | `QrScannerPage`, ZXing | ✅ 🎯 | Valor core del mobile |
| Marcar reserva manualmente | En `ProviderReservationsPage` | ⚠️ verificar | Confirmar que exista el fallback manual del QR |
| Ver reseñas | `ProviderReviewsPage` | ✅ | |
| **Responder reseñas** | ❌ | ❌ | Existe la view de reseñas, falta el flujo de respuesta |
| Ver payouts + comprobantes | ❌ | ❌ | Sin `PayoutsPage` — falta construir |
| **Crear operarios (`PROVIDER_OPERATOR`)** | `OperatorsPage`, `OperatorFormPage`, `ProviderOperatorService` | ✅ | MO-22 cerrado 2026-07-15. Share sheet nativo (MO-23) al finalizar create + reset |
| Editar / reasignar operarios | `OperatorFormPage` (mismo form) | ✅ | Multi-select tours + tour principal en el mismo form |
| Resetear password operarios | `ResetOperatorPasswordPage` | ✅ | MO-24 cerrado 2026-07-15. Share sheet al finalizar |
| Panel KYB / documentos | `KybStatusPage`, `KybRegistrationPage`, `KybDocumentsPage` | ✅ | Buena implementación |
| Notificaciones push | ❌ | ❌ | **Prioridad alta según doc 14** |
| Modo campo / offline | ❌ | ❌ | **Prioridad alta según doc 14** |

### Operario (`PROVIDER_OPERATOR`)

| Funcionalidad (doc 14) | Estado mobile | Categoría | Notas |
|-------------------------|:-------------:|:---------:|-------|
| Login con clave temporal + cambio | Compartido con LoginPage | ⚠️ verificar | Confirmar UI de "mustChangePassword" al hacer login |
| Ver reservas de tours asignados | `ProviderReservationsPage` (compartida con PROVIDER) | ✅ | Backend filtra por scope; verificar UX específica |
| Escanear QR | `QrScannerPage` | ✅ 🎯 | Core del rol |
| Confirmar reserva manualmente | En `ProviderReservationsPage` | ⚠️ verificar | |
| Notificaciones de nueva reserva | ❌ | ❌ | Push pendiente |
| Widget / home "próximas reservas del día" | ❌ | ❌ | |

### Backoffice / ADMIN

| Funcionalidad (doc 14) | Estado mobile | Categoría |
|-------------------------|:-------------:|:---------:|
| Todo el backoffice | Sin implementar | ✅ **correcto** — se mantiene web-only |

---

## Features "solo mobile" del doc 14 (donde el móvil suma valor real)

Recap de lo que el doc 14 propone como prioridad de inversión mobile:

| Feature | Estado actual | Prioridad |
|---------|:-------------:|:---------:|
| Escaneo QR (operario, operador) | ✅ hecho | — |
| Notificaciones push (turista, operador, operario) | ❌ | **Alta** |
| Cámara para reseñas | ⚠️ falta UI de crear reseña | **Alta** |
| Geolocalización ("cerca de mí", "cómo llegar") | ❌ | **Alta** |
| Deep-linking (compartir tours por WhatsApp) | ❌ | Media |
| Wallet integration (Apple Pay / Google Pay vía Wompi) | ❌ | Media |
| Widget "próxima reserva" | ❌ | Baja |
| Modo offline (reservas del día para operario) | ❌ | Media-Alta |
| Escaneo OCR docs KYB | ❌ | Baja (upload manual ya funciona) |
| Compartir en redes | ❌ | Baja |

---

## Tech debt actual del mobile

| # | Item | Impacto |
|---|------|---------|
| 1 | Sin remoto Git (solo local) | ⚠️ Alto — pérdida de código ante falla de disco |
| 2 | URL API hardcoded a la IP legacy de AWS (`44.203.38.85:8088`) | ⚠️ Alto — la app real de producción apuntaría al lugar equivocado |
| 3 | Syncfusion License key sin registrar | Medio — banner de trial en producción |
| 4 | `ForgotPasswordPage` sin endpoint backend correspondiente | Medio — feature muerta |
| 5 | iOS no soportado (solo Android) | Bajo (decisión de foco) |
| 6 | Sin CI/CD para builds y distribución (APK manual, sin Play Store) | Medio — proceso de release informal |
| 7 | Sin analytics / crash reporting | Medio — visibilidad cero en producción |
| 8 | Sin política de privacidad / términos formalizados | Bloqueante para Play Store |

---

## Diagnóstico ejecutivo

### Lo que está bien alineado con el doc 14

- El **operario** ya tiene lo esencial: escaneo QR + ver sus reservas asignadas.
- El **turista** cubre el flujo E2E completo (buscar → detalle → cart → checkout → mis reservas + QR).
- El **operador** cubre lo core en campo (dashboard rápido, reservas, QR, reseñas view).
- El **KYB** está bien portado para que el operador pueda subir docs desde móvil.

### Lo que falta construir según el alcance definido por Luis

**Para el turista**:
1. **UI de creación de reseña** con cámara (el `ReviewService` existe, falta la UI).
2. **Wishlist** (lista de deseos) — pantalla dedicada.
3. **UI de gestión de créditos** — saldo, historial de créditos, transferir a otro turista.
4. **Integración con Travel Concierge (agente IA)** — ver [16](16-agentes-ia.md).

**Para el proveedor**:
5. **Responder reseñas** (existe la vista, falta el flujo).
6. **Ver payouts + comprobantes** (`PayoutsPage`).
7. **Crear operarios (`PROVIDER_OPERATOR`)** — formulario + envío de contraseña temporal por WhatsApp / SMS.
8. **Editar / reasignar operarios**.
9. **Resetear password de operarios**.

**Features "solo mobile" que aún faltan**:
10. **Notificaciones push** (FCM) — turista, operador, operario.
11. **Geolocalización** — "tours cerca de mí" en Explore, "cómo llegar" en TourDetail (`Syncfusion.Maui.Maps` ya importado).
12. **Modo offline** para operario (cache de reservas del día).
13. **Deep-linking** — compartir tours por WhatsApp que abre la app.
14. **Widget "próxima reserva"** (para turista y operario).

### Lo que existe pero necesita refinar UX mobile

- **Wizard de creación de tour** — funciona, pero requiere autosave / borradores, compresión de imágenes en cliente, upload en background, y opcionalmente dictado de voz para descripciones.
- **Configuración de schedule** — funciona, pero vale la pena una vista de calendario más clara y "copiar precios de otro tour".

### Lo que urge arreglar antes de cualquier feature nueva

1. **Crear repo `tourya-mobile` en GitHub** y subir el código. Sin esto se corre riesgo real de pérdida.
2. **Actualizar la URL del backend** a `https://tourya.co/api/v1/` (o la variable equivalente) — hoy apunta a AWS legacy.
3. **Registrar la Syncfusion License** (o migrar a controles no-Syncfusion si no se compra licencia).
4. **Configurar CI/CD** para build de APK firmado (GitHub Actions + Keystore + Play Store internal track).

---

## Roadmap sugerido de mobile (próximos ciclos)

### Ciclo 0 — Higiene (obligatorio)
1. Subir a GitHub como `tourya-mobile`.
2. Actualizar URL backend + variables por entorno.
3. Syncfusion License.
4. CI/CD básico → APK firmado en Play Store internal track.

### Ciclo 1 — Cerrar brechas de la app turista
1. UI de creación de reseña con cámara.
2. Wishlist en tab del turista.
3. UI dedicada de créditos (saldo, historial, transferir).

### Ciclo 2 — Cerrar brechas de la app proveedor
1. Responder reseñas desde la vista de reseñas.
2. Ver payouts + comprobantes (`PayoutsPage`).
3. **Crear / editar / gestionar operarios** (formulario + compartir clave temporal por WhatsApp).

### Ciclo 3 — Refinar UX de creación/config del proveedor
1. Autosave / borradores en el wizard de tour.
2. Compresión de imágenes en cliente + upload en background.
3. Vista de calendario mejorada en schedule.
4. Copiar precios entre tours.

### Ciclo 4 — Valor incremental móvil (features "solo mobile")
1. Push notifications (FCM) para turista, operador, operario.
2. Geolocalización: "tours cerca de mí" + "cómo llegar al punto de encuentro".
3. Deep-linking + compartir tours por WhatsApp.

### Ciclo 5 — Robustecer el rol operario
1. Modo offline (cache de reservas del día).
2. Widget de "próxima reserva".
3. Reforzar vista de reservas del operario (agrupación por hora).

### Ciclo 6 — Integración con agentes IA
1. Travel Concierge en `ExplorePage` (según [16](16-agentes-ia.md)).
2. Otros agentes según el alcance que defina Luis.

---

## Métricas para trackear post-lanzamiento

Para validar el alcance del doc 14 con datos:

| Métrica | Qué mide |
|---------|----------|
| % de reservas creadas desde mobile vs web | Confirmar que el móvil es primario para el turista |
| % de tours creados desde mobile vs web (por operador) | Validar si los operadores usan mobile para configurar (esperado: bajo) |
| % de reservas confirmadas por QR vs manual (mobile) | Uso real del QR scanner |
| Tasa de opt-in de push notifications | Salud del canal de notificaciones |
| Uso de "cerca de mí" | Validar el valor de la geolocalización |
| Retención D7 / D30 mobile vs web | Comportamiento diferenciado |

---

## Referencias

- [14 — Gap Web vs Mobile](14-gap-web-mobile.md) — framework y decisiones estratégicas.
- [10 — Mobile spec](10-mobile-spec.md) — estructura técnica del MAUI (pantallas, VMs, servicios).
- [01 — Visión y negocio](01-vision-y-negocio.md) — meta original de iso-funcionalidad y su matización.
