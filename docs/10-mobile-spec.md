# 10 — Mobile spec (MAUI Android)

App móvil de Tourya: `.NET MAUI 10` targeting Android. Soporta turistas y operadores en una sola app, con TabBars distintas según rol.

> ⚠️ **El código de mobile NO tiene remoto Git**. Solo existe localmente en `D:/Users/Usuario/source/repos/tourya/tourya-mobile/`.
> Ver [12 — Seguridad y autenticación](12-seguridad-y-auth.md) y [13 — Despliegue GCP y CI/CD](13-despliegue-cicd.md) para detalles.

---

## Stack

| Componente | Versión / Detalle |
|------------|-------------------|
| .NET MAUI | 10.0 (`net10.0-android`) |
| Target | Android único (no iOS hoy) |
| CommunityToolkit.Mvvm | 8.4.2 (source generators) |
| CommunityToolkit.Maui | 14.0.1 |
| Syncfusion.Maui.* | 33.1.46 (10 paquetes) |
| ZXing.Net.Maui | 0.7.4 (QR Scanner) |
| Microsoft.Extensions.Http | 10.0.5 |
| Microsoft.Extensions.Logging.Debug | 10.0.0 |
| Tipografía | IBM Plex Sans (Regular, Medium, SemiBold, Bold) |

---

## Estructura del proyecto

```
TouryaMobile/
├── App.xaml / App.xaml.cs          ← entry point, auto-login, role routing
├── AppShell.xaml / AppShell.xaml.cs ← Shell navigation
├── MauiProgram.cs                   ← DI container, registrations
├── Constants.cs                     ← API URL, roles, storage keys
├── MainPage.xaml                    ← splash placeholder
├── Views/                           (25 .xaml pages)
├── ViewModels/                      (26 VMs + BaseViewModel)
├── Services/                        (16 servicios)
├── Models/                          (10 namespaces por dominio)
├── Converters/                      (6 + I18nFieldConverter)
├── Helpers/                         (WompiHelper)
├── Controls/                        (ConnectivityBanner, ShimmerList, StateView)
├── Resources/
│   ├── Styles/                      (Colors.xaml, Styles.xaml ~25 KB)
│   ├── Fonts/                       (IBM Plex Sans)
│   ├── AppIcon/, Splash/
```

---

## Pantallas (25 XAML pages)

### Auth (4 pantallas)
- `LoginPage` — Email/password.
- `RegisterPage` — Registro de turista.
- `ForgotPasswordPage` — ⚠️ Sin endpoint backend implementado (`TODO`).
- `RoleSelectorPage` — Cambio entre tourist y provider.

### Turista — TabBar "tourist" (8 pantallas)
1. `ExplorePage` — Búsqueda con filtros (SearchBar + Picker + CollectionView infinite scroll).
2. `TourDetailPage` — Detalle: imágenes, ratings, descripciones.
3. `CartPage` — Carrito + checkout.
4. `CheckoutPage` — Selección de participantes + payment method.
5. `PaymentConfirmationPage` — Wompi WebView.
6. `MyTripsPage` — Reservas del turista.
7. `ReservationDetailPage` — Detalle de una reserva.
8. `ProfilePage` — Perfil, editar, settings.

### Provider — TabBar "provider" (13 pantallas)
1. `DashboardPage` — Estadísticas, revenue, bookings.
2. `ProviderToursPage` — Lista de tours del provider.
3. `ProviderReservationsPage` — Reservas de los tours.
4. `QrScannerPage` — Escáner QR para fulfillment.
5. `ProviderReviewsPage` — Reseñas + responses.
6. `KybStatusPage` — Estado del KYB.
7. `KybRegistrationPage` — Formulario KYB (datos).
8. `KybDocumentsPage` — Upload de documentos KYB.
9. `TourFormPage` — **Wizard multi-step** para crear/editar tour (Syncfusion TabView).
10. `TourSchedulesPage` — Schedules del tour.
11. `ScheduleTemplatesPage` — Plantillas reutilizables.
12. `ScheduleTemplateFormPage` — Crear/editar template.
13. `ScheduleCalendarPage` — Deploy plantilla a rango de fechas.

---

## TourFormPage — wizard multi-step

✅ Es la pantalla más compleja del mobile. ~6 pasos con `Syncfusion.Maui.TabView`:

