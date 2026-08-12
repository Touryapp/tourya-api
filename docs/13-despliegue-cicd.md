# 13 — Despliegue GCP y CI/CD

Cómo está montada la infraestructura y los pipelines de Tourya. Estado actual: **híbrido AWS (legacy) + GCP (actual)**.

---

## Estado actual del despliegue

| Componente | Producción actual | Notas |
|------------|-------------------|-------|
| Backend (`tourya-api`) | GCP Cloud Run (`tourya-dev-api`) | Migrado desde AWS EC2 |
| Frontend (`tourya-front`) develop branch | AWS EC2 | Pipeline GitHub Actions |
| Frontend (`tourya-front`) develop_ftmg branch | GCP Cloud Run (`tourya-front`) | Landing del cliente |
| Frontend (`tourya-front`) main branch | (pendiente) | Cloud Build trigger pendiente |
| Mobile | Sin pipeline. Build manual a APK | Sin Play Store ni distribución oficial |

---

## Infraestructura GCP

### Proyectos (2 ambientes)

| Ambiente | Project ID | Uso |
|----------|------------|-----|
| **Dev / staging** | `tourya-project-dev` | Donde trabajamos día a día. Fase 0 y siguientes se aplican aquí primero |
| **Producción** | `tourya-project-493820` | No tocar hasta validar en dev |

- **Región principal**: `us-east1`
- **Owners GCP**: `franklinmarcano1970@gmail.com`, `luis.mendoza@wass.com.co`

### Servicios habilitados en `tourya-project-dev`

| Servicio | Uso | Estado |
|----------|-----|--------|
| **Cloud Run** | Backend y frontend serverless | ✅ Activo |
| **Cloud SQL** | PostgreSQL 15 (privado vía VPC) | ✅ Activo |
| **Cloud Storage (GCS)** | Imágenes, comprobantes, documentos KYB, QRs | ✅ Activo |
| **Cloud Build** | CI/CD pipeline | ✅ Activo |
| **Artifact Registry** | Docker images | ✅ Activo |
| **VPC Connector** | Cloud Run ↔ Cloud SQL privado | ✅ Activo |
| **Load Balancer (HTTPS)** | Routing + dominio | ✅ Activo |
| **Secret Manager** | Para secretos runtime | ✅ Habilitado en `tourya-project-dev` (2026-07-08). 4 secretos activos: `tourya-jwt-key`, `tourya-wompi-integrity-secret`, `tourya-db-password`, `tourya-smtp-password` |
| **Workload Identity Federation** | Autenticación GitHub Actions → GCP sin keys JSON | ✅ Habilitado en `tourya-project-dev`. Pool `github-actions-pool` + provider `github-oidc` con condición `repository_owner == 'Touryapp'`. IAM binding sobre SA `tourya-dev-cloud-run` incluye **`Touryapp/tourya-api`** (desde 2026-07-08) y **`Touryapp/tourya-front`** (agregado 2026-07-10) |

### Cloud Run

| Servicio | URL |
|----------|-----|
| `tourya-dev-api` | https://tourya-dev-api-640622322458.us-east1.run.app |
| `tourya-dev-front` | https://tourya-dev-front-640622322458.us-east1.run.app |

Service Account: `tourya-dev-cloud-run@tourya-project-dev.iam.gserviceaccount.com`

### Cloud SQL

| Item | Valor |
|------|-------|
| Instancia | `tourya-dev-db` |
| Motor | PostgreSQL 15 |
| Tier | `db-f1-micro` (mínimo) |
| Ubicación | `us-east1-c` |
| IP pública | `34.148.43.142` |
| IP privada | `192.168.0.3` (vía VPC connector) |
| Estado | RUNNABLE |

### Dominios

| Dominio | Apunta a | DNS |
|---------|----------|-----|
| `tourya.co` | Load Balancer `34.160.22.16` | GoDaddy (cliente) |
| `tourya-dev-api-640622322458.us-east1.run.app` | Cloud Run backend (dev) | GCP |
| `tourya-dev-front-640622322458.us-east1.run.app` | Cloud Run frontend (dev) | GCP |

📌 PENDIENTE — confirmar si hay subdominios planeados (`api.tourya.co`, `admin.tourya.co`).

---

## Configuración Cloud Run del backend

### Servicio: `tourya-dev-api`
- **Imagen**: del Artifact Registry tras build.
- **Puerto**: 8088 (interno).
- **CPU throttling**: ⚠️ **DESACTIVADO** (`--no-cpu-throttling`). Razón: SMTP `@Async` fallaba con throttling durante TLS handshake.
- **Memoria / CPU**: ❓ — verificar.
- **Min instances**: ❓ (probable 0).
- **Max instances**: ❓.
- **VPC Connector**: sí (para Cloud SQL privado).

