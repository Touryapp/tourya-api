# 12 — Seguridad y autenticación

Detalle de la seguridad implementada hoy en Tourya: autenticación, autorización, criptografía, validación. Y catálogo de **vulnerabilidades pendientes** documentadas (referencia al plan completo).

> El plan completo de remediación vive **fuera del repo** (interno) en `D:/Users/Usuario/source/repos/tourya/security-remediation-plan.md`. **No commitear ese plan** — expondría la "mapa del tesoro" de las vulnerabilidades.

---

## Autenticación

### JWT (JSON Web Token)

✅ Tourya emite JWT propios al hacer login.

**Implementación**: `JwtService.java`.

**Claims**:
```json
{
  "sub": "usuario@example.com",          ← email
  "fullName": "Juan Pérez",
  "iat": 1717250000,
  "exp": 1717336400,                     ← exp = iat + 86400000 ms (24h)
  "authorities": ["USER", "PROVIDER"]     ← roles
}
```

**Algoritmo**: HMAC-SHA con secret key.

**Property**:
```properties
application.security.jwt.secret-key=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
application.security.jwt.expiration=86400000
```

⚠️ **Vulnerabilidad CRITICAL (C-1)**: el secret está hardcoded en `application.properties`. Cualquiera con acceso al repo puede forjar JWTs válidos.

### JWT — Access + Refresh (implementado)

