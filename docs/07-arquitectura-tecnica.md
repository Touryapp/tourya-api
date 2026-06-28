# 07 — Arquitectura técnica

Stack tecnológico y arquitectura física de Tourya, con justificaciones donde se conocen.

---

## Componentes del sistema

```
┌──────────────────┐                  ┌──────────────────┐                  ┌──────────────────┐
│  Web app         │                  │ Mobile app       │                  │ Backoffice       │
│  Angular 19      │                  │ .NET MAUI 10     │                  │ (Angular 19)     │
│  Cloud Run       │                  │ Android          │                  │ (mismo Cloud Run)│
└────────┬─────────┘                  └────────┬─────────┘                  └────────┬─────────┘
         │                                     │                                     │
         └──────────────┬──────────────────────┴──────────────────┬──────────────────┘
                        │                                          │
                        │  HTTPS                                   │
                        ▼                                          ▼
              ┌──────────────────────────────────────┐
              │   GCP Cloud Load Balancer (HTTPS)    │
              │   34.160.22.16 → tourya.co           │
              │   /api/* → backend                   │
              │   /*     → frontend                  │
              └────────┬─────────────────────────────┘
                       │
                       ▼
              ┌────────────────────────┐
              │  tourya-dev-api        │
              │  Cloud Run (Java 17)   │
              │  Spring Boot 3.4.4     │
              │  --no-cpu-throttling   │
              └────────┬───────────────┘
                       │
            ┌──────────┼───────────────────────────────────┐
            │          │                                   │
            ▼          ▼                                   ▼
    ┌──────────┐  ┌──────────┐                    ┌──────────────────┐
    │ Cloud SQL│  │ GCS      │                    │ Servicios        │
    │ Postgres │  │ Storage  │                    │ externos         │
    │ (VPC)    │  │ (fotos,  │                    │ • Wompi          │
    └──────────┘  │ docs,    │                    │ • Gmail SMTP     │
                  │ QRs)     │                    │ • Firebase Auth  │
                  └──────────┘                    └──────────────────┘
```

---

## Stack por componente

### Backend (`tourya-api`)
| Componente | Versión | Notas |
|------------|---------|-------|
| Java | 17 (OpenJDK) | LTS |
| Spring Boot | 3.4.4 | |
| Spring Security | 6.x | JWT, BCrypt |
| Spring Data JPA | 3.x | Hibernate 6 |
| Maven | 3.9.4 | Build tool |
| PostgreSQL driver | (latest) | |
| Hibernate | 6.x | `ddl-auto=none`, migraciones manuales |
| Lombok | (latest) | |
| jjwt | **0.11.5** | Token handling (migrar a 0.12.x es tech debt) |
| AWS SDK v2 | 2.25.26 | S3 (legacy, soporte aún activo) |
| google-cloud-storage | 2.40.0 | Producción actual |
| Spring Mail | (latest) | SMTP |
| Jakarta Validation | 3.x | `@Valid`, `@NotBlank`, etc. |
| Thymeleaf | (latest) | Templates de email |

**Puerto**: 8088 (local), context path `/api/v1/`.

### Frontend web (`tourya-front`)
| Componente | Versión | Notas |
|------------|---------|-------|
| Angular | 19 | |
| TypeScript | 5.x | |
| Firebase JS SDK | (latest) | Auth Google/Facebook |
| RxJS | 7.x | |
| Bootstrap / SCSS | | Estilos |
| ngx-translate o similar | | i18n UI |

Build: `npm run build` → Nginx serve.

### Mobile (`tourya-mobile`)
| Componente | Versión | Notas |
|------------|---------|-------|
| .NET MAUI | 10.0 (net10.0-android) | **Solo Android hoy** |
| CommunityToolkit.Mvvm | 8.4.2 | Source generators |
| Syncfusion.Maui.* | 33.1.46 | UI components (TabView, Inputs, Calendar, Maps, Barcode, ListView, DataForm, Popup, Buttons) |
| ZXing.Net.Maui | 0.7.4 | QR/Barcode scanning |
| WompiHelper.cs | | WebView integration |

⚠️ **Sin remoto Git** — el código solo está localmente, en riesgo.

### Base de datos
| Componente | Notas |
|------------|-------|
| PostgreSQL | Cloud SQL en GCP |
| Schema | Manejado por migraciones manuales (`database/migrations/`) |
| Acceso | VPC connector (sin IP pública) |

---

## Arquitectura backend (`tourya-api`)

### Estructura de paquetes

