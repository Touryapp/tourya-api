# 15 — Estado del MVP mobile vs alcance

Análisis granular del código actual de `tourya-mobile` (MAUI Android) contra el alcance funcional definido en [14 — Web vs Mobile](14-gap-web-mobile.md).

> **Nota 2026-07-06**: este documento se actualizó tras la reescritura del doc 14 con el alcance definido por Luis. La categorización "construido pero cuestionable" se removió — las pantallas de crear tour, schedule template y schedule calendar **están alineadas con el alcance**.

> **Nota 2026-08-13**: se agregó la sección "Estado post ciclo QA agosto" y la matriz "Gap con TCs 007-021" al final del doc. El diagnóstico y roadmap del cuerpo principal siguen vigentes en su lectura estratégica; los items concretos por TC se listan en el nuevo roadmap actualizado 2026-08-13.

> **Nota 2026-08-14**: se agregó la sección "Post-Sprint 3/4a/5a — cierre 2026-08-14" al final. Se actualizó la matriz "Gap Analysis contra el doc 14" con los cierres de Sprint 3 A/B (autosave + upload + calendar + copy prices), Sprint 4a (autoVerify + assetlinks), Sprint 5a (responder reseñas provider) y las 2 deudas nuevas MO-53 (reagendar 3 casuísticas turista) y MO-54 (`mustChangePassword` operario) detectadas por la auditoría Sprint 5b.

> **Nota 2026-08-14b**: se agregó la sección "Post-Sprint 6 — cierre 2026-08-14b" al final. Cierran las 2 deudas registradas por la auditoría Sprint 5b: **MO-54** (`mustChangePassword` operario, Sprint 6a) y **MO-53** (reagendar reserva turista con 3 casuísticas, Sprint 6b). Filas correspondientes de la matriz "Gap Analysis contra el doc 14" pasan ❌ → ✅. Se retiran de la lista "Deudas nuevas registradas por auditoría Sprint 5b" (ahora vacía). Adicional: WompiHelper cleanup (cierre técnico chico del hardening Sprint 4a).

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
- **2026-07-15 — MO-31 + MO-34 (refinamiento provider mobile)**: (1) **MO-31**: nuevo NuGet SkiaSharp 3.116.1 + `IImageCompressionService` (redimensiona a 1920px max lado largo, re-encode JPEG 80). Integrado en `CreateReviewViewModel.AddPickedImageAsync` — foto de cámara/galería se comprime antes de guardarse en la lista de picks. Reducción típica: 3-8 MB → 300-800 KB (~85% menos). Fail-safe: si SkiaSharp no puede decodificar, devuelve el original. TourFormPage no sube imágenes desde mobile (grep vacío); KYB usa FilePicker para PDFs (compresión no aplica). (2) **MO-34**: cambio de scope respecto al plan inicial — el response `/tour-schedules/templates` del backend NO incluye `tourId`, entonces no se puede filtrar por tour sin tocar backend. Cambiado a "Copiar de otra plantilla" que muestra TODAS las plantillas del provider (mejor UX igual — el provider puede tener varias plantillas por tour como "fin de semana" vs "semana"). Botón "📋 Copiar" en `ScheduleTemplateFormPage` al lado de "+ Slot" → `DisplayActionSheet` con label + días → confirm reemplazo si hay slots existentes → aplica slots+precios de la fuente.
- **2026-07-17 — MO-50 + MO-51 (operario mobile)**: (1) **MO-50**: nuevo `IReservationCacheService` con JSON en `FileSystem.CacheDirectory/reservations_today.json` (sin SQLite, sin dep nueva). `DashboardViewModel.LoadDashboardAsync` intenta backend → si OK guarda cache; si falla carga cache + activa `IsOfflineMode` + banner amarillo "📴 Modo offline · última actualización hace X min". Best-effort save (no aborta el flow). Solo Dashboard v1 — la lista paginada del ProviderReservationsPage no cachea (menos crítico para operator en la calle). (2) **MO-51**: `ProviderReservationsViewModel` rehecho — chips filtro por tour (auto-derivados de las reservas cargadas, sin llamada extra al backend), agrupación por día usando `CollectionView.IsGrouped` con nuevo `ReservationDayGroup : ObservableCollection<ClientReservation>`, ordenamiento ascendente por `ScheduleDate + SlotTimeStart`. Filtro y agrupación son puramente client-side sobre las páginas ya cargadas.
- **2026-07-15 — MO-43 (deep-linking WhatsApp)**: (1) botón 🔗 en `TourDetailPage` al lado del ❤️. Comando `ShareTourCommand` arma `"¡Mira este tour en Tourya!\n{tourName}\n{Constants.WebBaseUrl}/clients/tours-detail/{tourId}"` y llama `Share.Default.RequestAsync` → share sheet nativo (WhatsApp/SMS/email/etc.). (2) `MainActivity` con `[IntentFilter]` captura `https://dev.tourya.co/clients/tours-detail/*`. `HandleDeepLink()` (invocado desde `OnCreate` cold start + `OnNewIntent` app viva) parsea el URI, extrae `tourId` y navega a Shell `tour-detail?tourId=X`. Sin `autoVerify` — Android muestra chooser "abrir con Chrome o Tourya?" (deuda **MO-43b**: montar `.well-known/assetlinks.json` en `dev.tourya.co` para autoVerify). Nuevo `Constants.WebBaseUrl` + `Constants.WebHost` — single point of change para migrar a `tourya.co` en prod.
- **2026-07-15 — MO-42 + MO-41 (geo mobile)**: (1) `TourDetailPage` sección "📍 Punto de encuentro" con dirección + botón "Cómo llegar" que abre la app de mapas default del sistema via `Map.Default.OpenAsync` (respeta Google Maps, Waze, etc.). Sección se oculta si el tour no tiene coords. Sin embedded map (MO-42b futuro). (2) `ExplorePage` nuevo chip toggle "📍 Cerca de mí": pide permiso runtime `ACCESS_FINE_LOCATION` on-demand, obtiene ubicación con `Geolocation.Default.GetLocationAsync` (cache 5min), calcula distancias con Haversine client-side, ordena tours ascendente por distancia (los sin coords quedan al final), muestra "X km" en cada card. Nuevo `ILocationHelperService` (wrapper con manejo de permisos + timeouts) + `DistanceCalculator` (static). Sin backend nuevo — todas las coords ya venían en `TourAddress.latitude/longitude` que backend expone.
- **2026-07-15 — MO-21**: nueva `PayoutsPage` provider. Header con 4 cards de totales (`totalIncome`, `paid`, `pending`, `canceled`) + chips filtro status + lista de órdenes. `PayoutOrderDetailsPage` con reservations + attachments (comprobantes se abren en visor del sistema via `Launcher.Default.OpenAsync`). Backend intacto — 2 endpoints ya existían (`GET /provider/payout-orders` y `/{id}`). Nuevo tile "💰 Payouts" en `DashboardPage` arriba de "👥 Operarios" (posición prioritaria). 2 converters nuevos para chips de filtro (BoolToColorPrimaryOrTransparent / BoolToColorWhiteOrPrimary). Sin filtros de fecha en v1 — deuda MO-21b para cuando haya volumen.
- **2026-07-15 — MO-40 Fase C**: recepción y deep-links de push. Nuevo `IPushNotificationHandler` (singleton) que se suscribe a `CrossFirebaseCloudMessaging.Current.NotificationReceived` y `NotificationTapped` al arrancar la app. Comportamiento: foreground → alert modal con Title+Body + botones "Ver"/"Cerrar"; background+tap → deep-link directo. Convención del payload `data` que espera del backend (Fase D): `type` (reservation/tour/credit/review/generic) + `targetId` opcional → resuelve a ruta Shell (`reservation-detail?reservationId=X`, `tour-detail?tourId=X`, `credits`, etc.). Fallback a alert simple si el `type` no matchea. Handler se `Start()` en `MauiProgram` justo después de `builder.Build()`. Sin custom notification channel (usa el default del plugin — refinamiento futuro).
- **2026-07-15 — MO-40 Fase B**: registro de FCM token desde el mobile. Nuevo NuGet `Plugin.Firebase.CloudMessaging` 3.1.0 (Android only via `Condition`). `google-services.json` descargado de Firebase Console (app Android nueva `com.tourya.mobile` en proyecto `tourya-169d6`, app ID `1:318643880116:android:ac284c9d61d41ad13a17e1`) — colocado en `Platforms/Android/google-services.json` y marcado como `<GoogleServicesJson>` en el csproj. `AndroidManifest.xml` permiso `POST_NOTIFICATIONS` (Android 13+). `MainActivity.OnCreate` invoca `CrossFirebase.Initialize(this)`. Nuevo `IDeviceTokenSyncService` con `RegisterCurrentTokenAsync` (llama `CheckIfValidAsync` + `GetTokenAsync` + POST `/users/device-token`) y `UnregisterCurrentTokenAsync` (DELETE antes de limpiar storage). `SecureStorageService` extendido con `GetFcmTokenAsync`/`SetFcmTokenAsync`/`RemoveFcmTokenAsync`. `AuthService.LoginAsync`/`SocialAuthAsync` disparan register fire-and-forget post-persist; `LogoutAsync` invoca unregister ANTES de `ClearAllAsync`. Errores del sync NO bloquean auth — se loguean. Sin recepción de mensajes (eso es Fase C). Franklin necesita subir el service account JSON a Secret Manager para que el backend pueda mandar push a estos tokens.
- **2026-07-15 — MO-12**: UI dedicada de créditos del turista. Nuevo `CreditsPage` con saldo agregado en el header (suma de `amount - reservedAmount` de los CREATED) y 3 secciones: Activos (transferibles), Reservados en carrito (informativos), Historial (CANCELED/EXPIRED/DELETED). Nuevo `TransferCreditPage` con flujo lookup por documento (`GET /credits/tourist-lookup?documentNumber=X`) → confirm dialog → `POST /credits/{id}/transfer`. Ampliado `CreditService` con 2 métodos (lookup + transfer) y el modelo `ClientCredit` para incluir `transferredFromUserId`, `transferredAt` + helpers `AvailableAmount` y `WasTransferred`. Nuevo tile "💳 Mis Créditos" en el `ProfilePage`. Sin 6to tab por la misma razón que operarios. Transferencia validada por backend: solo 1 vez por crédito, no permite transferir reservados/vencidos — mensajes de error mapeados al 400. **Primera implementación de UI de créditos en cualquier cliente Tourya** (el frontend web nunca la tuvo, solo el service básico). El código sigue en local (MO-00 postponed).
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
| Reagendar reserva | `RescheduleReservationPage` + `RescheduleReservationViewModel` + `IReservationService.RescheduleReservationAsync` + botón "Reagendar" en `ReservationDetailPage` (visible sólo si `canReschedule==true`) | ✅ | **MO-53 CERRADO 2026-08-14 (Sprint 6b, commit `58d7a4f` + merge `1af997a`)**. Consume `PUT /reservations/{id}/reschedule` (backend ya existía por BE-20 / RN-033) — cero cambios backend. Dispatch client-side por `priceComparison` de la respuesta: **EQUAL/LOWER** → alert de confirmación + volver a Mis Viajes; **HIGHER** → alert explicando el flujo + `navigate //tourist/cart` porque el backend NO abre Wompi por el delta, sino que cancela la reserva anterior, abre crédito por lo pagado y agrega el nuevo tour al carrito (`status=CANCELLED_AND_ADDED_TO_CART`) — mismo patrón que web, intencional. 12 keys i18n `reservation.reschedule.*` + `common.ok` |
| Dejar reseña (con fotos) | `CreateReviewPage`, `CreateReviewViewModel` | ✅ | MO-10 (2026-07-14) + MO-31 (2026-07-15) confirmados: cámara + galería, 5 fotos máx, multipart, con compresión SkiaSharp cliente (1920px max + JPEG 80, ~85% ahorro) |
| Ver / gestionar créditos (usar, transferir) | `CreditsPage`, `TransferCreditPage`, `CreditsViewModel`, `TransferCreditViewModel` | ✅ | MO-12 cerrado 2026-07-15. Saldo agregado + 3 secciones (Activos / Reservados / Historial) + transferir con lookup por documento |
| Wishlist (lista de deseos) | `WishlistPage`, `WishlistViewModel`, `WishlistService` | ✅ | MO-11 cerrado 2026-07-15. Tab dedicado + toggle ❤️ desde `TourDetailPage` |
| Perfil turista (foto, documento, dirección) | `ProfilePage` | ✅ | |
| Notificaciones push | `IPushNotificationHandler` + FCM registro + hooks backend | ✅ | MO-40 completo (Fases A/B/C/D cerradas 2026-07-15). 5 flows: reserva confirmada, recordatorio 24h, crédito por expirar/expirado, respuesta review. Deep-links a Shell por `type + targetId` |
| Geolocalización ("cerca de mí") | `ExploreViewModel.ToggleNearbyCommand` + `LocationHelperService` + `DistanceCalculator` (Haversine) | ✅ | MO-41 cerrado 2026-07-15. Chip toggle en ExplorePage, permission on-demand, ordena por distancia client-side, muestra "X km" en cada card. Sin backend nuevo |
| Deep-linking | `MainActivity` IntentFilter con `AutoVerify=true` + `Constants.WebBaseUrl` + `TourDetailViewModel.ShareTourCommand` + `assetlinks.json` en `dev.tourya.co/.well-known/` | ✅ | MO-43 (2026-07-15) + Sprint 4a (2026-08-14). Compartir tour vía share sheet nativo + capture de link entrante que abre `TourDetailPage` directamente **sin chooser** — deuda MO-43b resuelta con `AutoVerify=true` en `MainActivity.cs` + assetlinks.json publicado en `tourya-front` PR #117 |

