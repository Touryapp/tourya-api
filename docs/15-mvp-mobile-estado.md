# 15 — Estado del MVP mobile vs propuesta

Análisis granular del código actual de `tourya-mobile` (MAUI Android) contra la matriz funcional propuesta en [14 — Gap Web vs Mobile](14-gap-web-mobile.md).

> **Objetivo**: dimensionar qué falta, qué sobra y en qué invertir a continuación para tener una app coherente con la posición estratégica acordada.

---

## Contexto

- **Repo local**: `D:/Users/Usuario/source/repos/tourya/tourya-mobile/`
- **Último commit**: `0bc518f` — 2026-04-10 — *"refactor: UI polish for Login, Profile, Cart, Reservation Detail"*
- **Rama**: `master` (única)
- ⚠️ **Sin remoto Git** — el código solo existe localmente.
- **Stack**: .NET MAUI 10 (Android only), CommunityToolkit.Mvvm, Syncfusion 33.1.46, ZXing.Net.Maui.

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

Se marcan las funcionalidades en 3 categorías:

- ✅ **Construido y alineado** con la propuesta.
- ⚠️ **Construido pero cuestionable** — el doc 14 dice que no debería estar en mobile o que debería restringirse.
- ❌ **Falta construir** — el doc 14 lo pide y no está.

### Turista (USER)

| Funcionalidad (doc 14) | Estado mobile | Categoría | Notas |
|-------------------------|:-------------:|:---------:|-------|
| Registro / login (email + social) | `LoginPage`, `RegisterPage` | ✅ | |
| Buscar tours (filtros simplificados) | `ExplorePage` | ✅ | |
| Ver detalle de tour + galería | `TourDetailPage` | ✅ | |
| Agregar al carrito | `CartPage` | ✅ | |
| Checkout + Wompi WebView | `CheckoutPage`, `PaymentConfirmationPage`, `WompiHelper` | ✅ | |
| Ver mis reservas + QR | `MyTripsPage`, `ReservationDetailPage` | ✅ | |
| Cancelar reserva | En `ReservationDetailPage` | ✅ | |
| Reagendar reserva | En `ReservationDetailPage` (a verificar) | ⚠️ verificar | Confirmar que el flujo esté completo con las 3 casuísticas (igual/menor/mayor precio) |
| Dejar reseña (con fotos) | ? | ❌ falta | No hay `ReviewCreatePage` ni `WriteReviewViewModel` — el `ReviewService` existe pero no la UI |
| Ver / usar créditos | `CreditService` | ⚠️ parcial | Servicio existe, pero no hay `CreditsPage` visible; se usa dentro del checkout |
| Transferir créditos | ❌ | ❌ | Sin UI |
| Wishlist | ❌ | ❌ | Sin `WishlistPage` — puede convivir con el USER en su tab |
| Perfil turista (foto, documento, dirección) | `ProfilePage` | ✅ | |
| Solicitar convertirse en Provider (KYB) | KYB solo desde tab provider | ⚠️ | Faltaría flujo desde USER si el doc lo permite (el doc 14 lo marca ⚡ nice-to-have) |
| Notificaciones push | ❌ | ❌ | No implementado |
| Geolocalización ("cerca de mí") | ❌ | ❌ | `Syncfusion.Maui.Maps` importado pero sin usar |
| Deep-linking | ❌ | ❌ | No implementado |

### Operador titular (PROVIDER)

| Funcionalidad (doc 14) | Estado mobile | Categoría | Notas |
|-------------------------|:-------------:|:---------:|-------|
| Dashboard (ingresos, tours, KPIs) | `DashboardPage`, `DashboardViewModel` | ✅ | |
| Crear tour (wizard) | `TourFormPage` (6+ steps) | ⚠️ **cuestionable** | El doc 14 dice **NO** hacer wizard en mobile — inviable UX con 12+ campos multilingües |
| Editar tour | `TourFormPage` | ⚠️ cuestionable | Idem |
| Crear plantilla de horarios | `ScheduleTemplateFormPage` | ⚠️ cuestionable | Doc 14 recomienda web-only |
| Configurar precios (providerPrice por slot) | En `ScheduleTemplateFormPage` | ⚠️ cuestionable | Idem |
| Ver / gestionar reservas | `ProviderReservationsPage` | ✅ | |
| Confirmar reserva (QR scanner) | `QrScannerPage`, ZXing | ✅ 🎯 | **Correcto — es el valor core del mobile del operador** |
| Marcar reserva manualmente | En `ProviderReservationsPage` | ⚠️ verificar | Confirmar que exista el flujo manual como fallback del QR |
| Ver / responder reseñas | `ProviderReviewsPage` | ⚠️ parcial | Ver sí; **responder falta verificar** |
| Ver payouts + comprobantes | ❌ | ❌ | No hay `PayoutsPage` |
| Crear operarios (`PROVIDER_OPERATOR`) | ❌ | ❌ **correcto según doc 14** | Doc 14 dice: gestión de sub-usuarios solo en web |
| Editar / reasignar operarios | ❌ | ❌ **correcto** | Idem |
| Resetear password operarios | ❌ | ❌ **correcto** | Idem |
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
| Todo el backoffice | Sin implementar | ✅ **correcto según doc 14** (no debe hacerse mobile) |

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

### Lo que se construyó pero según el doc 14 sobra

- **Wizard de creación / edición de tour** en mobile (`TourFormPage`).
- **Plantillas de horarios** y **configuración de precios** en mobile (`ScheduleTemplateFormPage`, `ScheduleCalendarPage`, `TourSchedulesPage`).

> Estas pantallas ya existen (esfuerzo hundido). Opciones a discutir:
> - **A**) **Mantenerlas** y considerar el debate cerrado — quedan como plus para operadores muy movilizados.
> - **B**) **Simplificarlas**: dejar solo lectura/consulta rápida ("ver mis tours", "ver mis horarios"), quitar edición y forzar a web.
> - **C**) **Deprecarlas** en el próximo ciclo para reducir superficie de mantenimiento.

### Lo que falta y aporta valor

1. **UI de creación de reseña** con cámara (turista) — el `ReviewService` existe pero la UI no. Es la brecha más rápida de cerrar.
2. **Notificaciones push** (FCM) — cierra el loop turista ↔ operador ↔ operario.
3. **Geolocalización** — "tours cerca de mí" en `ExplorePage`, "cómo llegar" en `TourDetailPage` con `Syncfusion.Maui.Maps` (ya importado).
4. **Modo offline básico** para el operario — cachear reservas del día para trabajar sin señal.
5. **Deep-linking** — compartir tours por WhatsApp con link que abre la app o cae a la web.
6. **Payouts** (view + comprobantes) para el operador — completa la visibilidad financiera en móvil.
7. **Transferencia de créditos** y **wishlist** para el turista — brechas rápidas de UI.
8. **Widget "próxima reserva"** (opcional, alto ROI en percepción de calidad).

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
3. Transferencia de créditos.

### Ciclo 2 — Valor incremental móvil (features "solo mobile")
1. Push notifications (FCM) para turista y operador.
2. Geolocalización: "tours cerca de mí" + "cómo llegar".
3. Deep-linking.

### Ciclo 3 — Robustecer el rol operario
1. Modo offline (cache de reservas del día).
2. Widget de "próxima reserva".
3. Reforzar la vista de reservas del operario (filtro por tour asignado, agrupación por hora).

### Ciclo 4 — Debate estratégico
1. Decidir A/B/C sobre las pantallas de creación/edición de tour y horarios en mobile.

---

## Métricas para trackear post-lanzamiento

Para validar la propuesta del doc 14 con datos:

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