```
com.tourya.api/
├── controller/         (35 controllers — REST endpoints)
├── services/           (lógica de negocio — ~30 servicios)
│   └── impl/           (implementaciones)
├── repository/         (Spring Data JPA, queries nativas)
│   └── impl/           (implementaciones de queries complejas)
├── models/             (entidades JPA — 54 entidades)
│   ├── request/        (DTOs de entrada)
│   ├── responses/      (DTOs de salida)
│   ├── mapper/         (manual mappers, sin MapStruct)
│   └── specification/  (JPA Specifications para queries dinámicas)
├── config/
│   ├── security/       (SecurityConfig, JwtFilter, JwtService)
│   ├── auth/           (Auth flows: registration, social, login)
│   ├── BeansConfig.java
│   └── OpenApiConfig.java
├── handler/            (GlobalExceptionHandler, BusinessErrorCodes)
├── constans/enums/     (enums con JPA converters — nota: typo "constans")
├── jobs/               (TemporalReservationExpiryJob, ReservationCancellationFlagsJob, ProviderPayoutOrderJob)
└── _utils/             (utilidades — ReservationDisplayId, TourDurationUtils, TouryaPriceCalculator, Utils)
```

### Patrones técnicos en uso

| Patrón | Dónde | Notas |
|--------|-------|-------|
| **REST con DTO request/response** | `controller/` → `services/` | Separación clara request/response/entity |
| **Manual Mappers** | `models/mapper/` | Sin MapStruct (decisión de mantener simple) |
| **JPA Specifications** | `models/specification/` | Para queries dinámicas con filtros |
| **Stored Procedures** | Llamadas desde repositorios | Para queries complejas (búsqueda, payouts) |
| **AttributeConverter** | `TranslatedFieldConverter`, otros | JSONB ↔ POJO |
| **@Async + @EnableAsync** | Email, generación QR | Sin broker, async simple |
| **@Scheduled** | Jobs en `jobs/` | Cron en zona Bogotá |
| **@PreAuthorize** | Algunos endpoints | RBAC declarativo |
| **GlobalExceptionHandler** | `handler/` | Error responses estructuradas con `BusinessErrorCodes` enum |

### Capas

```
HTTP Request
    │
    ▼
┌──────────────────┐
│  JwtFilter        │ ← extrae Bearer, valida, setea SecurityContext
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│  Controller       │ ← @RestController, valida @Valid request
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│  Service          │ ← lógica de negocio, @Transactional
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│  Repository       │ ← Spring Data JPA + queries nativas + SPs
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│  PostgreSQL       │
└──────────────────┘
```

---

## Arquitectura frontend (`tourya-front`)

### Estructura general

```
src/app/
├── auth/                          (login, register, activate, password recovery)
│   └── login-tourist/             (componente principal de login)
├── core/
│   ├── interceptors/auth.interceptor.ts   ← inyecta Bearer
│   ├── guards/                            (auth, admin, redirect guards)
│   └── services/                          (auth.service, etc.)
├── pages/
│   ├── clients/                   (vistas del turista)
│   │   ├── home-clients
│   │   ├── cart-summary
│   │   ├── my-profile
│   │   ├── client-credits
│   │   ├── client-dashboard
│   │   ├── list-tours
│   │   ├── tours-detail
│   │   ├── booking-confirm
│   │   ├── booking-tours
│   │   └── tour-booking-confirmation
│   ├── providers/                 (vistas del proveedor)
│   │   ├── provider-panel
│   │   ├── tours/                 (add-tour, tour-details, tour-list, tour-schedule)
│   │   ├── provider-payments
│   │   ├── provider-reviews
│   │   ├── provider-users
│   │   ├── provider-tour-management
│   │   └── templates              (schedule templates)
│   ├── admin/                     (dashboard, tour-admin-list)
│   ├── requestproviders/          (KYB)
│   └── maritime-activity-reports/
├── shared/
│   ├── common/                    (default-header, footer, floating-cart)
│   ├── dto/                       (interfaces TS)
│   ├── enums/                     (Roles, etc.)
│   ├── pipe/localized-name/       (pipe para TranslatedField)
│   └── services/
└── environments/                  (environment.ts, environment.prod.ts)
```

### Patrones técnicos

| Patrón | Notas |
|--------|-------|
| **Lazy loading** de módulos | Por área (clients, providers, admin) |
| **Interceptor HTTP** | Auto-inyecta Bearer token |
| **Route Guards** | Auth, Admin, Redirect (basado en rol) |
| **i18n** | ngx-translate con `en.json`, `es.json`, `pt.json` (~1000 keys cada uno) |
| **TranslatedField pipe** | `| localizedName` para mostrar texto multilingüe del backend |
| **Firebase Auth SDK** | Para login social (Google + Facebook) |
| **LocalStorage** | JWT (`token`), user (`user`), `requestProviderStatus` |
| **Bootstrap + custom SCSS** | UI base |