### Variables de entorno críticas

Estado en `tourya-dev-api` (dev) al 2026-07-08:

**Plain env vars (no sensibles)**:
```
DB_HOST, DB_PORT, DB_USER
MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_STARTTLS, MAIL_SSL, MAIL_DEBUG
ACTIVATION_URL
STORAGE_PROVIDER=GCP
GCS_BUCKET, GCP_PROJECT_ID
WOMPI_PUBLIC_KEY
AWS_REGION, AWS_BUCKET (legacy)
```

**Secret Manager refs (inyectados vía `--set-secrets`)**:
```
JWT_SECRET                → tourya-jwt-key
WOMPI_INTEGRITY_SECRET    → tourya-wompi-integrity-secret
WOMPI_EVENTS_SECRET       → tourya-wompi-events-secret        (agregado 2026-07-08, PR #156)
DB_PASSWORD               → tourya-db-password
MAIL_PASSWORD             → tourya-smtp-password
```

### Referencia: patrón de WASS

**Ya replicado en Tourya dev** (2026-07-08). El proyecto hermano `wass-project-dev` **también tiene Secret Manager configurado** con esquema similar:

```
wass-jwt-key
wass-db-app, wass-db-owner
wass-smtp-password
wass-syncfusion
wass-wompi-integrity-secret, wass-wompi-private-key, wass-wompi-public-key, wass-wompi-events-secret
```

Nomenclatura equivalente para Tourya (a crear): `tourya-jwt-key`, `tourya-wompi-integrity-secret`, `tourya-smtp-password`, `tourya-db-app`, `tourya-db-owner`.

### Histórico relevante

- ⚠️ 19 revisiones acumuladas → se limpió a 2 (revisión activa `00021-dtf` + rollback `00020-f94`).
- ✅ Migración SMTP de `eowkin@gmail.com` (Gmail personal) → `noreply@wass.com.co` (Workspace SMTP Relay).

---

## CI/CD

### 1. AWS EC2 (legacy, branch `develop`)

✅ Pipeline GitHub Actions: `.github/workflows/build_deploy_develop.yml`.

Flow:
1. Push a `develop`.
2. GitHub Actions builds Docker image.
3. Push a AWS ECR vía CodeBuild.
4. Deploy a EC2 vía AWS CodeDeploy (`appspec.yml`).

Usado para mantener el entorno AWS de turistas legacy.

### 2. GCP Cloud Build (frontend en `develop_ftmg`)

✅ Trigger configurado: push a `develop_ftmg` → Cloud Build → deploy a Cloud Run `tourya-front`.

### 3. GCP Cloud Build (backend en `main`)

📌 PENDIENTE — comando de creación del trigger:
```bash
gcloud beta builds triggers create github \
  --repo-name=tourya-api \
  --repo-owner=Touryapp \
  --branch-pattern='^main$' \
  --build-config=cloudbuild.yaml \
  --name=tourya-front-deploy-main
```

### 4. Mobile

❌ Sin pipeline. Build manual a APK.

📌 PENDIENTE — definir flujo de release mobile (Play Store).

---

## Ramas Git por repo

### `tourya-api` (GitHub `Touryapp/tourya-api`)

| Rama | Para qué |
|------|----------|
| `develop` | Desarrollo, deploy a AWS EC2 |
| `develop_infra` | Cambios de infra |
| `feature/gcp-migration` | Trabajos de migración a GCP |
| `feature/reschedule` | Trabajo en feature de reschedule |
| `feature/reschedule-overrides-fix` | (rama nueva en remoto) |
| `main` | (atrasada de develop por 2 commits) |

### `tourya-front` (GitHub `Touryapp/tourya-front`)

| Rama | Para qué |
|------|----------|
| `develop` | Desarrollo, deploy a AWS EC2 |
| `develop_ftmg` | **Landing page** (versión deployed a GCP). Cambios visuales temporales para presentación cliente |
| `develop_infra` | Workflows GitHub Actions |
| `feature/gcp-migration` | Migración GCP |
| `feature/homeCardsRefactor` | (en desarrollo) |
| `feature/lastRequirementsDocument` y `...2` | (nuevas en remoto, posiblemente de otro dev) |
| `main` | (atrasada) |

### `tourya-mobile`

⚠️ Solo `master` local. **Sin remoto GitHub**.

---

## Dockerfile (`tourya-api`)

Multi-stage build. Estado actual tras TC-020 #235 (2026-08-11):