### Operador titular (PROVIDER)

| Funcionalidad (doc 14) | Estado mobile | Categoría | Notas |
|-------------------------|:-------------:|:---------:|-------|
| Dashboard (ingresos, tours, KPIs) | `DashboardPage`, `DashboardViewModel` | ✅ | |
| Crear tour (wizard) | `TourFormPage` (7 steps con Galería) + `TourDraftService` + `TourGalleryUploadService` + `AddressType` Picker + Meeting Point cascade | ✅ | Sprint 3 A + P6 + P7 (2026-08-13/14). Autosave en `AppDataDirectory/tour_draft.json`, upload background con `SemaphoreSlim(3)` + progress per-item + retry, addressType HOTEL_PICKUP soportado, Meeting Point con Country/State/City en cascada + geolocation. Deudas restantes: dictado de voz (P3) + `Syncfusion.Maui.Maps` embebido (P7b) + multi-location (P7c) |
| Editar tour | `TourFormPage` | ✅ | Idem — reusa el mismo wizard con autosave + upload background |
| Gestionar schedule (plantillas + slots + precios) | `ScheduleTemplateFormPage`, `ScheduleCalendarPage`, `TourSchedulesPage` + `SfCalendar` toggle Día/Semana/Mes + "💰 Copiar precios de otro tour" | ✅ | Sprint 3 B (2026-08-14). `ScheduleCalendarPage` con toggle Día/Semana/Mes vía `SfCalendar` + special dates predicate. Botón "💰 Copiar precios de otro tour" en `ScheduleTemplateFormPage` reusando endpoint TC-019 `GET /tour-schedules/templates?tourId={id}` + nuevo `ScheduleService.GetTemplatesForTourAsync` |
| Ver / gestionar reservas | `ProviderReservationsPage` | ✅ | |
| Confirmar reserva (QR scanner) | `QrScannerPage`, ZXing | ✅ 🎯 | Valor core del mobile |
| Marcar reserva manualmente | `QrScannerPage.xaml:29-34` botón "Ingresar manualmente" + `QrScannerViewModel.ProcessManualEntryAsync` (`:164-183`) | ✅ | Auditoría Sprint 5b (2026-08-14): vive en `QrScannerPage` (correcto UX — donde el operador está haciendo el scan), no en `ProviderReservationsPage`. Reusa el mismo pipeline QR con guard TC-007 |
| Ver reseñas | `ProviderReviewsPage` | ✅ | |
| **Responder reseñas** | `ProviderReviewsPage` + `ProviderReviewsViewModel` + `StringNotEmptyToBoolConverter` + 7 keys i18n | ✅ | Sprint 5a cerrado 2026-08-14. Flujo completo de responder reseñas reusando `ReviewService.ReplyToReviewAsync` (existía por MO-20). Cierra el gap del doc 14 "Responder reseñas provider" |
| Ver payouts + comprobantes | `PayoutOrdersPage`, `PayoutOrderDetailsPage`, `PayoutOrderService` | ✅ | MO-21 cerrado 2026-07-15. Lista con 4 totales agregados + filtro por status + detalle con reservas + descarga comprobante via `Launcher` del sistema. Sin filtros de fecha en v1 (deuda MO-21b) |
| **Crear operarios (`PROVIDER_OPERATOR`)** | `OperatorsPage`, `OperatorFormPage`, `ProviderOperatorService` | ✅ | MO-22 cerrado 2026-07-15. Share sheet nativo (MO-23) al finalizar create + reset |
| Editar / reasignar operarios | `OperatorFormPage` (mismo form) | ✅ | Multi-select tours + tour principal en el mismo form |
| Resetear password operarios | `ResetOperatorPasswordPage` | ✅ | MO-24 cerrado 2026-07-15. Share sheet al finalizar |
| Panel KYB / documentos | `KybStatusPage`, `KybRegistrationPage`, `KybDocumentsPage` | ✅ | Buena implementación |
| Notificaciones push | `IPushNotificationHandler` + FCM + hook "provider nueva reserva recibida" | ✅ | MO-40 completo (Fases A/B/C/D cerradas 2026-07-15). Nueva reserva post-pago dispara push al provider (dedup por proveedor) |
| Modo campo / offline | `IReservationCacheService` + fallback en Dashboard | ✅ | MO-50 cerrado 2026-07-17. JSON en `FileSystem.CacheDirectory`, save-on-success, fallback-on-error con banner "📴 Modo offline". Solo reservas del día en Dashboard v1 |