---

## Arquitectura mobile (`tourya-mobile`)

### Estructura

```
TouryaMobile/
├── App.xaml.cs                    ← entry point + auto-login + role routing
├── AppShell.xaml                  ← Shell navigation
├── MauiProgram.cs                 ← DI setup
├── Constants.cs                   ← API URL, roles, storage keys
├── Views/                         (25 XAML pages)
│   ├── Auth: Login, Register, ForgotPassword, RoleSelector
│   ├── Tourist (TabBar): Explore, TourDetail, Cart, Checkout, PaymentConfirmation, MyTrips, ReservationDetail, Profile
│   └── Provider (TabBar): Dashboard, Tours, Reservations, QrScanner, Reviews + KYB pages + Tour wizard
├── ViewModels/                    (26 VMs + BaseViewModel)
│   └── Notables grandes: TourFormViewModel (wizard 6+ steps), ScheduleTemplateFormViewModel
├── Services/                      (16 servicios)
│   ├── ApiService                 (HttpClient base)
│   ├── AuthService                (login/register/logout)
│   ├── AuthHandler                (delegating handler para Bearer)
│   ├── SecureStorageService       (MAUI SecureStorage)
│   ├── + servicios por dominio: Tour, Cart, Payment, Credit, Reservation, Provider, Review, Connectivity, Kyb, TourManagement, Schedule
├── Models/                        (10 namespaces por dominio)
├── Helpers/WompiHelper.cs         ← Wompi WebView integration
├── Converters/                    (6 value converters + I18nFieldConverter)
└── Resources/
    ├── Styles/Colors.xaml + Styles.xaml
    └── Fonts/                     (IBM Plex Sans)
```

### Patrones técnicos

| Patrón | Notas |
|--------|-------|
| **MVVM con CommunityToolkit.Mvvm** | `[ObservableProperty]`, `[RelayCommand]`, source generation |
| **Shell-based navigation** | Routes por tabbar (tourist vs provider) |
| **Constructor injection** | Singleton services + transient pages/VMs |
| **DelegatingHandler** | `AuthHandler` para Bearer + 401 → logout |
| **MAUI SecureStorage** | JWT y user JSON |
| **WebView (Wompi)** | Pasarela embebida |
| **Syncfusion components** | UI rica (SfComboBox, SfCheckBox, SfButton, futuros) |
| **ZXing.Net.Maui** | QR Scanner para fulfillment de reservas |

### Pendientes / Tech debt mobile

- ⚠️ **URL hardcoded a la IP vieja de AWS** (`http://44.203.38.85:8088/api/v1/`). Debe actualizarse a `https://tourya.co/api/v1/` o la URL de GCP.
- ⚠️ **Sin repo Git remoto** — el código solo está local.
- ⚠️ **Syncfusion License key sin registrar** (`MauiProgram.cs` TODO).
- ⚠️ **Forgot Password sin endpoint backend** (`ForgotPasswordViewModel` TODO).
- ❓ **Solo Android** — iOS no está habilitado en el `.csproj`.

---

## Infraestructura GCP

### Proyecto
- **Project ID**: `wass-project`
- **Región principal**: `us-east1`

### Servicios GCP en uso

| Servicio | Para qué | Notas |
|----------|----------|-------|
| **Cloud Run** | Hosting de backend y frontend | `tourya-dev-api`, `tourya-front` |
| **Cloud SQL** | PostgreSQL | Privado vía VPC connector |
| **Cloud Storage (GCS)** | Imágenes, comprobantes, documentos KYB, QRs | `IStorageService` lo abstrae |
| **Cloud Build** | CI/CD para deploy a Cloud Run | Configurado vía `cloudbuild.yaml` |
| **Artifact Registry** | Imágenes Docker | |
| **VPC Connector** | Cloud Run ↔ Cloud SQL privado | |
| **Load Balancer** | HTTPS + dominio | `34.160.22.16` → `tourya.co` |
| **IAM Service Account** | Cloud Run identity | ⚠️ key en repo es vulnerabilidad |

### Configuración crítica de Cloud Run

- **`--no-cpu-throttling`**: ✅ ACTIVO. Razón: el `@Async` para envío SMTP fallaba con throttling porque Cloud Run cortaba la CPU durante el TLS handshake (50s+). Sin throttling, el email async funciona.
- **Min instances**: ❓ (probablemente 0 — escala desde cero).
- **CPU/Memory**: ❓.

📌 PENDIENTE — confirmar tier de Cloud Run.

### Dominios

