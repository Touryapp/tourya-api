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

### JWT (rediseño pendiente) — Access + Refresh diferenciados por rol

✅ Definido Franklin (2026-07-07) basado en OWASP ASVS Level 2:

| Rol | Access token | Refresh token | Idle |
|-----|--------------|---------------|------|
| **USER (turista)** | 60 min | 30 días | Sin idle |
| **PROVIDER / PROVIDER_OPERATOR** | 30 min | 7 días | 4 horas |
| **ADMIN / BACKOFFICE_OPERATION** | 15 min | 8 horas | 15 min |

**Reglas transversales**: refresh rotativo, detección de reuso (revoca familia entera), almacenamiento en cookie HttpOnly (web) o SecureStorage (mobile), nueva tabla `refresh_token`.

Detalle completo en [05 — Reglas de negocio, RN-005](05-reglas-de-negocio.md).

---

### Login email/password

✅ `POST /api/v1/auth/authenticate`:
- Valida email + password (`AuthenticationManager` + `DaoAuthenticationProvider`).
- BCrypt para hash.
- Responde con JWT de 24h.

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
| `PROVIDER_OPERATOR` | Sub-usuario del PROVIDER. |
| `ADMIN` | Admin Tourya (asignación manual en BD). |
| `BACKOFFICE_OPERATION` | Operación Tourya (precios, payouts). |

### Filtrado por scope

✅ El backend filtra automáticamente:
- PROVIDER ve solo SUS tours/reservas/payouts (filtro por `providerId` del JWT).
- PROVIDER_OPERATOR ve solo los tours que tiene asignados (`provider_user_tour`).
- USER ve solo SUS reservas/créditos/wishlist.

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

✅ Configurado en `BeansConfig.java`:

```java
Allowed origins: [
  http://localhost:4200,
  http://localhost:8080,
  http://localhost:8100,
  https://localhost/,
  http://44.203.38.85:8088,    ⚠️ AWS legacy
  http://44.203.38.85:8080,    ⚠️ AWS legacy
  https://tourya-dev-front-5j2nd2oflq-ue.a.run.app,   ⚠️ URL vieja (ya no aplica)
  https://tourya-dev-api-5j2nd2oflq-ue.a.run.app     ⚠️ URL vieja (ya no aplica)
]

Allowed methods: [GET, POST, DELETE, PUT, PATCH]
Allowed headers: [Origin, Content-Type, Accept, Authorization]
```

📌 **A actualizar en Fase 0** (SEC-11): agregar `https://tourya.co`, `https://tourya-dev-front-640622322458.us-east1.run.app`, `https://tourya-dev-api-640622322458.us-east1.run.app`. Quitar IPs AWS y URLs viejas de Cloud Run.

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
- **GitHub Secrets** — ya se usan (para el deploy continuo a AWS EC2 legacy). Nombres exactos: consultar en `github.com/Touryapp/tourya-api/settings/secrets/actions`.
- **GCP Secret Manager** — ⚠️ **NO habilitado** en `tourya-project-dev` al 2026-07-08. Pendiente en Fase 0.

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

### CRITICAL — Deben fixearse pronto

| # | Vulnerabilidad | Archivo |
|---|----------------|---------|
| C-1 | JWT secret hardcoded en `application.properties` | `application.properties` línea 76 |
| C-2 | Wompi integrity secret hardcoded | `application.properties` línea 14 |
| C-3 | Spring Actuator `*` (todos endpoints expuestos) | `application.properties` línea 18 |
| C-4 | Activation token sin protección de replay | `AuthenticationService.java` |
| C-5 | Endpoint público `/public/bookings/{id}` leakea PII (datos pagador) | `PublicController` |
| C-6 | Social login sin validar token Firebase | `AuthenticationService.authenticateWithSocial` |

### HIGH — Importantes pero menos urgentes

| # | Vulnerabilidad |
|---|----------------|
| H-1 | `tourya-dev-sa-key.json` (GCP service account) commiteado en repo |
| H-2 | `System.out.println("tempPassword: ...")` leakea passwords temporales a logs |
| H-3 | Sin rate limiting en endpoints de auth |
| H-4 | Sin lockout tras N intentos fallidos de login |
| H-5 | Sin protección CSRF (compensado por JWT en header pero…) |
| H-6 | Sin webhook Wompi (pagos huérfanos posibles) |
| H-7 | CORS demasiado permisivo (`*` en algunos casos) |

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

### Decisión: JWT stateless, sin refresh token
✅ Sesión completa en el JWT, expira 24h, usuario debe volver a loguearse.
> **Trade-off**: simple pero UX peor. Sin posibilidad de revocar un token específico antes de exp.

### Decisión: Roles cargados desde BD en cada request
✅ El `JwtFilter` recarga `UserDetails`.
> **Trade-off**: +1 query por request, pero los roles siempre están frescos (si se asigna PROVIDER mientras el usuario tiene sesión activa, lo ve sin re-login).

### Decisión: BCrypt sobre SHA-256 / Argon2
✅ Default de Spring Security, adecuado.

### Decisión: CORS permite credentials con orígenes específicos
✅ Adecuado, pero la lista debe actualizarse (quitar AWS, agregar `tourya.co`).