```Dockerfile
# Stage 1: Build
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

# TC-020 #235 bug (c): forzar TZ Bogota en el container y en el JVM
# Cloud Run defaults to UTC; sin esto, LocalDateTime.now() bare (~28 sitios en
# services + jobs) resolvía a UTC en dev/prod → offset de 5h respecto al calendario
# del turista. Ver RN-060 en doc 05.
RUN apt-get update && apt-get install -y --no-install-recommends tzdata && rm -rf /var/lib/apt/lists/*
ENV TZ=America/Bogota

WORKDIR /app
COPY --from=build /app/target/*SNAPSHOT.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseContainerSupport", \
            "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=America/Bogota", \
            "-jar", "/app/app.jar"]
```

### Nota sobre `PORT` env var

✅ Cloud Run inyecta `PORT=8080`. El backend Spring Boot lee `SERVER_PORT` desde `application.properties`; en Cloud Run se mapea al puerto expuesto. La entrada AWS EC2 fue retirada (ver `develop` post-migración GCP 2026-07).

### Timezone (TC-020 #235, ciclo agosto 2026)

Cadena de defensa en profundidad para garantizar hora Colombia en toda fecha generada por el backend:

1. **Container OS**: `tzdata` instalado + `ENV TZ=America/Bogota` → `/etc/localtime` apunta a `America/Bogota`.
2. **JVM**: `-Duser.timezone=America/Bogota` en el `ENTRYPOINT` (redundante con TZ; garantiza el fallback si algún proceso spring lee la propiedad directamente).
3. **JPA Auditing**: `JpaAuditingConfig` expone bean `DateTimeProvider` que retorna `LocalDateTime.now(BOGOTA)`. Spring Data JPA lo usa al poblar `@CreatedDate/@LastModifiedDate`.

**Impacto operativo**:
- Antes: `PendingReservationNoShowJob` cron "7am" corría a las 2am Bogota (7am UTC).
- Después: corre a las 7am Bogota real (12pm UTC).
- Igual para `CreditExpirationJob` (5am), `TourReminder24hJob` (8am), y todos los `now()` bare en `ReservationService`.

