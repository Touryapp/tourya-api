# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Run locally (requires .env file — see .env.example)
# Option 1: PowerShell script that loads .env and runs
powershell -File run.ps1

# Option 2: Manual — load env vars then run
./mvnw spring-boot:run

# Run tests
./mvnw test
```

**Server**: Port 8088, context path `/api/v1/`
**Swagger UI**: `http://localhost:8088/api/v1/swagger-ui/index.html`

## Environment

Requires a `.env` file (copy from `.env.example`):
- `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD` — PostgreSQL connection
- `AWS_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` — S3 file storage
- `GOOGLE_CLIENT_ID` — optional, for OAuth2

## Architecture

**Spring Boot 3.4.4 / Java 17 / PostgreSQL / Maven**

### Layered structure under `com.tourya.api`:
- **controller/** — REST endpoints (23+ controllers). Public routes under `/auth/**` and `/public/**`; everything else requires JWT Bearer token.
- **services/** — Business logic. Notable large services: `ReservationService`, `PaymentService`, `TourScheduleConfigGeneralService`, `ReviewService`, `ShoppingCartService`.
- **repository/** — Spring Data JPA repositories. Some use native queries via `@Query` and a custom `ReservationNativeRepository` impl.
- **models/** — JPA entities extending `BaseEntity` (provides auditing: createdDate, lastModifiedDate, createdBy).
  - **models/request/** — Inbound DTOs
  - **models/responses/** — Outbound DTOs
  - **models/mapper/** — Manual mappers (no MapStruct)
  - **models/specification/** — JPA Specification builders for dynamic queries
- **config/security/** — JWT auth: `SecurityConfig`, `JwtFilter`, `JwtService`. Stateless sessions, BCrypt passwords.
- **config/auth/** — Registration/login flows, OAuth2 (Google).
- **handler/** — `GlobalExceptionHandler` with `BusinessErrorCodes` enum for structured error responses.
- **constans/enums/** — Business enums with JPA converters (note: package is misspelled as "constans").
- **jobs/** — Scheduled tasks (e.g., `TemporalReservationExpiryJob`).
- **_utils/** — Utility classes.

### Key patterns:
- **i18n via JSONB**: Multi-language fields (es/en/pt) stored as JSONB in PostgreSQL, mapped to `TranslatedField` model with auto-fallback to Spanish. Affected tables: tour_address, tour_main_attractions, tour_includes_excludes, tour_faq, tour_itinerary, tour_cancellation_policy, tour_gallery.
- **JPA Auditing** enabled via `@EnableJpaAuditing` — entities track created/modified timestamps and user.
- **JSON Schema Validation**: Used for validating complex tour payloads.
- **Async processing**: `@EnableAsync` for email notifications and background tasks.
- **Scheduled tasks**: `@EnableScheduling` for cron jobs.

### External integrations:
- **Wompi** — Payment gateway (test env configured in application.properties)
- **AWS S3** — File/image storage
- **Gmail SMTP** — Email notifications via Thymeleaf templates
- **Google OAuth2** — Social login

## Database

- **Hibernate ddl-auto=none** — Schema managed manually via SQL migrations in `database/migrations/`.
- **Stored procedures** used for complex queries (e.g., `sp_get_tour_schedule_json`, `sp_get_provider_reservations`).
- Full DDL available in `database/ddl.sql`.
- Migration files are numbered sequentially with rollback scripts where applicable.

## Deployment

- **Dockerfile**: Multi-stage build (Maven 3.9.4 + OpenJDK 17), exposes port 8088.
- **AWS (develop branch)**: GitHub Actions → EC2 via `build_deploy_develop.yml`.
- **AWS CodeBuild**: `buildspec.yml` for ECR image builds.

## CORS

Allowed origins configured in `BeansConfig.java`: localhost:4200, localhost:8080, localhost:8100, and the production EC2 IP.