### Operario (`PROVIDER_OPERATOR`)

| Funcionalidad (doc 14) | Estado mobile | Categoría | Notas |
|-------------------------|:-------------:|:---------:|-------|
| Login con clave temporal + cambio | `AuthResponse.mustChangePassword` + guard en `LoginViewModel` + `ChangePasswordPage` + `ChangePasswordViewModel` + `AuthService.ChangeMyPasswordAsync` + `ApiService.PatchAsync<TRequest>` | ✅ | **MO-54 CERRADO 2026-08-14 (Sprint 6a, commit `8f50f1b` + merge `8693c97`)**. Consume `PATCH /users` (backend ya lo exponía en `UserController.java:30-38`) y `AuthenticationResponse.mustChangePassword` — cero cambios backend, primer consumo desde mobile. `LoginViewModel.LoginAsync` chequea el flag y navega a `//change-password` bloqueando el home; hasta cambiar la clave el operario no puede navegar. Ruta en `AppShell.xaml`, DI en `MauiProgram.cs`, 11 keys i18n `auth.changePassword.*` (es/en/pt). Cierra el security issue moderado del operario recién creado con clave temporal por PROVIDER (MO-24) |
| Ver reservas de tours asignados | `ProviderReservationsPage` (compartida con PROVIDER) | ✅ | Backend filtra por scope; verificar UX específica |
| Escanear QR | `QrScannerPage` | ✅ 🎯 | Core del rol |
| Confirmar reserva manualmente | `QrScannerPage.xaml:29-34` botón "Ingresar manualmente" + `QrScannerViewModel.ProcessManualEntryAsync` (`:164-183`) | ✅ | Auditoría Sprint 5b (2026-08-14): vive en `QrScannerPage` (donde el operador está haciendo el scan). Reusa el pipeline QR con guard TC-007 |
| Notificaciones de nueva reserva | `IPushNotificationHandler` + FCM + hook backend | ✅ | MO-40 completo (Fases A/B/C/D cerradas 2026-07-15). Nueva reserva post-pago dispara push al provider owner de la reserva |
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
| Notificaciones push (turista, operador, operario) | ✅ hecho (MO-40 A/B/C/D — 2026-07-15) | — |
| Cámara para reseñas | ✅ hecho (MO-10 + MO-31 — 2026-07-14/15) | — |
| Geolocalización ("cerca de mí", "cómo llegar") | ✅ hecho (MO-41 + MO-42 — 2026-07-15) | — |
| Deep-linking (compartir tours por WhatsApp) | ✅ hecho (MO-43 — 2026-07-15 + autoVerify Sprint 4a — 2026-08-14) | — |
| Wallet integration (Apple Pay / Google Pay vía Wompi) | ❌ | Media |
| Widget "próxima reserva" | ❌ | Baja |
| Modo offline (reservas del día para operario) | ✅ hecho (MO-50 — 2026-07-17) | — |
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

---

## Estado al 2026-08-13 (post ciclo QA agosto)

Auditoría técnica ejecutada tras el ciclo QA de agosto (TCs 007-021 mergeados a `develop` de `tourya-api` y `tourya-front` entre 24-jul y 13-ago) para dimensionar qué de esos cambios fue portado a mobile.

### Cambios ocurridos en `tourya-mobile` desde 17-jul

**Ninguno versionado.** El último commit sigue siendo `0bc518f` del 2026-04-10. Todo el trabajo posterior (MO-01 URL, MO-10 review UI, MO-11 wishlist, MO-12 credits, MO-20 responder reseñas, MO-21 payouts, MO-22/23/24 operarios, MO-31 compresión imágenes, MO-34 copiar plantilla, MO-40 push, MO-41/42/43 geo+deep-linking, MO-50/51 offline+filtros provider reservations) sigue vivo únicamente en el working tree local.

`git status` al 2026-08-13:
- **32 archivos modificados** desde `0bc518f` (incluye `Constants.cs`, `AppShell`, `MauiProgram`, 4 servicios base, 8 ViewModels, 8 Views).
- **40 archivos nuevos** untracked (Models/Operator/, Models/Payout/, Models/Push/, Models/Wishlist/, 9 services nuevos, 5 ViewModels tourist, 5 ViewModels provider, 9 Views nuevas, `google-services.json`).
- Total: **72 archivos con cambios sin commit ni push (MO-00 sigue abierto)**.

### Estado de compilación 2026-08-13

```
dotnet build TouryaMobile/TouryaMobile.csproj -c Debug -f net10.0-android
Compilación correcta. 0 Advertencia(s). 0 Errores. Tiempo 00:00:26
```

SDK usado: `.NET 10.0.300`. La app compila verde en el estado no-versionado actual.

### URL backend actual

`TouryaMobile/Constants.cs:9`:
```csharp
public const string ApiBaseUrl = "http://34.160.22.16/api/v1/";
```

Sigue siendo **HTTP** al LB GCP dev (MO-01 del 2026-07-13). El backend dev ya tiene HTTPS provisionado (`https://dev.tourya.co`), entonces esta constante **debería migrarse a `https://dev.tourya.co/api/v1/`** en el próximo ciclo mobile. `Constants.WebBaseUrl` (`https://dev.tourya.co`) y `Constants.WebHost` (`dev.tourya.co`) ya están en HTTPS desde MO-43.

### Inventario ampliado

Cambios vs inventario del snapshot 17-jul del cuerpo principal:

- **Vistas**: 33 páginas .xaml (25 + 9 nuevas — se listan explícitamente en la sección "Cambios post-último-commit").
- **Servicios**: 25 (14 + 11 nuevos: DeviceTokenSync, DistanceCalculator, ImageCompression, LocationHelper, PayoutOrder, ProviderOperator, PushNotificationHandler, ReservationCache, Wishlist + refactor de AuthHandler/ApiService/SecureStorage).
- **ViewModels**: 35 (26 + 9 nuevos: CreateReview, Credits, TransferCredit, Wishlist, Operators, OperatorForm, ResetOperatorPassword, PayoutOrders, PayoutOrderDetails).

### Deudas técnicas críticas abiertas al 2026-08-13 (actualizado post cierre Sprint 2 mobile)

De la tabla "Tech debt actual" del cuerpo principal (8 items), estado tras el cierre del Sprint 2 mobile:

| # | Item | Estado 2026-08-13 (post Sprint 2) |
|---|------|-----------------------------------|
| 1 | Sin remoto Git | 🔴 **Abierto — diferido por decisión operativa Franklin (P3)**. MO-00 pospuesto desde 2026-07-13; se retomará más adelante. Riesgo #1 persiste. |
| 2 | URL API hardcoded a IP legacy | 🟢 **CERRADA** — MO-01b hecho: `Constants.cs:9` migrado a `https://dev.tourya.co/api/v1/` (HTTPS). Coherente con `Constants.WebBaseUrl`/`WebHost` ya en HTTPS desde MO-43. |
| 3 | Syncfusion License sin registrar | 🟢 **CERRADA** — MO-02 hecho: license de **Touya Marketplace S.A.S.** registrada en `MauiProgram.cs` vía `SyncfusionLicenseProvider.RegisterLicense`. Fin del banner de trial. |
| 4 | `ForgotPasswordPage` sin endpoint backend | 🔴 Abierto — sigue como feature muerta (no se abordó en Sprint 2). |
| 5 | iOS no soportado | 🟢 **CERRADA por decisión (P5)** — MVP Android only confirmado. Se replanteará solo si el mercado empuja (~35% iOS en tourista colombiano) — no antes de ganar tracción en Android. |
| 6 | Sin CI/CD para builds firmados | 🔴 Abierto — bloqueante para Play Store. Depende de MO-00 (sin repo no hay CI). |
| 7 | Sin analytics / crash reporting | 🔴 Abierto. |
| 8 | Sin política de privacidad / términos | 🔴 Abierto — bloqueante Play Store. |