| Step | Contenido | Campos del backend involucrados |
|------|-----------|-------------------------------|
| 0 | Información básica | `name` (TF), `description` (TF), `category`, `duration`, `durationEnum`, `subCategory`, `timeOfDay`, `maxPeople`, `isUnlimitedCapacity` |
| 1 | Ubicaciones / direcciones | `TourAddress[]` con tipo encuentro/finalización/recogida, lat/long |
| 2 | Atracciones principales | `TourMainAttraction[]` (es/en/pt) |
| 3 | Incluye / no incluye | `TourIncludesExcludes[]` con `type` |
| 4 | Itinerario | `TourItinerary[]` (day, time, title TF, description TF) |
| 5 | FAQ | `TourFaq[]` (question TF, answer TF) |
| 6 | Imágenes / galería | `TourGallery[]` (multipart upload) |

✅ `TourFormViewModel` tiene ~120 `[ObservableProperty]`s.

---

## ViewModels

**Pattern**: CommunityToolkit.Mvvm con source generators (`[ObservableProperty]`, `[RelayCommand]`, `[QueryProperty]`).

**Base**:
```csharp
public partial class BaseViewModel : ObservableObject
{
    [ObservableProperty] private bool _isBusy;
    [ObservableProperty] private string _title = string.Empty;
    [ObservableProperty] private bool _isRefreshing;
    public bool IsNotBusy => !IsBusy;
}
```

26 VMs registrados como **transient**. Total ~120-150 properties observables en toda la app.

---

## Services (16 servicios)

| Service | Interfaz | Propósito |
|---------|----------|-----------|
| `ApiService` | `IApiService` | HttpClient base (GET/POST/PUT/PATCH/DELETE/Multipart) |
| `AuthService` | `IAuthService` | Login, register, logout |
| `SecureStorageService` | `ISecureStorageService` | JWT + user en MAUI SecureStorage |
| `AuthHandler` | (DelegatingHandler) | Inyecta Bearer + maneja 401 → logout |
| `TourService` | `ITourService` | Búsqueda, detalles, locations |
| `I18nService` | `II18nService` | Traducción |
| `CartService` | `ICartService` | Operaciones carrito |
| `PaymentService` | `IPaymentService` | Pagos Wompi |
| `CreditService` | `ICreditService` | Créditos |
| `ReservationService` | `IReservationService` | Reservas CRUD |
| `ProviderService` | `IProviderService` | Provider dashboard, stats |
| `ReviewService` | `IReviewService` | Reseñas |
| `ConnectivityService` | `IConnectivityService` | Detección de red |
| `KybService` | `IKybService` | Onboarding KYB |
| `TourManagementService` | `ITourManagementService` | Creación/edición tour del provider |
| `ScheduleService` | `IScheduleService` | Plantillas de schedule |

Todos registrados como **singleton** en `MauiProgram.cs`.

---

## Wompi integration

**File**: `Helpers/WompiHelper.cs`.

### Flujo
1. Usuario llega a `PaymentConfirmationPage`.
2. Se monta un `WebView` con el widget de Wompi:
   - URL: `https://checkout.wompi.co/widget.js`
   - PublicKey: `pub_test_bIOZLLlzg8Oel52ljFIp7Sd4FDEOo1da` (test)
   - Currency: COP
3. Usuario completa el pago en el WebView.
4. Wompi redirige al callback (con `transactionId`).
5. App captura el `transactionId` y llama al backend (`POST /payment`).

⚠️ Aplicar las mismas advertencias que el flow 5 del [doc 06](06-flujos-y-eventos.md): sin webhook server-side, si el WebView se cierra entre Wompi success y `POST /payment`, queda en limbo.

---

## QR Scanner

**File**: `Views/QrScannerPage.xaml` + `ViewModels/QrScannerViewModel.cs`.

Librería: `ZXing.Net.Maui.Controls`.

**Flujo**:
1. PROVIDER abre la pantalla.
2. Apunta la cámara al QR del turista.
3. ZXing decodifica el contenido del QR (URL Tourya).
4. La app llama `POST /reservations/{id}/consume` para marcar como `DELIVERED`.

💡 TODO en el código: "try RES-XXX format" — sugiere que aún hay parsing pendiente para algunos formatos antiguos de QR.

---

## Navegación

Pattern: **Shell-based** (`AppShell.xaml`).

### Routes

```
Auth (sin tabs):
  login
  kyb-status
  role-selector

Tourist TabBar (tourist):
  Explorar
  Carrito
  Mis Viajes
  Perfil

Provider TabBar (provider):
  Dashboard
  Mis Tours
  Reservaciones
  QR Scanner
  Reseñas
```

### Route parameters

```csharp
[QueryProperty(nameof(TourId), "tourId")]
public partial class TourFormViewModel : BaseViewModel
```

```csharp
await Shell.Current.GoToAsync($"tour-detail?tourId={tour.Id}");
```

---

## Auto-login (App.xaml.cs)