Ver [RN-060 en doc 05](05-reglas-de-negocio.md#rn-060).

---

## Frontend deploy

### Para web Angular

Build: `npm run build` → genera `dist/`.

Servido vía Nginx en Cloud Run.

### Para landing temporal (`develop_ftmg`)

✅ Cambios visuales en esta rama (temporales para presentación cliente):
- **Footer**: agregado "Touya Marketplace S.A.S" y "RNT: 263565 - NIT: 901.961.052-4".
- **Navbar**: removidos todos los links de navegación y user profile.
- **Hero**: reemplazado el search form por "Próximamente".
- **Top Tours**: oculto (display: none).

⚠️ Estos cambios son temporales — luego se revierten para volver a la app completa.

---

## Migraciones de base de datos

✅ Manuales: `psql -f database/migrations/XXX_*.sql`.

Hay un archivo `MIGRACIONES_A_EJECUTAR.txt` que mantiene la lista de migraciones pendientes para cada entorno.

📌 PENDIENTE — definir si se quiere automatizar (Flyway, Liquibase).

---

## Logs y monitoreo

| Sistema | Servicio |
|---------|----------|
| Logs backend | Cloud Logging (auto-stream desde Cloud Run) |
| Métricas | Cloud Monitoring (auto) |
| Alertas | ✅ **7 alertas activas en dev** (2026-07-08). Falta cost anomaly (requiere Billing Budget separado) |
| Tracing | ❓ — no configurado |
| APM | ❌ Sentry / Datadog no integrados |

✅ **Decisión Franklin (2026-07-07)**: **configurar alertas básicas de Cloud Monitoring es prerequisito antes de salir a producción**. Estamos por operar con dinero real (pagos Wompi, payouts a operadores). Sin alertas, el equipo se entera de los problemas por reclamos del turista.

### Alertas activas en dev (INF-01 completado 2026-07-08)

Canal de notificación: **email a `franklinmarcano1970@gmail.com`**.

| # | Alerta | Umbral configurado | Estado |
|---|--------|--------------------|:------:|
| 1 | Tourya API - HTTP 5xx errors | > 0.05 req/s (~3/min) por 5 min | ✅ Activa |
| 2 | Tourya Frontend - HTTP 5xx errors | > 0.05 req/s (~3/min) por 5 min | ✅ Activa |
| 3 | Tourya API - P95 latency | > 3s por 5 min | ✅ Activa |
| 4 | Cloud SQL - CPU utilization | > 85% por 10 min | ✅ Activa |
| 5 | Cloud SQL - Disk utilization | > 80% por 5 min | ✅ Activa |
| 6 | Cloud SQL - Connections | > 80 conexiones por 5 min | ✅ Activa |
| 7 | Tourya API - CPU utilization | > 95% (percentil 99) por 10 min | ✅ Activa |
| 8 | Cost anomaly | > 2× media histórica en 24h | ⚠️ **Pendiente** — requiere Billing Budget separado (permisos de Billing Account) |

Auto-close de alertas: 30 minutos (`autoClose: 1800s`).

Costo: prácticamente cero para el volumen inicial.

---

## CI/CD propuesto (futuro)

> Plan completo en `cicd-improvement-plan.md` (interno).

Fases:

1. **JaCoCo + SpotBugs + PMD**: análisis estático + coverage gate (80%).
2. **Snyk Free**: scanning de vulnerabilidades en dependencias (100 tests/mes).
3. **Branch Protection**: bloquear merge a `develop` / `main` si CI falla.
4. **Webhook Wompi** (si se implementa).
5. **Mobile pipeline**: GitHub Actions → build APK → upload a Play Store internal track.

---

## Variables de entorno por archivo `.env.example`

✅ Tourya tiene un `.env.example` en `tourya-api/`:

```
DB_HOST=
DB_PORT=
DB_USER=
DB_PASSWORD=

AWS_BUCKET=
AWS_REGION=
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=

GOOGLE_CLIENT_ID=
```

📌 Falta: variables de Wompi, JWT, SMTP, GCS — agregar al template.

---

## Backups

| Recurso | Backup |
|---------|--------|
| Cloud SQL PostgreSQL (dev) | ✅ **Backups automáticos habilitados** (2026-07-08, INF-02): diarios a las 03:00, retention 30 días, multi-region `us`, Point-in-Time Recovery activo con transaction logs 7 días |
| GCS bucket | versioning ❓ |
| Repos GitHub | GitHub mismo (snapshots por commits) |
| Mobile code | ⚠️ **NO** — solo local |

📌 PENDIENTE — confirmar política de backup de Cloud SQL.

---

## Acceso

| Recurso | Quién tiene acceso |
|---------|--------------------|
| GCP Console | Franklin, Luis |
| GitHub `Touryapp/` | ❓ — varios devs (verificar) |
| Wompi Dashboard | ❓ — el cliente |
| GoDaddy DNS | El cliente |
| Google Cloud Console Firebase | ❓ |
| Workspace `wass.com.co` | Luis (admin) |

📌 PENDIENTE LUIS — confirmar lista de accesos.

---

## Riesgos operacionales identificados

| # | Riesgo | Severidad |
|---|--------|-----------|
| 1 | `tourya-mobile` no tiene remoto Git → pérdida si falla disco | HIGH |
| 2 | `develop_ftmg` tiene 10 commits sin pushear → pérdida si falla disco | HIGH |
| 3 | Sin webhook Wompi → pagos huérfanos posibles | HIGH |
| 4 | Service account key commiteado en repo | HIGH |
| 5 | Sin alertas de Cloud Monitoring | MEDIUM |
| 6 | Sin retry policy en jobs | MEDIUM |
| 7 | Sin política clara de backup PG | MEDIUM |
| 8 | Migraciones manuales (sin Flyway/Liquibase) | LOW (es decisión consciente) |

---

## Acciones inmediatas recomendadas

1. **Crear repo `tourya-mobile` en GitHub** y subir el código.
2. **Pushear `develop_ftmg` de tourya-front** (10 commits sin respaldo).
3. **Rotar el service account key** y eliminarlo del repo.
4. **Configurar Cloud Build trigger** para `main` de tourya-api.
5. **Verificar backups de Cloud SQL** habilitados con retention 30 días.
6. **Configurar alertas** en Cloud Monitoring (errores 5xx, latencia, DB connections).
7. ~~Migrar secretos a Secret Manager~~. — ✅ Completado en dev (2026-07-08). Falta replicar en prod (`tourya-project-493820`).

---

## Comandos útiles

### Ver revisiones de Cloud Run
```bash
gcloud run revisions list --service=tourya-dev-api --region=us-east1
```

### Forzar deploy de la última imagen
```bash
gcloud run services update tourya-dev-api --no-cpu-throttling --region=us-east1
```

### Ver logs en vivo
```bash
gcloud logging read 'resource.type=cloud_run_revision AND resource.labels.service_name=tourya-dev-api' --limit 50 --order desc
```

### Conectarse a Cloud SQL
```bash
gcloud sql connect tourya-db --user=postgres
```

### Probar SMTP desde Cloud Shell (script de diagnóstico)
Usado para resolver el problema de `@Async` con throttling.