**Nuevas deudas post ciclo QA** (detalle en próxima sección):

- **MO-DT9** — `document_type` del turista no editable en Profile y no seleccionable en Checkout (hardcoded "CC" en `CheckoutViewModel:36`).
- **MO-DT17** — Hotel Pickup del tour no soportado en mobile (ni display en `TourDetailPage`, ni en `TourFormPage`).
- **MO-DT18** — DIMAR RED / `blockedByMaritimeReport` no respetado por `TourDetailPage` add-to-cart.
- **MO-DT14** — Cart mobile no detecta ni comunica items purgados automáticamente por el backend.
- **MO-DT16** — `travelerBreakdown` por ageType no se renderiza en pantallas provider.
- **MO-DT21** — Provider views no manejan explícitamente el scrub de datos cliente (`ClientReservation.PayerName?` ya es nullable pero XAML no muestra placeholder "🔒 Datos disponibles el día del tour").
- **MO-DT19** — `ScheduleTemplateFormViewModel` hardcodea `AgeType = "ADULT"` en línea 233 (un solo precio por slot); backend soporta multi-ageType con `providerPrice` margen.

**Deudas nuevas registradas por auditoría Sprint 5b (2026-08-14)**: — **ninguna abierta**. Ambas cerradas por Sprint 6 (2026-08-14b):

- ~~**MO-53** — Reagendar reserva turista con 3 casuísticas~~ → **CERRADA Sprint 6b** (commit `58d7a4f` + merge `1af997a`). Ver sección "Post-Sprint 6 — cierre 2026-08-14b" abajo.
- ~~**MO-54** — Forzar cambio password primer login operario~~ → **CERRADA Sprint 6a** (commit `8f50f1b` + merge `8693c97`). Ver sección "Post-Sprint 6 — cierre 2026-08-14b" abajo.

---

## Gap con ciclo QA agosto (TCs 007-021)

Matriz de cross-reference: para cada TC mergeado en `develop` durante el ciclo QA de agosto, evaluación de si el cambio fue portado al mobile local y qué archivos habría que tocar.

Convenciones: ✅ Portado · ⚠️ Parcial · ❌ No portado · 🟢 N/A (feature web-only o backend-only).

| TC | Cambio backend/web | Estado mobile | Evidencia | Archivo(s) mobile a tocar |
|----|---------------------|:-------------:|-----------|---------------------------|
| **TC-007** | Guard temporal confirmar reserva + timezone Bogota en scheduleDate | ⚠️ | Sin guard visual client-side. `QrScannerViewModel` procesa el response del backend sin mensaje user-friendly si el backend rechaza por "muy temprano". Timezone lo maneja el backend (mobile solo consume string ISO). | `ViewModels/Provider/QrScannerViewModel.cs` (mapear código de error "temprano" a mensaje amigable). |
| **TC-008** | Rol BACKOFFICE_OPERATION + sidebar admin | 🟢 | Feature web/admin-only. | — |
| **TC-009** | `document_type` en tourist_profile + migración 083 + POST/GET endpoints | ❌ | `ProfilePage.xaml` no permite editar documento (solo `Initials`/`FullName`/`Email`). `CheckoutViewModel.cs:36` hardcodea `PayerDocumentType = "CC"`; XAML usa `Entry` libre (no `Picker`). `AuthResponse` no tiene `documentType`. | `Views/Tourist/ProfilePage.xaml` + `ProfileViewModel.cs` (nueva sección "Editar datos personales" con `Picker` documentType CC/CE/PP/TI); `Views/Tourist/CheckoutPage.xaml:49` (Entry → Picker prellenado con el perfil); `Models/Auth/AuthResponse.cs` (agregar `documentType`); nuevo endpoint client `GET /users/me/profile` y `PUT` en `AuthService.cs`. |
| **TC-011** | Job NO_SHOW procesa RESCHEDULED | 🟢 | Backend-only. Mobile consume estados finales. | — |
| **TC-012/013** | Guest-info-modal en checkout | ❌ | Grep `guestInfo` vacío. La app requiere login para todo el flujo (add-to-cart/checkout ya lo asumen autenticado). Definir si mobile abre flujo guest. | (Ver pregunta abierta P1) — potencial `Views/Tourist/GuestInfoModal.xaml` + refactor `CheckoutViewModel` si se aprueba. |
| **TC-014** | Auto-purga cart items con scheduleDate vencido | ❌ | `CartViewModel` no compara cart pre/post-load ni muestra alerta. `ShoppingCartResponse` no expone flag `purgedItems`. El usuario ve items desaparecer sin explicación. | `ViewModels/Tourist/CartViewModel.cs` (diff pre/post-refresh, contar removidos); `Views/Tourist/CartPage.xaml` (banner "⚠️ Se eliminaron X reservas caducadas"). |
| **TC-015** | ADMIN edita porcentajeTourya | 🟢 | Feature admin-only. | — |
| **TC-016** | `travelerBreakdown` por ageType en reservas | ❌ | `ClientReservation` (`ReservationDtos.cs:8-81`) solo tiene `TotalTourists`; no hay `travelerBreakdown`. Idem `ReservationDetails`. Provider ve "3 turistas" sin saber composición. | `Models/Reservation/ReservationDtos.cs` (nuevo `TravelerBreakdownDto { AgeType, Quantity }` + campo en Client/Details); `Views/Provider/ProviderReservationsPage.xaml` (chip "2 Adultos · 1 Niño"); `Views/Tourist/ReservationDetailPage.xaml` y `Views/Provider/...ReservationDetailPage.xaml` (desglose completo). |
| **TC-017** | Flag HOTEL_PICKUP + guardar en español + mensaje UI + i18n priceType + selector orden | ❌ (parcial dependencia con TC-019) | Grep `HOTEL_PICKUP`/`hotelPickup`/`AddressType` vacío en todo el repo mobile. `TourLocationDto` (`TourDetailResponse.cs:69-85`) no tiene `addressType`. `TourFormViewModel.cs` no permite elegir addressType. `TourDetailPage.xaml:120` renderiza siempre el punto de encuentro con coords. | (a) `Models/Tour/TourDetailResponse.cs` (agregar `AddressType` a `TourLocationDto`); (b) `Views/Tourist/TourDetailPage.xaml:120` ocultar cuando `AddressType == HOTEL_PICKUP` y mostrar mensaje traducido i18n (3 idiomas); (c) `Views/Provider/TourFormPage.xaml` + `TourFormViewModel.cs` + `Models/Provider/TourFormDtos.cs` (Picker addressType HOTEL_PICKUP / MEETING_POINT); (d) `Views/Tourist/CartPage.xaml` (chip "🏨 Pickup en hotel" en el item). |
| **TC-018** | Reporte DIMAR RED bloquea carrito + cancela reservas retroactivamente | ❌ | Grep `dimar`/`maritime`/`blockedBy` vacío. `TourDetailViewModel.CanAddToCart` no chequea flag. `ProviderReservationsPage` no muestra badge de "Cancelada por DIMAR". | `Models/Tour/TourDetailResponse.cs` (agregar `blockedByMaritimeReport: bool`, `maritimeAlertMessage: TranslatedField?`); `ViewModels/Tourist/TourDetailViewModel.cs` (bloquear `CanAddToCart` + banner rojo); `Models/Reservation/ReservationDtos.cs` (agregar `cancellationReason` para distinguir DIMAR); `Views/Provider/ProviderReservationsPage.xaml` (badge "🌊 Cancelada por DIMAR"). |
| **TC-019** | sub_category + batch schedule + precios con margen + backfill drift + reschedule booking count + slot pct recalc | ⚠️ | ✅ `TourFormViewModel.cs:60,319` + `TourFormPage.xaml:66-67` YA tienen SubCategory Picker (portado). ✅ `SlotPriceRequest.cs:52-54` YA tiene `providerPrice` para el margen (portado modelo). ❌ `ScheduleTemplateFormViewModel.cs:233` hardcodea `[new SlotPriceRequest { AgeType = "ADULT", Price = s.Price }]` — no soporta multi-ageType ni edición del margen. ❌ Batch schedule create no expuesto en UI. ❌ ScheduleCalendarPage no muestra desglose de precios con margen. | `ViewModels/Provider/ScheduleTemplateFormViewModel.cs` (soporte multi-precio por ageType + edición providerPrice); `Views/Provider/ScheduleTemplateFormPage.xaml` (repeater de precios); opcional `Views/Provider/ScheduleBatchCreatePage.xaml` nuevo. |
| **TC-020** | docType real a Wompi + timezone JVM Bogota + botón Ver Reservas + refresh perfil | ⚠️ | `CheckoutViewModel:36` docType default "CC" con `Entry` libre — el user PUEDE escribir el correcto pero por defecto va "CC" (mismo bug que Wompi rechazaba). Timezone lo maneja backend. `PaymentConfirmationPage` a validar si tiene botón "Ver Reservas". Refresh perfil post-tx no se hace. | `Views/Tourist/CheckoutPage.xaml:49` (Entry → Picker con opciones CC/CE/PP/TI/NIT); `Views/Tourist/PaymentConfirmationPage.xaml` (botón "Ver mis reservas" navega a `//tourist/my-trips`); `ViewModels/Tourist/ProfileViewModel.cs` (refresh en `OnAppearing` post-tx). |
| **TC-021** | Backend scrub datos cliente al PROVIDER cuando falta >1 día al tour | ⚠️ | `ClientReservation.PayerName?` ya es `string?` (nullable safe). Falta comportamiento explícito de UI: no romper cuando null y mostrar mensaje. `ProviderReservationsPage.xaml` bindings a validar. | `Views/Provider/ProviderReservationsPage.xaml` (fallback text "🔒 Datos disponibles el día del tour" cuando `PayerName == null`); `Views/Provider/...ReservationDetailPage.xaml` (idem); potencialmente flag `dataScrubbed` en `ClientReservation` para diferenciar "aún no visible" vs "nunca hubo dato". |