✅ Al arrancar la app:
1. Lee JWT de `SecureStorage`.
2. Si existe y no expiró → carga datos del usuario.
3. Decide route inicial según rol:
   - `USER` → tourist tab.
   - `PROVIDER` con KYB aprobado → provider tab.
   - `PROVIDER` con KYB pendiente → `kyb-status`.
   - Sin login → `login`.

---

## Configuración hardcoded

⚠️ `Constants.cs`:

```csharp
public const string ApiBaseUrl = "http://44.203.38.85:8088/api/v1/";
```

❌ **Es la IP vieja de AWS**. Debería ser `https://tourya.co/api/v1/` o la URL de Cloud Run.

📌 PENDIENTE — actualizar URL al backend de GCP antes del próximo build.

---

## Estilo visual

### Colores principales (`Colors.xaml`)

| Token | Color |
|-------|-------|
| Primary | `#1b6475` (teal) |
| Secondary | `#FFCA18` (gold) |
| Success | greens |
| Danger | reds |
| Gray scale | Gray100 — Gray700 |

### Fuente
IBM Plex Sans (Regular, Medium, SemiBold, Bold).

### Estilos (`Styles.xaml` ~25 KB)
- PrimaryAction, SecondaryAction (button styles).
- Label, Entry, Picker, Editor base styles.
- Convenciones responsivas.

---

## Converters

| Converter | Propósito |
|-----------|-----------|
| `BoolToDayColorConverter` | Color del chip de día (blanco si seleccionado, primary si no) |
| `BoolToUploadTextConverter` | Texto del botón: "Subir" / "Resubir" |
| `IsNotNullConverter` | Visibility según null |
| `BoolToColorConverter` | Color por estado error/success |
| `BoolToIconConverter` | ✓ / ✗ |
| `CountToHeightConverter` | Altura dinámica de listas (min 55dp) |
| `I18nFieldConverter` | Convierte `TranslatedField` → string según idioma actual |

---

## Modelos

10 namespaces en `Models/`:

| Folder | Contenido |
|--------|-----------|
| Auth/ | LoginRequest, RegisterRequest, AuthResponse, RoleDto, SocialAuthRequest |
| Cart/ | ShoppingCartResponse |
| Common/ | PageResponse, TranslatedField |
| Credit/ | CreditDtos |
| Location/ | SearchLocationDto |
| Payment/ | PaymentDtos |
| Provider/ | DashboardDtos, KybDtos, ScheduleDtos, TourEnums, TourFormDtos |
| Reservation/ | ReservationDtos (con status enums) |
| Review/ | ReviewDtos, UpdateReviewRequest |
| Tour/ | SearchTourResponse, TourDetailResponse, SearchCategoryDto, ProviderTourDto, ParticipantSelection |

DTOs en records inmutables con camelCase JSON naming.

---

## Tech debt y TODOs en mobile

| # | Item | Severidad |
|---|------|-----------|
| 1 | URL backend hardcoded a AWS IP legacy | HIGH |
| 2 | Sin repo Git remoto (riesgo de pérdida) | HIGH |
| 3 | Syncfusion License key sin registrar (`MauiProgram.cs`) | MEDIUM |
| 4 | `ForgotPasswordViewModel` sin endpoint backend (TODO) | MEDIUM |
| 5 | iOS no soportado | LOW (decisión) |
| 6 | Sin push notifications | LOW (no implementado) |
| 7 | Sin shimmer loading completo | LOW |
| 8 | Sin cache de imágenes (cada navegación re-descarga) | LOW |
| 9 | QR parsing legacy "RES-XXX" pendiente | LOW |

---

## Estructura de commits

✅ 4 commits totales (rama `master`):
1. `1a46a54` — `feat: initial TouryaMobile .NET MAUI app`
2. `aaaef08` — `feat: provider modules - dashboard, KYB, tour creation, schedules, reviews`
3. `c942639` — `refactor: UI polish with Syncfusion components`
4. `0bc518f` — `refactor: UI polish for Login, Profile, Cart, Reservation Detail` ← último

📌 PENDIENTE — crear repo `tourya-mobile` en GitHub y subir el código.

---

## Cosas a definir con Luis

1. **iOS**: ¿se desarrolla en algún momento?
2. **Push notifications**: ¿es prioridad? ¿Firebase Cloud Messaging?
3. **Offline**: ¿hace falta cache para que la app funcione sin internet?
4. **Versionado de releases**: ¿Play Store? ¿qué CI/CD para mobile?
5. **App icon, splash, branding** definitivos.
6. **Política de privacidad / términos**: ¿están redactados? son obligatorios para publicar.