✅ **Implementado 2026-07-08** (PR #159, BE-12/13/14/15). Estado actual:

| Aspecto | Estado actual (dev) | Objetivo futuro (por rol) |
|---------|---------------------|---------------------------|
| Access token duración | 24 h (todos los roles) | 60/30/15 min según rol (backlog) |
| Refresh token duración | 30 días (todos los roles) | 30d/7d/8h según rol (backlog) |
| Idle timeout | Sin implementar | Sí para PROVIDER/ADMIN (backlog) |

**Reglas transversales implementadas** (PR #159):
- **Rotación**: cada uso de `POST /auth/refresh` revoca el refresh actual y emite uno nuevo con el mismo `family_id`.
- **Detección de reuso**: si un refresh revocado se usa de nuevo → se revoca **toda la familia**. Log WARN con `jti` y `family_id`.
- **Logout server-side**: `POST /auth/logout` revoca la familia entera (invalida otros dispositivos con la misma sesión).
- **Tabla `refresh_token`**: `jti` (unique), `family_id`, `previous_jti`, `expires_at`, `revoked_at`, `revoked_reason`.

**Backwards compatibility en response**: `/auth/authenticate` y `/auth/social-auth` ahora devuelven `token` (legacy, alias de `accessToken`) + `accessToken` + `refreshToken` simultáneamente. Angular y MAUI actuales siguen funcionando sin cambios.

**Pendiente**: duraciones por rol, idle timeout, cookie HttpOnly en web. Ver [05 — Reglas de negocio, RN-005](05-reglas-de-negocio.md).

---

### Login email/password

✅ `POST /api/v1/auth/authenticate`:
- Valida email + password (`AuthenticationManager` + `DaoAuthenticationProvider`).
- BCrypt para hash.
- Responde con `token`/`accessToken` (24h) + `refreshToken` (30d).
- Cliente puede rotar con `POST /auth/refresh` antes de que expire el access token.
- Cerrar sesión: `POST /auth/logout` revoca la familia.

---

### Login social (Google / Facebook vía Firebase)

⚠️ `POST /api/v1/auth/social-auth`:
- Frontend usa Firebase JS SDK.
- Envía: `{ firstname, lastname, email, uuidSocial }`.
- Backend confía en `uuidSocial` (Firebase UID).

⚠️ **Vulnerabilidad CRITICAL**: NO valida el token de Firebase ni el JWT de Google. Cualquiera puede:
```
POST /auth/social-auth
{ "email": "victima@gmail.com", "uuidSocial": "lo-que-sea" }
```
→ Y recibir un JWT válido a nombre de la víctima.

Propuesta de fix completa en `social-login-google-facebook.md`.

---

### Activación de cuenta

✅ Token numérico de 6 dígitos enviado por email. Expira en 15 min.

⚠️ **Vulnerabilidad CRITICAL (C-4)**: el token no se invalida explícitamente después de uso. Replay attack posible.

---

### Sub-usuarios del proveedor

✅ El PROVIDER crea sub-usuarios con contraseña temporal. El sub-usuario tiene `mustChangePassword=true` en su primer login.

✅ Validación de cambio de password (`PATCH /users`):
- `currentPassword` debe coincidir.
- `newPassword == confirmationPassword`.
- Mínimo 8 caracteres.

---

## Autorización (RBAC)

### Mecanismos

Tourya usa **dos mecanismos combinados**:

1. **Path-based en `SecurityConfig`**:
   ```java
   .requestMatchers("/auth/**", "/public/**", "/actuator/**", "/swagger-ui/**").permitAll()
   .anyRequest().authenticated()
   ```

2. **Roles asignados al cargar `UserDetails`** del JWT.

3. **`@PreAuthorize` (parcial)**: algunos endpoints lo usan, otros no. Inconsistencia.

### Roles

| Rol | Descripción |
|-----|-------------|
| `USER` | Turista. Asignado al registrarse. |
| `PROVIDER` | Operador titular. Asignado al aprobar KYB. |
| `PROVIDER_OPERATOR` | Sub-usuario del PROVIDER. Ver [RN-058](05-reglas-de-negocio.md#rn-058) — no ve datos del cliente hasta 1 día antes del tour. |
| `ADMIN` | Admin Tourya (asignación manual en BD). Menú admin completo en todas las vistas. |
| `BACKOFFICE_OPERATION` | Operación Tourya (subset ver [FE-15b/c](17-backlog-implementacion.md)): ver reservas, subir pagos, gestionar reportes DIMAR, gestionar devoluciones de crédito (TC-022 — `POST /admin/credits/{id}/upload-refund-proof` + `GET /admin/credits`). |

### Filtrado por scope

✅ El backend filtra automáticamente:
- PROVIDER ve solo SUS tours/reservas/payouts (filtro por `providerId` del JWT). Además, datos del cliente ofuscados hasta 1 día antes del tour ([RN-058](05-reglas-de-negocio.md#rn-058)).
- PROVIDER_OPERATOR ve solo los tours que tiene asignados (`provider_user_tour`). Mismo scrub que PROVIDER.
- USER ve solo SUS reservas/créditos/wishlist. En TC-022 (RN-062), el turista solicita la devolución de sus propios créditos vía `POST /credits/{id}/request-refund` — guard de ownership en `CreditService.requestRefund`.
- ADMIN y BACKOFFICE_OPERATION ven todo (auditoría/soporte). `Utils.isTouryaBackoffice()` es el helper canónico. Aplica también al listado global de créditos (`GET /admin/credits`) y a la carga del comprobante de devolución (`POST /admin/credits/{id}/upload-refund-proof`) del flujo TC-022 — patrón consistente con `BackofficeGuard` de Angular (que expone ambas rutas en el sidebar admin bajo el item "Créditos").

### Layout admin — sidebar por rol (TC-008 #195, ciclo agosto 2026)

Al desplegar FE-15b/c (`BackofficeGuard` en Angular), aparecieron dos regresiones que llevaron al **TC-008 #195**:
1. El sidebar admin se perdía al navegar entre vistas admin (no persistente en shell).
2. El ADMIN entraba a `/admin/bookings-management` y veía solo los 3 items del subset BACKOFFICE (perdía acceso al resto del panel admin).

**Fix definitivo** (tourya-front PRs #96, #109):
- **Sidebar persistente**: se mueve al shell del `/admin/*` para que las navegaciones internas no lo remonten.
- **Items por rol**: `ADMIN` mantiene TODO el menú admin en todas las vistas admin. `BACKOFFICE_OPERATION` ve solo 3 items (reservas, payouts, DIMAR). Los guards de ruta (`AdminGuard` vs `BackofficeGuard`) siguen aplicando — solo cambia la visibilidad del menú.

**Regla operativa**: cuando se agregue una ruta admin nueva, decidir explícitamente si `BACKOFFICE_OPERATION` puede verla; por default va con `AdminGuard` (solo ADMIN).

---

## Filtro JWT

✅ `JwtFilter.java` (extends `OncePerRequestFilter`):
1. Salta rutas en `/auth` y `/public`.
2. Extrae `Bearer {token}` del header `Authorization`.
3. Valida vía `JwtService.isTokenValid()`.
4. **Recarga `UserDetails` desde la BD en cada request** (para roles frescos).
5. Setea autenticación en `SecurityContextHolder`.

⚠️ Recargar UserDetails en cada request agrega 1 query por request — overhead a considerar.

---

## CORS

✅ Configurado en `BeansConfig.java` (actualizado 2026-07-14):

```java
Allowed origins: [
  // Dev local
  http://localhost:4200,
  http://localhost:8080,
  http://localhost:8100,
  // LB dev (dominio custom + IP directa como fallback)
  https://dev.tourya.co,
  http://34.160.22.16,
  // Cloud Run dev (URLs directas)
  https://tourya-dev-front-640622322458.us-east1.run.app,
  https://tourya-dev-api-640622322458.us-east1.run.app,
  // Prod
  https://tourya.co,
  https://www.tourya.co
]

Allowed methods: [GET, POST, DELETE, PUT, PATCH]
Allowed headers: [Origin, Content-Type, Accept, Authorization]
Allow credentials: true
```

Historial: SEC-11 (Fase 0, 2026-07-08) quitó las IPs AWS legacy y las URLs viejas de Cloud Run. La entrada `https://dev.tourya.co` se agregó cuando se aprovisionó HTTPS en el LB dev (2026-07-14) — sin esa entrada el backend rechazaba con 403 `Invalid CORS request`.

---

## Criptografía y secretos

### Password hashing
✅ BCrypt (`BCryptPasswordEncoder`), cost factor default = 10.

### JWT signing
✅ HMAC-SHA con secret key. ⚠️ Secret hardcoded.

### Almacenamiento de secretos
⚠️ **Hoy todos los secretos están como properties hardcoded o env vars en Cloud Run**. Debería usarse **GCP Secret Manager** para los críticos:
- JWT secret
- Wompi integrity secret
- SMTP password
- DB password
- GCS service account key

### GitHub Secrets vs GCP Secret Manager (aclaración)

Son cosas distintas que viven en momentos distintos del ciclo:

| Aspecto | GitHub Secrets | GCP Secret Manager |
|---------|----------------|--------------------|
| **Momento de uso** | Build / CI/CD (GitHub Actions) | Runtime (app corriendo en Cloud Run) |
| **Ejemplos típicos** | `GCP_SA_KEY` para desplegar, `DOCKER_TOKEN` para push a registry | `JWT_SECRET`, `WOMPI_INTEGRITY_SECRET`, `DB_PASSWORD` |
| **Quién los lee** | Runners de GitHub Actions | La app Spring Boot en cada request |
| **Cuándo se acceden** | Segundos-minutos del pipeline | Todo el tiempo mientras la app está viva |
| **Frecuencia de rotación** | Cuando cambian credenciales de deploy | Cuando se comprometen o por política |

**Analogía**: GitHub Secrets = llaves del camión de mudanza que trae los muebles. Secret Manager = llaves de la casa cuando ya vives en ella.

**En Tourya**:
- **GitHub Secrets** — usados actualmente para deploys legacy y `DB_HOST`/`DB_PORT`/`DB_USER`. El anterior `GCP_SA_KEY_DEV` fue **eliminado** al migrar a Workload Identity Federation.
- **GCP Secret Manager** — ✅ **Habilitado y en uso** en `tourya-project-dev` desde 2026-07-08 (Fase 0, bloque 0.3). Secretos activos: `tourya-jwt-key`, `tourya-wompi-integrity-secret`, `tourya-db-password`, `tourya-smtp-password`.
- **Workload Identity Federation** — ✅ **Habilitado** en `tourya-project-dev`. GitHub Actions se autentica vía OIDC contra el pool `github-actions-pool` / provider `github-oidc` con condición `repository == Touryapp/tourya-api`. Sin necesidad de service account keys.

---

## Validación de input

✅ Tourya valida en varios niveles:

| Nivel | Mecanismo | Ejemplo |
|-------|-----------|---------|
| DTO de entrada | Jakarta Validation (`@NotBlank`, `@Email`, `@Size`) | `RegistrationRequest` |
| Service | Excepciones de negocio | `InsufficientCapacityException` |
| BD | Constraints SQL | CHECK constraints sobre JSONB español |

⚠️ Faltan validaciones en algunos campos (ej. patrones de RNT, NIT colombiano).

---

## Vulnerabilidades documentadas (resumen)

### CRITICAL — Estado

| # | Vulnerabilidad | Estado | PR / Referencia |
|---|----------------|:------:|-----------------|
| C-1 | JWT secret hardcoded en `application.properties` | ✅ **Resuelto** en dev | PRs #151, #152 (Secret Manager) + rotación de clave |
| C-2 | Wompi integrity secret hardcoded | ✅ **Resuelto** en dev | PRs #151, #152 (movido a Secret Manager) |
| C-3 | Spring Actuator `*` (todos endpoints expuestos) | ✅ **Resuelto** | PR #146 (SEC-03) |
| C-4 | Activation token sin protección de replay | ✅ **Resuelto** | PR #147 (SEC-04) |
| C-5 | Endpoint público `/public/bookings/{id}` leakea PII (datos pagador) | ✅ **Resuelto** | PR #148 (SEC-05) |
| C-6 | Social login sin validar token Firebase | ⚠️ Pendiente | Ver `social-login-google-facebook.md` |

### HIGH — Estado

| # | Vulnerabilidad | Estado | Notas |
|---|----------------|:------:|-------|
| H-1 | Service account key `df67...` con expiración infinita + expuesto en filesystem local y GitHub Secrets | ✅ **Resuelto en dev** | Key deshabilitado en IAM; migración a Workload Identity Federation completada (2026-07-08). Nunca estuvo commiteado al repo, `.gitignore` lo protegió |
| H-2 | `System.out.println("tempPassword: ...")` leakea passwords temporales a logs | ✅ **Resuelto** | PR #149 (SEC-08) |
| H-3 | Sin rate limiting en endpoints de auth | ✅ **Resuelto** en dev con flag OFF | PR #164 (SEC-09). `AuthRateLimitFilter` aplica a `/auth/**` con contador in-memory por IP. Umbrales configurables en `app_config` (`AUTH_RATE_LIMIT_ENABLED`, `AUTH_RATE_LIMIT_PER_MINUTE`). Se activa sin re-deploy cuando Luis diga. Limitación conocida: contador no coordinado entre réplicas de Cloud Run — límite efectivo hasta 2× con max-instances=2 |
| H-4 | Sin lockout tras N intentos fallidos de login | ✅ **Resuelto** en dev con flag OFF | PR #165 (SEC-10). Contador `_user.failed_login_attempts` + timestamp `_user.locked_until`. Backoff exponencial (60s → 120s → 240s → ... cap 24h) al superar `AUTH_LOCKOUT_MAX_ATTEMPTS` (default 5). Silencioso si el email no existe (evita filtrar cuentas). Auto-unlock via `isAccountNonLocked()` cuando pasa el timestamp — sin cron. Se activa sin re-deploy con `PUT /config/AUTH_LOCKOUT_ENABLED` |
| H-5 | Sin protección CSRF (compensado por JWT en header pero…) | ⚠️ Pendiente | — |
| H-6 | Sin webhook Wompi (pagos huérfanos posibles) | ✅ **Resuelto** en dev | PRs #156 + #157 (webhook + verificación firma) + PR #158 (job de reconciliación). Detecta pagos huérfanos y los loguea como WARN para investigación manual |
| H-7 | CORS permisivo con URLs muertas AWS legacy | ✅ **Resuelto** | PR #150 (SEC-11) |

### MEDIUM

| # | Item |
|---|------|
| M-1 | jjwt 0.11.5 (debería 0.12.x para fix de issues conocidos) |
| M-2 | Sin logs estructurados (JSON) — más difícil debug producción |
| M-3 | Swagger UI accesible sin auth en producción |
| M-4 | Sin retención clara de logs (¿qué tiempo se guardan?) |

> 📋 **El detalle completo, con steps de remediación**, vive en `security-remediation-plan.md` (interno, NO commitear).

---

## Auditoría

✅ Las entidades con `BaseEntity` registran:
- `createdDate`, `lastModifiedDate` (cuándo).
- `createdBy`, `lastModifiedBy` (quién — ID del usuario JWT).

⚠️ No hay **audit log explícito** para acciones críticas (login, cancelación, aprobación KYB, asignación de %). Útil para forensia.

📌 PENDIENTE LUIS — ¿se requiere log de auditoría para compliance?

---

## Almacenamiento de datos sensibles

| Dato | Donde se guarda | ¿Encriptado? |
|------|-----------------|---------------|
| Password | `_user.password` (BCrypt) | ✅ hash |
| Email | `_user.email` | ❌ plano (es razonable) |
| Datos del pagador (Wompi) | `payment.payer_name/email/phone/document` | ❌ plano |
| Tarjetas | NO se guardan | ✅ (Wompi tokeniza) |
| Documentos KYB | GCS / S3 (URL pública o signed) | depende del bucket policy |
| JWT en cliente | localStorage (web), MAUI SecureStorage (mobile) | depende del medio |

⚠️ El JWT en `localStorage` es vulnerable a XSS. En mobile `SecureStorage` está OK (encriptado por OS).

---

## Recomendaciones inmediatas

1. **Rotar y mover a Secret Manager** los secretos hardcoded (JWT, Wompi, GCS key).
2. **Eliminar `tourya-dev-sa-key.json` del repo** y agregar a `.gitignore`.
3. **Quitar el `System.out.println` del password temporal** (`AuthenticationService.java` línea 163).
4. **Cerrar Actuator exposure** — solo health/info.
5. **Implementar validación real del token Firebase** (o migrar a Token Exchange).
6. **Limpiar endpoints públicos** que filtran PII.
7. **Agregar rate limiting** a `/auth/authenticate`.

> Ver `security-remediation-plan.md` (interno) para el detalle paso a paso.

---

## Decisiones de seguridad

### Decisión: JWT híbrido stateless + refresh state en BD (actualizada 2026-07-08)

✅ **Actualizado en PR #159**: access token stateless (JWT firmado, exp 24h) + refresh token con estado en tabla `refresh_token`.

- **Access token**: JWT autocontenido, se valida sin BD hit → mismo rendimiento que antes.
- **Refresh token**: JWT firmado + registro en BD. La BD es fuente autoritativa del estado (revocado o no) → permite logout server-side + detección de reuso.
- **Trade-off resuelto**: UX ya no obliga a re-loguear diario (refresh rota tokens sin fricción). Access token puede revocarse indirectamente vía revocación de familia + expiración natural.

**Duración por rol** aún pendiente (backlog): hoy es 24h/30d para todos.

### Decisión: Roles cargados desde BD en cada request
✅ El `JwtFilter` recarga `UserDetails`.
> **Trade-off**: +1 query por request, pero los roles siempre están frescos (si se asigna PROVIDER mientras el usuario tiene sesión activa, lo ve sin re-login).

### Decisión: BCrypt sobre SHA-256 / Argon2
✅ Default de Spring Security, adecuado.

### Decisión: CORS permite credentials con orígenes específicos
✅ Adecuado, pero la lista debe actualizarse (quitar AWS, agregar `tourya.co`).