**Resumen numérico:**

- ✅ **0 TCs portados completos** (TC-019 sub_category está portado pero es 1 de 6 subitems).
- ⚠️ **4 TCs parciales** (TC-007, TC-019, TC-020, TC-021).
- ❌ **5 TCs no portados** (TC-009, TC-012/013, TC-014, TC-016, TC-017, TC-018).
- 🟢 **3 TCs N/A** (TC-008, TC-011, TC-015).

Los ❌ pesan más en la UX del turista (TC-009, TC-014, TC-017, TC-018) que en la del provider (TC-016 + refuerzo TC-021).

---

## Roadmap actualizado 2026-08-13 (post ciclo QA)

Reemplaza los "Ciclos 0-6" del cuerpo principal para priorizar el gap del ciclo QA antes de features nuevas.

### Ciclo 0 — Higiene (bloqueante, no negociable)

1. **MO-00 — Crear repo `Touryapp/tourya-mobile` en GitHub** y `git push -u origin master`. Sin esto la próxima falla de disco borra 4 meses de trabajo (72 archivos untracked hoy).
2. **MO-01b — Migrar URL API a HTTPS** `https://dev.tourya.co/api/v1/` en `Constants.cs:9`.
3. **MO-02 — Registrar Syncfusion License** (env var + `SyncfusionLicenseProvider.RegisterLicense` en `MauiProgram.cs`).
4. **MO-03 — CI/CD básico**: GitHub Actions build + APK firmado (Keystore en Secret Manager) + subida a Play Store internal track.
5. **MO-04 — Habilitar Firebase Crashlytics** (`Plugin.Firebase.Crashlytics`) — sin visibilidad no hay operación seria.

### Ciclo 1 — Portar QA agosto que impacta UX del turista (alta prioridad)

1. **MO-DT9 (TC-009)** — Editar `document_type` en Profile + Picker en Checkout + prellenar del perfil.
2. **MO-DT20 (TC-020)** — Reforzar checkout con Picker docType real + botón "Ver mis reservas" en confirmación + refresh perfil.
3. **MO-DT17 (TC-017)** — Hotel Pickup end-to-end: mostrar mensaje traducido en `TourDetailPage`, ocultar mapa, chip en cart, addressType en TourFormPage.
4. **MO-DT18 (TC-018)** — DIMAR RED: bloquear add-to-cart si `blockedByMaritimeReport = true` + banner rojo + notificar reservas canceladas al provider.
5. **MO-DT14 (TC-014)** — Cart: detectar items purgados post-refresh + banner amarillo "Se eliminaron X reservas caducadas".

### Ciclo 2 — Portar QA agosto que impacta UX del provider

1. **MO-DT16 (TC-016)** — `travelerBreakdown` en `ProviderReservationsPage` y ambos `ReservationDetailPage`.
2. **MO-DT21 (TC-021)** — Placeholder "🔒 Datos disponibles el día del tour" cuando el scrub deja nulls.
3. **MO-DT7 (TC-007)** — Mapear código de error "guard temporal" del backend a mensaje user-friendly en `QrScannerViewModel`.
4. **MO-DT19b (TC-019)** — Multi-ageType + `providerPrice` (margen) en `ScheduleTemplateFormViewModel` y su XAML.
5. **MO-DT19c (TC-019)** — Batch schedule create UI si se prioriza (ver pregunta abierta P2).

### Ciclo 3 — Refinar UX de creación/config del proveedor (arrastra del roadmap original)

1. Autosave / borradores en el wizard de tour.
2. Vista de calendario mejorada en schedule (día/semana/mes).
3. Compresión de imágenes en wizard de tour (SkiaSharp ya está en el csproj vía MO-31).
4. Upload en background.

### Ciclo 4 — Features "solo mobile" pendientes

De los items del doc 14 no portados por el ciclo QA:

1. **Push Fase D**: definir con backend los payloads de notificación por evento (nueva reserva → provider, cancelación DIMAR → turista, etc.). Registro FCM (MO-40 Fase B) y handler (Fase C) ya están.
2. **MO-43b — autoVerify de deep-links**: montar `.well-known/assetlinks.json` en `dev.tourya.co` para que el chooser desaparezca.
3. **Widget "próxima reserva"** (turista + operario).
4. **Wallet integration** (Apple Pay / Google Pay vía Wompi) — media prioridad, deprioritzar hasta salir de Android-only.

### Ciclo 5 — Robustecer offline y rol operario

1. Extender `IReservationCacheService` a la lista paginada del `ProviderReservationsPage` (hoy solo Dashboard).
2. Reforzar vista de reservas del operario (agrupación por hora en el día).
3. Fallback manual del QR — verificar que exista (deuda pendiente del doc 15 cuerpo).

### Ciclo 6 — Integración con agentes IA (Fase 2)

1. Travel Concierge en `ExplorePage` (según [16](16-agentes-ia.md)).
2. Otros agentes según alcance de Luis.

### Ciclo 7 — Guest checkout (si aplica) — 🚫 **CANCELADO 2026-08-13 (Luis)**

1. ~~**MO-DT12/13 (TC-012/013)** — Guest info modal en checkout si se decide portar (ver pregunta abierta P1).~~ **Ciclo cancelado**: Luis confirmó 2026-08-13 que el mobile obliga a crear cuenta. No hay flujo guest en mobile.

---

## Preguntas abiertas para Luis / Franklin

- **P1 (TC-012/013 guest checkout)**: ¿el mobile debe permitir compra sin login (con guest-info-modal como el web) o queda auth-required? El web lo soporta post-TC-013. Impacto: si sí, es Ciclo 7 con ~1 semana de trabajo (nuevo modal + refactor `CheckoutViewModel` + endpoint client `/public/guest-checkout` a verificar en backend).
- **P2 (TC-019 batch schedule mobile)**: ¿el provider mobile va a crear schedules en lote (batch) desde el móvil o eso se queda como flujo web? El doc 14 pone provider config como "solo cuando está en la oficina", entonces batch podría quedar solo-web.
- **P3 (MO-00 repo Git)**: ¿seguimos posponiendo o ya lo creamos ahora que estamos por retomar el ciclo? Riesgo: 72 archivos untracked hoy, sin backup remoto.
- **P4 (Syncfusion license)**: ¿se compra la license (~USD 995/dev/año Essential Studio) o migramos los controles Syncfusion (Maps, Charts) a alternativas free/OSS antes del release público? Bloqueante para Play Store por el banner de trial.
- **P5 (iOS)**: ¿confirmamos que iOS queda fuera de scope para el MVP o se replantea? Ya se decidió Android-only pero el mercado turista colombiano tiene ~35% iOS.
- **P6 (TC-017 addressType HOTEL_PICKUP en `TourFormPage`)**: ¿el provider mobile debe permitir crear tours con addressType HOTEL_PICKUP desde el móvil o eso queda solo-web?

---

## Cierre Sprint 2 mobile 2026-08-13