| Dominio | Apunta a |
|---------|----------|
| `tourya.co` | Load Balancer GCP (DNS en GoDaddy, cliente) |
| `tourya-dev-api-5j2nd2oflq-ue.a.run.app` | URL directa Cloud Run (dev) |
| `tourya-front-24ohzuhvrq-ue.a.run.app` | URL directa Cloud Run (front) |

---

## Decisiones arquitectónicas

### Decisión 1 — Monolito (no microservicios)

> **Decisión**: Mantener Tourya como **monolito Spring Boot** (un solo despliegue, una sola BD).
>
> **Por qué**:
> - Equipo pequeño.
> - Dominios fuertemente acoplados (carrito → reserva → pago → payout → review).
> - Una sola transacción ACID simplifica la lógica de checkout.

📌 PENDIENTE LUIS — confirmar que no hay planes de microservicios a corto plazo.

### Decisión 2 — JSONB para i18n (no tablas separadas)

> **Decisión**: Multi-idioma vía `TranslatedField` JSONB inline.
>
> **Por qué**:
> - Una sola query trae el texto en todos los idiomas.
> - Sin joins por idioma.
> - Índices GIN dan búsqueda full-text rápida.

> **Trade-off**: validación de schema más débil que tablas relacionales.

### Decisión 3 — Hibernate `ddl-auto=none`

> **Decisión**: Schema gestionado manualmente vía SQL migrations.
>
> **Por qué**:
> - Control total (especialmente con SPs y JSONB).
> - Evita sorpresas en producción.

> **Trade-off**: dev debe correr scripts a mano. Hay un `MIGRACIONES_A_EJECUTAR.txt` que lista pendientes.

### Decisión 4 — Stored Procedures para queries complejas

> **Decisión**: Búsqueda de tours y reservaciones del provider via SP (`sp_get_tour_schedule_json`, `sp_get_provider_reservations`).
>
> **Por qué**:
> - Performance (la SP construye el JSON aggregado en BD, no en JVM).
> - Filtros muy dinámicos (20+ dimensiones).

> **Trade-off**: lógica fuera de Java, harder to test, harder to change.

### Decisión 5 — Storage abstrayendo S3 y GCS

> **Decisión**: Interfaz `IStorageService` con dos implementaciones (`S3Service`, `GcsStorageService`).
>
> **Por qué**:
> - Migración GCP gradual sin romper AWS legacy.
> - Decisión runtime via `storage.provider` property.

### Decisión 6 — `--no-cpu-throttling` en Cloud Run

> **Decisión**: Desactivar throttling de CPU.
>
> **Por qué**:
> - `@Async` para envío SMTP fallaba con TLS handshake interrumpido por throttling.
> - Costo mayor pero compensa con confiabilidad de emails.

### Decisión 7 — Solo Android en mobile

> **Decisión actual**: MAUI Android único target.
>
> **Por qué**: ❓ — probablemente decisión de prioridad (iOS requiere Mac para build, más burocracia App Store).

📌 PENDIENTE LUIS — ¿iOS está en roadmap?

### Decisión 8 — Sin webhook server-side de Wompi

> **Decisión actual**: confirmación de pago vía cliente (`POST /payment` desde el front después del checkout Wompi).
>
> **Trade-off**: si el cliente cierra la app entre Wompi success y POST `/payment`, el pago queda huérfano.
>
> **Riesgo**: ⚠️ pagos pendientes sin sincronizar.

📌 PENDIENTE LUIS — confirmar si el cliente quiere webhook.

---

## Tech debt y vulnerabilidades

| # | Item | Severidad | Documento |
|---|------|-----------|-----------|
| 1 | JWT secret hardcoded en `application.properties` | CRITICAL | `security-remediation-plan.md` |
| 2 | Wompi integrity secret hardcoded | CRITICAL | `security-remediation-plan.md` |
| 3 | Actuator full exposure | CRITICAL | `security-remediation-plan.md` |
| 4 | Activation token sin protección de replay | CRITICAL | `security-remediation-plan.md` |
| 5 | PII leak en `/public/bookings` | CRITICAL | `security-remediation-plan.md` |
| 6 | Social login sin validación de token Firebase | CRITICAL | `security-remediation-plan.md`, `social-login-google-facebook.md` |
| 7 | `tourya-dev-sa-key.json` commiteado en repo | HIGH | `security-remediation-plan.md` |
| 8 | jjwt en 0.11.5 (debería ser 0.12.x) | MEDIUM | — |
| 9 | `tourya-mobile` sin remoto GitHub | HIGH | — |
| 10 | URL backend hardcoded en mobile (AWS legacy) | HIGH | — |
| 11 | CI/CD sin coverage gate ni Snyk | MEDIUM | `cicd-improvement-plan.md` |
| 12 | Sin webhook Wompi server-side | MEDIUM | — |