Cierre del ciclo mobile que porta al MAUI Android los 9 TCs del ciclo QA agosto que impactaban al mobile (los 3 N/A `TC-008/011/015` quedan como web-only por diseño; guest checkout `TC-012/013` queda cancelado por decisión de Luis) **y** el nuevo feature TC-022 (devolución de créditos, issue #253) que el mismo día se agregó al roadmap y se cerró en los 3 frentes en un solo día.

### Estado del código MAUI al 2026-08-13 (post Sprint 2)

- **Build**: verde en `dotnet build TouryaMobile/TouryaMobile.csproj -c Debug -f net10.0-android` con `.NET 10.0.300` — 0 errores, 0 advertencias.
- **URL API**: migrada a HTTPS `https://dev.tourya.co/api/v1/` (MO-01b cerrado).
- **Syncfusion License**: registrada a nombre de Touya Marketplace S.A.S. (MO-02 cerrado).
- **Sin repo Git remoto** (MO-00): sigue diferido por decisión Franklin (P3 aún no).
- **Sin CI/CD** (MO-03): bloqueado por MO-00.

### TCs portados en el Sprint 2

Los 9 TCs del ciclo QA agosto que aplicaban al mobile pasaron todos a **✅** en la matriz "Gap con ciclo QA agosto":

| TC | Cambio portado a mobile | Piezas nuevas |
|----|-------------------------|---------------|
| **TC-009** | `documentType` del turista editable en `ProfilePage` (Picker CC/CE/PA/NIT) + `CheckoutViewModel` pre-fill desde profile. | Refactor de `AuthResponse` con `documentType`, endpoint client `GET /users/me/profile` + `PUT` en `AuthService`, sección "Editar datos personales" en `ProfilePage.xaml`, `Picker` en `CheckoutPage.xaml`. |
| **TC-014** | Auto-purga cart items con `scheduleDate` vencido — detección client-side + alert al turista. | `CartViewModel` diff pre/post-refresh + banner en `CartPage.xaml`. |
| **TC-017** | Display Hotel Pickup en 4 vistas mobile (TourDetail, Explore, Cart, ReservationDetail) + i18n `priceType` + nueva API `I18nService.T(key)` con tablas es/en/pt. | `AddressType` en `TourLocationDto`, getter `IsHotelPickup`, chip "🏨 Pickup en hotel", `I18nService` extendido con `T()` + diccionarios. Creación con `addressType=HOTEL_PICKUP` en `TourFormPage` queda como **deuda pendiente** (P6 aprobada Luis 2026-08-13 — para futuro ciclo). |
| **TC-018** | DIMAR RED bloquea add-to-cart mobile + guard 400 backend + banner rojo. | `blockedByMaritimeReport` en `TourDetailResponse`, `TourDetailViewModel.CanAddToCart` chequea flag, banner + mensaje traducido. |
| **TC-020** | `docType` real a Wompi mobile (no más hardcode `"CC"`) + botón "Ver mis reservas" en confirmación + refresh profile post-tx. | Picker en `CheckoutPage.xaml`, botón en `PaymentConfirmationPage`, refresh en `ProfileViewModel.OnAppearing`. |
| **TC-016** | `travelerBreakdown` por `ageType` en reservas mobile — desglose "2 Adultos · 1 Niño" con nuevo `TravelerBreakdownConverter` + pluralización via `I18nService`. | Nuevo DTO `TravelerBreakdownDto`, chip en `ProviderReservationsPage`, desglose en ambos `ReservationDetailPage`. |
| **TC-021** | Placeholder "🔒 Datos disponibles el día del tour" cuando el backend scrubbea datos cliente (PROVIDER puro, >1 día al tour). | Fallback text en `ProviderReservationsPage.xaml` + `ReservationDetailPage`. |
| **TC-007** | Guard temporal QR scanner (solo día del tour Bogota) — mapeo de código de error backend a mensaje user-friendly en `QrScannerViewModel`. | Actualización de `QrScannerViewModel` para interpretar la excepción de guard temporal. |
| **TC-019** | Multi-ageType en `ScheduleTemplateFormViewModel` mobile (antes hardcoded `AgeType = "ADULT"` en línea 233). | Repeater de precios en `ScheduleTemplateFormPage.xaml` + soporte multi-precio con `providerPrice` en el VM. Batch schedule create UI mobile (MO-DT19c) sigue **pendiente respuesta Luis** (P2). |

Resultado: **mobile alineado 1:1 con web al 13-ago** para todos los TCs del ciclo QA agosto que aplicaban.

### TC-022 (devolución de créditos, issue #253) — mobile cerrado el mismo día

Feature nuevo que apareció el 13-ago y se cerró en los 3 frentes en un día:

- **Backend** (PR api #254 + migración 091): 2 nuevos estados `REFUND_REQUESTED`/`REFUNDED` en `CreditStatusEnum` + 3 columnas nuevas en `credit` + 3 endpoints (`POST /credits/{id}/request-refund` turista, `POST /admin/credits/{id}/upload-refund-proof` ADMIN/BACKOFFICE multipart, `GET /admin/credits` paginado global). Ver [doc 09 §Credits](09-api-design.md) y [RN-062](05-reglas-de-negocio.md#rn-062).
- **Frontend web** (PR front #115): turista con botón "Solicitar devolución" + SweetAlert2 confirm en `/clients/my-profile?section=credits`. Nueva ruta `/admin/credits` con `AdminCreditsComponent` (tabla paginada + modal upload). Item "Créditos" agregado al sidebar admin. i18n ES/EN/PT completo.
- **Mobile MAUI** (commit local `2d770f8` mergeado a develop): turista con botón "Solicitar devolución" + confirm nativo en `CreditsPage`. Chip visual REFUND_REQUESTED (amarillo) y REFUNDED (azul) + link "Ver comprobante" cuando aplica. **Flujo admin queda web-only** (mobile no expone el upload — coherente con doc 14: backoffice es web-only por diseño).

### Deudas y decisiones de scope registradas

- **P6 TC-017 addressType HOTEL_PICKUP en `TourFormPage` mobile**: **SÍ** (Luis 2026-08-13). Provider mobile debe crear tours con `addressType=HOTEL_PICKUP`. Deuda registrada para futuro ciclo (Picker addressType en `TourFormPage.xaml` + `TourFormViewModel` + `TourFormDtos`).
- **P1 TC-012/013 guest checkout mobile**: **NO** (Luis 2026-08-13). Mobile obliga a crear cuenta. **Ciclo 7 del roadmap cancelado**.
- **P2 TC-019 batch schedule provider mobile**: sigue pendiente respuesta de Luis. Deuda MO-DT19c abierta.
- **P3 (repo Git), P4 (Syncfusion license), P5 (iOS)**: respondidas por Franklin — decisiones grabadas en memoria del proyecto (`mobile_retomar_decisiones_2026-08-13.md`). Ver tabla de tech debt arriba.

---

## Post-Sprint 2 — cierre P6 + P7 mobile (2026-08-13)

Cierre del arrastre inmediato del Sprint 2 mobile: se implementa la decisión P6 aprobada por Luis y se agrega P7 (Meeting Point cascade + geolocation en `TourFormPage`) — que estaba latente como bug del CHECK backend `tour_address_geo_required_unless_hotel_pickup` (migración 087). Ambos cerrados en `tourya-mobile` local con merge a `develop`.

### P6 — TourFormPage crea tours HOTEL_PICKUP (commit `35c205c` + merge `33dc18a`)

Cumple la decisión Luis P6 = SÍ (2026-08-13). El provider mobile ahora puede crear tours con `addressType = HOTEL_PICKUP` desde el móvil, cerrando la asimetría con web.

- **`TourFormPage.xaml` + `TourFormViewModel`**: nuevo `Picker` para `addressType` con opciones `MEETING_POINT` (default) y `HOTEL_PICKUP`.
- **Comportamiento del Picker**: cuando el provider selecciona `HOTEL_PICKUP`, los campos geo (Country/State/City + latitude/longitude + address) quedan ocultos — coherente con el CHECK backend `tour_address_geo_required_unless_hotel_pickup` (migración 087) que sólo admite geo NULL cuando `address_type = 'Hotel Pickup'`.
- **`TourFormDtos.cs`**: agregado `AddressType` al request/response del form.
- **Sin cambios backend** — el enum + CHECK ya existen desde el ciclo TC-017.

### P7 — TourFormPage Meeting Point con Country/State/City cascade + geolocation (commit `0a10ef7` + merge)

Cierra un **bug latente** del CHECK backend: antes de este cierre, el mobile no capturaba geo cuando el provider elegía `MEETING_POINT` — los INSERT posteriores fallaban con `null value violates check constraint tour_address_geo_required_unless_hotel_pickup` porque `country_id/state_id/city_id/lat/lng` iban vacíos. P7 lo resuelve capturando geo real desde el mobile.

- **`TourFormPage.xaml`**: sección Meeting Point con **3 `Picker` en cascada** — Country → State (filtrado por country) → City (filtrado por state).
- **`LocationCatalogService` (nuevo)**: consume los 3 endpoints públicos existentes del backend:
  - `GET /public/country/getAllCountryList`
  - `GET /public/state/getAllStateByCountryIdList/{countryId}`
  - `GET /public/city/getAllCityByStateIdList/{stateId}`
  - Sin nuevos endpoints — reusa los que ya alimentan el frontend web.
- **`ILocationHelperService` reutilizado (de MO-41)**: nuevo botón "Usar mi ubicación actual" en la sección Meeting Point → `Geolocation.Default.GetLocationAsync` (con permission on-demand) → pre-llena `latitude` y `longitude`.
- **`TourFormViewModel`**: comandos `LoadCountriesCommand`, `OnCountryChangedCommand` (limpia state+city y carga states), `OnStateChangedCommand` (limpia city y carga cities), `UseCurrentLocationCommand`.
- **Sin cambios backend** — todos los endpoints y validaciones ya existían.

### Deudas nuevas registradas en el backlog

Ambos merges dejaron deuda técnica identificada; se traza para futuros ciclos:

- **P7b — `Syncfusion.Maui.Maps` embebido en TourForm**: hoy el provider ve lat/lng como números (o "usar mi ubicación" resuelve una vez). Falta un mapa embebido para arrastrar el pin y ajustar el punto exacto. Deuda de UX; depende de Syncfusion License (MO-02 cerrada) pero es refinamiento no bloqueante.
- **P7c — Multi-location por tour en mobile**: el backend y el web soportan varias `TourLocation` por tour (array). El mobile actualmente sólo captura **una** location. Deuda funcional — hay tours reales con múltiples meeting points (San Andrés + tour multi-punto) que el provider no puede crear desde mobile.
- **P7d — Cleanup de `AddLocation` / `RemoveLocation` no-op en `TourFormViewModel`**: hay comandos `AddLocationCommand` / `RemoveLocationCommand` heredados de una iteración previa que no hacen nada útil ahora que la UI es single-location. Cleanup pendiente; sacar el código muerto para no confundir en próximas iteraciones. Se elimina cuando se aborde P7c o antes si molesta.
- **TC-022b — Agregar columna `reason` a `Credit` entity para que los emails de refund mencionen motivo original**: los templates `credit_refund_requested.html` y `credit_refund_completed.html` de PR #257 no pueden mostrar el motivo original del crédito porque la entidad `Credit` sólo tiene `type` (CANCELATION/RESCHEDULE/BONUS) y no un free-text con el detalle. Un turista que reciba el email no ve "tu tour del 15 de julio a Islas del Rosario fue cancelado por lluvia" — solo "devolución de tu crédito". Deuda de calidad de comunicación; requiere migración BD + backfill NULL + actualización de los publishers para pasar el `reason` al event snapshot.
- **TC-022c — Multi-idioma en emails de refund (hoy solo ES)**: los 2 templates de PR #257 sólo tienen versión ES, coherente con los templates BE-18/19 de expiración de créditos que también son solo español. Migrar a ES/EN/PT es ítem aparte — impacta también BE-18/19 y otros correos transaccionales del backend. Fuera de scope de TC-022; se traza para cuando se decida i18n de comunicaciones del backend.

---

## Post-Sprint 3/4a/5a — cierre 2026-08-14

Cierre de 3 sprints mobile sucesivos + auditoría 5b. Todos los cambios viven en `tourya-mobile` local (MO-00 sigue abierto: `develop` local mergea las ramas pero no hay remoto Git).

### Sprint 3 A — Autosave + upload background en `TourFormPage` (cierra MO-30 + MO-32)

- **`TourDraftService`**: nuevo servicio que persiste el estado del wizard de creación de tour en `AppDataDirectory/tour_draft.json`. Guarda cada cambio del form como snapshot JSON, recupera al reabrir la app si hubo crash o cierre inesperado. Cierra deuda MO-30 (autosave/borradores).
- **`TourGalleryUploadService`**: upload de imágenes en background con `SemaphoreSlim(3)` (3 uploads paralelos máximo), progress per-item (0-100%) y retry en fallo transitorio. El wizard ahora no bloquea al provider mientras suben las imágenes — puede seguir configurando otros steps. Cierra deuda MO-32 (upload background).
- **`TourFormPage` Step 7 Galería**: nueva sección UI dentro del wizard que permite adjuntar imágenes de la galería del tour con progress bar por imagen y estado "subiendo / subida / falló → retry".

### Sprint 3 B — Vista calendario + copiar precios (cierra MO-33 + reabre alcance MO-34)

- **`ScheduleCalendarPage`** con `SfCalendar` (Syncfusion) + toggle Día/Semana/Mes vía botones + special dates predicate (marca los días con slots configurados). Cierra deuda MO-33 (vista calendario mejorada).
- **Botón "💰 Copiar precios de otro tour"** en `ScheduleTemplateFormPage`. Ahora reusa el endpoint TC-019 `GET /tour-schedules/templates?tourId={id}` con nuevo método `ScheduleService.GetTemplatesForTourAsync` — el provider puede filtrar plantillas **por el tour destino** (antes MO-34 quedó en "Copiar de otra plantilla" mostrando TODAS las del provider porque el response no incluía `tourId`; TC-019 lo agregó). Cierra el gap del alcance original del backlog "Copiar precios de otro tour".

### Sprint 4a — autoVerify + assetlinks (cierra MO-43b)

- **`MainActivity.cs`** con `AutoVerify=true` en el `IntentFilter` del deep-link para `https://dev.tourya.co/clients/tours-detail/*`. Android ahora abre la app **sin chooser** cuando el usuario tapea un link (antes MO-43 mostraba "Abrir con Chrome o Tourya?" porque faltaba autoVerify).
- **Depende de `tourya-front` PR #117** mergeado: publica `assetlinks.json` en `public/.well-known/assetlinks.json` con el fingerprint SHA-256 del keystore de la app. Android descarga el JSON y valida la asociación automáticamente.
- Cierra deuda MO-43b registrada en MO-43 (2026-07-15).

### WompiHelper cleanup — hardening del checkout (mismo Sprint 4a)

- **Reemplazado `EscapeJs` manual** (solo escapaba `'` y newlines) por `System.Text.Json.JsonSerializer.Serialize` que cubre XSS/escape completo. Vulnerabilidad **moderada** cerrada: user-input del turista (nombre, teléfono, doc, email) que llegaba al HTML/JS del `WompiHelper.BuildCheckoutHtml` ya no puede escaparse del contexto JS con caracteres exóticos.
- **`redirectUrl`** ahora usa `Constants.WebBaseUrl` (antes hardcodeado a la URL de producción). Coherente con la migración a HTTPS `dev.tourya.co` (MO-01b). `CheckoutViewModel.cs:176`.

### Sprint 5a — Responder reseñas provider (cierra ❌ doc 14 "Responder reseñas provider")

- **`ProviderReviewsViewModel` + `ProviderReviewsPage`** con flujo completo de responder reseñas — TextEditor + botón Enviar/Cancelar + estado de loading.
- **7 keys i18n nuevas** en `I18nService` (es/en/pt) para labels, placeholders y mensajes de éxito/error.
- **Nuevo `StringNotEmptyToBoolConverter`** para habilitar el botón Enviar solo cuando el TextEditor tiene contenido.
- **Reusa `ReviewService.ReplyToReviewAsync`** (existía desde MO-20 con el fix del multipart) — sin cambios backend, sin cambios en el service.
- Cierra el gap ❌ del doc 14 "Responder reseñas provider" y actualiza a ✅ la fila correspondiente de la matriz "Gap Analysis contra el doc 14".

### Sprint 5b — Auditoría de 3 ítems ⚠️ del doc 14 (sin fixes, solo reporte)

Se auditaron los 3 ítems que aún estaban marcados ⚠️ verificar en la matriz "Gap Analysis contra el doc 14". Resultado:

1. **Reagendar reserva turista con 3 casuísticas** — ❌ NO IMPLEMENTADO. Registrado como deuda **MO-53** (~4-6h, P1). Fila del doc 14 actualizada de ⚠️ → ❌.
2. **Confirmar reserva manualmente (fallback QR)** — ✅ YA IMPLEMENTADO en `QrScannerPage.xaml:29-34` + `QrScannerViewModel.cs:164-183`. El doc 15 decía "En `ProviderReservationsPage`" (ubicación incorrecta); la implementación real vive en `QrScannerPage` (UX correcto — el operador está scaneando ahí). Filas del doc 14 actualizadas de ⚠️ → ✅.
3. **Login `mustChangePassword` operario** — ❌ NO IMPLEMENTADO. Solo `ProviderOperatorService.ResetPassword` (MO-24) permite que el PROVIDER resetee — no self-service del operario. **Security issue moderado**. Registrado como deuda **MO-54** (~3-4h, P1). Fila del doc 14 actualizada de ⚠️ → ❌.

### Estado del MVP mobile tras estos sprints

Con Sprint 3 A/B + 4a + 5a cerrados y las 2 deudas nuevas (MO-53/54) trackeadas:

- **Turista**: cerrado 100% del alcance del doc 14 excepto MO-53 (reagendar 3 casuísticas) y la integración con Travel Concierge (agente IA — fuera de scope MVP mobile).
- **Provider**: cerrado 100% del alcance del doc 14 — responder reseñas cerrada, TourFormPage completo con Hotel Pickup + Meeting Point cascade + autosave + upload background, ScheduleCalendarPage con calendario + copiar precios.
- **Operario**: cerrado excepto MO-54 (`mustChangePassword` primer login). Manual confirm + push funcionando.
- **Features "solo mobile"**: cerrado push + geo + deep-linking + offline + cámara para reseñas. Pendientes: wallet integration + widget "próxima reserva" (baja prioridad).

---

## Post-Sprint 6 — cierre 2026-08-14b

Cierre en bloque de las 2 deudas Sprint 5b (MO-53 + MO-54) + cierre técnico del WompiHelper cleanup abierto por Sprint 4a. Todos los cambios viven en `tourya-mobile` local (MO-00 sigue abierto — sin remoto Git).

### Sprint 6a — MO-54 `mustChangePassword` operario (commit `8f50f1b` + merge `8693c97`)

Cierra el **security issue moderado** registrado por Sprint 5b: el operario recién creado por el PROVIDER (via `ProviderOperatorService.ResetPassword` de MO-24) recibía una clave temporal pero el mobile lo dejaba entrar al home sin forzarlo a cambiarla.

- **Backend sin cambios** — ya exponía todo lo necesario:
  - `AuthenticationResponse.mustChangePassword` en la respuesta del `POST /auth/authenticate`.
  - Endpoint self-service `PATCH /users` en `UserController.java:30-38` que acepta el cambio de contraseña del usuario autenticado.
  - Este sprint es el **primer consumo desde mobile** de ambas piezas.
- **Mobile**:
  - **`AuthResponse`** (`Models/Auth/AuthResponse.cs`): nuevo campo `mustChangePassword: bool`.
  - **`LoginViewModel.LoginAsync`**: guard que chequea `authResponse.MustChangePassword` post-persist. Si `true`, `Shell.Current.GoToAsync("//change-password")` en vez de navegar al home. Bloquea el flujo hasta que el operario cambie la clave.
  - **`ChangePasswordPage` + `ChangePasswordViewModel`** nuevas: formulario simple (contraseña actual + nueva + confirmar) con validación client-side + botón "Cambiar".
  - **`ChangePasswordRequest` DTO** (`Models/Auth/ChangePasswordRequest.cs`).
  - **`AuthService.ChangeMyPasswordAsync`**: método cliente del `PATCH /users`.
  - **`ApiService.PatchAsync<TRequest>`**: nuevo helper generic para PATCH con body JSON (antes sólo existían `PatchAsync<TRequest, TResponse>` y `PatchMultipartAsync`).
  - **Ruta `change-password`** registrada en `AppShell.xaml` fuera de las tabs (no navegable manualmente por el user, sólo via el guard).
  - **DI** en `MauiProgram.cs` (VM + Page + Service extension).
  - **11 keys i18n** `auth.changePassword.*` en es/en/pt: title, subtitle, currentPassword/newPassword/confirmPassword labels + placeholders, submit button, success/error messages.

### Sprint 6b — MO-53 reagendar reserva turista 3 casuísticas (commit `58d7a4f` + merge `1af997a`)

Cierra la deuda MO-53 de Sprint 5b. Cubre las 3 casuísticas de RN-033 (EQUAL / LOWER / HIGHER precio) desde mobile.

- **Backend sin cambios** — ya exponía `PUT /reservations/{id}/reschedule` (`ReservationController:387`, implementado en BE-20). Este sprint es el **primer consumo desde mobile**.
- **Contrato del backend (importante para entender la implementación mobile)**:
  - Response `RescheduleReservationResponse` incluye `priceComparison: EQUAL/LOWER/HIGHER` + `status`.
  - **EQUAL** → reserva reagendada al nuevo día al mismo precio; nueva reserva con la nueva fecha, la anterior marcada `RESCHEDULED`. `status = RESCHEDULED`.
  - **LOWER** → reserva reagendada; se abre crédito al turista por la diferencia. `status = RESCHEDULED`.
  - **HIGHER** → **sorpresa del contrato**: el backend **NO** abre Wompi directo por el delta. Cancela la reserva anterior, abre crédito por el valor pagado y **agrega el nuevo tour al carrito** con el precio nuevo. `status = CANCELLED_AND_ADDED_TO_CART`. El turista completa el pago via el checkout normal del carrito — mismo patrón que web, decisión intencional del backend para reusar el pipeline de checkout.
- **Mobile**:
  - **`IReservationService.RescheduleReservationAsync(reservationId, newDate)`**: método cliente del endpoint.
  - **`RescheduleReservationRequest` + `RescheduleReservationResponse`** DTOs en `Models/Reservation/` (con `priceComparison` enum-like string y `status`).
  - **Botón "Reagendar"** en `ReservationDetailPage`: visible **sólo si `canReschedule == true`** (el flag ya venía en `ReservationDetails.CanReschedule` desde antes; MO-53 activa el uso).
  - **`RescheduleReservationPage` + `RescheduleReservationViewModel`** nuevas:
    - Lista de días disponibles (fuente: mismo servicio de schedules del tour, bounded por `MaxReschedulingDate` del backend).
    - Al tap → confirm dialog → llama `RescheduleReservationAsync`.
    - Dispatch client-side por `response.priceComparison`:
      - **EQUAL / LOWER** → `DisplayAlert` con mensaje explicando el resultado (incluye monto del crédito nuevo si LOWER) + `GoToAsync("//tourist/my-trips")`.
      - **HIGHER** → `DisplayAlert` explicando "tu reserva anterior fue cancelada, se abrió un crédito por lo pagado y el nuevo tour está en tu carrito; completá el pago para reservarlo" + `GoToAsync("//tourist/cart")`.
  - **12 keys i18n** `reservation.reschedule.*` en es/en/pt (title, subtitle, submit, mensajes por casuística, error genérico) + `common.ok` (utility key reusable).

### WompiHelper cleanup (commit `2cecbb2` + merge — cierre técnico chico)

Cierre técnico del hardening abierto por Sprint 4a. Sale como commit separado pero conceptualmente es la misma línea del WompiHelper.

- **`EscapeJs` manual** (que sólo escapaba `'` y newlines) reemplazado por `System.Text.Json.JsonSerializer.Serialize` para cubrir XSS/escape completo: `<`, `\`, `</script>`, unicode, etc. Cierra vulnerabilidad latente donde user-input del turista embebido en el HTML/JS del `WompiHelper.BuildCheckoutHtml` podía escaparse del contexto JS con caracteres exóticos (no explotable trivialmente desde la UI actual pero mala higiene).
- **`redirectUrl`** ahora usa `Constants.WebBaseUrl` (antes hardcodeado a la URL de producción). Coherente con MO-01b (`Constants.ApiBaseUrl` migrado a `https://dev.tourya.co/api/v1/`). `CheckoutViewModel.cs:176`.

### Estado del MVP mobile tras Sprint 6

Con Sprint 6a + 6b cerrados + WompiHelper cleanup:

- **Turista**: cerrado 100% del alcance del doc 14 **excepto** la integración con Travel Concierge (agente IA — fuera de scope MVP mobile). MO-53 cerrado.
- **Provider**: cerrado 100% del alcance del doc 14 (sin cambios respecto al Sprint 5a).
- **Operario**: cerrado 100% del alcance del doc 14. MO-54 cerrado — el security issue moderado ya no existe.
- **Features "solo mobile"**: cerrado push + geo + deep-linking + offline + cámara para reseñas. Pendientes: wallet integration + widget "próxima reserva" (baja prioridad, no bloqueantes).
- **Deudas nuevas Sprint 5b**: **cero abiertas** (MO-53 + MO-54 cerradas por Sprint 6).

### Notas sobre docs no tocados en este sync

- **Doc 05 (reglas de negocio)**: no se toca. MO-53 usa la RN-033 (reagendamiento) ya documentada; MO-54 no introduce RN nueva (es UX/security, no regla de negocio).
- **Doc 09 (API design)**: no se toca. Los endpoints `PATCH /users` (change password) y `PUT /reservations/{id}/reschedule` **ya existían en backend** y ya están documentados — este sprint es su primer consumo desde mobile.
