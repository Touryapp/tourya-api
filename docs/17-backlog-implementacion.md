# 17 — Backlog de implementación y plan de ejecución

Consolida todo el trabajo pendiente que emergió de los documentos [00–16](00-README.md) en un **backlog priorizado** con estimación de esfuerzo, dependencias y **plan por fases**.

> **Objetivo**: llevar Tourya de "documentación completa" a "MVP productivo" con un roadmap ejecutable.

> ⚠️ **Modelo de ejecución (importante)**: el desarrollo se hará con un **agente IA (Claude) como implementador**, con Franklin como revisor y product owner técnico, y Luis como PO de negocio. Las estimaciones de esfuerzo aquí reflejan **tiempo real de agente + review + validación**, NO tiempo de un desarrollador humano trabajando solo.
>
> Por qué esto importa:
> - Escribir código: **agente es 10-20× más rápido** que un dev humano.
> - Review humano: **no se acelera** — sigue siendo cuello de botella.
> - Aprobaciones externas (Twilio WABA, Meta plantillas, Play Store): **tiempo calendario NO comprimible**.
> - Testing con usuarios reales, coordinación con operadores: **tiempo calendario NO comprimible**.
> - Deployment + rollback en producción: **cuidadoso, no se acelera**.

---

## Convenciones

### Prioridad

| Nivel | Significado |
|-------|-------------|
| **P0** | Bloqueante de producción. No podemos salir en productivo sin esto. |
| **P1** | Necesario para el MVP funcional. |
| **P2** | Post-MVP importante — 3-6 meses después de salir. |
| **P3** | Nice-to-have / futuro / roadmap 12m+. |

### Esfuerzo estimado (agente IA + review humano)

| Talla | Trabajo agente | Review + validación | Total calendario típico |
|-------|:---------------:|:-------------------:|:-----------------------:|
| **XS** | 15–30 min | 15–30 min | ~1 hora |
| **S** | 30 min – 2 h | 30 min – 1 h | ~½ día |
| **M** | 2–4 h | 1–2 h | ~1 día |
| **L** | 4–8 h (1 día agente) | 2–4 h | ~2 días |
| **XL** | 1–3 días agente | 4–8 h | ~3–5 días |

> **Nota**: estas estimaciones asumen el patrón "agente propone → Franklin revisa PR → merge → deploy → validar". No incluyen tiempo de aprobación externa (Twilio, Play Store, etc.), que se contabiliza en "dependencias" de cada fase.

### Estado

| Marca | Significado |
|-------|-------------|
| ⚪ | No iniciado |
| 🟡 | En progreso |
| 🟢 | Terminado |
| 🔒 | Bloqueado por dependencia |

---

## Backlog por área

### A. Seguridad (crítico — antes de producción)

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| SEC-01 | 🟢 Rotar JWT secret y mover a **GCP Secret Manager** | P0 | S | — | C-1, PRs #151+#152 (dev) |
| SEC-02 | 🟢 Rotar Wompi integrity secret y mover a Secret Manager | P0 | S | — | C-2, PRs #151+#152 (dev, movido — no rotado) |
| SEC-03 | 🟢 Cerrar exposición de Actuator (solo `/health`, `/info`) | P0 | XS | — | C-3, PR #146 |
| SEC-04 | 🟢 Fix bug de replay en token de activación (invalidar tras uso) | P0 | XS | — | C-4, RN-003, PR #147 |
| SEC-05 | 🟢 Cerrar filtración de PII en `/public/bookings/{id}` | P0 | S | — | C-5, PR #148 |
| SEC-06 | ⚪ Reemplazar Firebase social login por **Token Exchange** (Google + Facebook) | P0 | M | — | C-6, RN-006, [social-login-google-facebook.md](../social-login-google-facebook.md) |
| SEC-07 | 🟢 Migrar a Workload Identity Federation (elimina SA keys JSON) | P0 | XS→M* | — | H-1, PR #153 |
| SEC-08 | 🟢 Eliminar `System.out.println` de password temporal | P0 | XS | — | H-2, PR #149 |
| SEC-09 | 🟢 Rate limiting en todos los endpoints `/auth/**` con feature flag OFF por default. In-memory por IP, umbral configurable via `app_config` | P1 | S | — | H-3, PR #164. Activación: `PUT /config/AUTH_RATE_LIMIT_ENABLED {"value":{"value":1}}` |
| SEC-10 | 🟢 Lockout tras N intentos fallidos con backoff exponencial, feature flag OFF por default. Complementa SEC-09 (bloqueo por cuenta cuando el atacante rota IPs). Auto-unlock al pasar `locked_until` sin cron | P1 | S | SEC-09 | H-4, PR #165. Activación: `PUT /config/AUTH_LOCKOUT_ENABLED {"value":{"value":1}}` |
| SEC-11 | 🟢 Limpiar CORS: quitar IPs AWS legacy, agregar `tourya.co` | P1 | XS | — | H-7, PR #150 |

*SEC-07 escaló a M porque en vez de solo rotar el key se migró a WIF (mejor solución de largo plazo).

**Subtotal seguridad**: ~1.5 días de agente + revisiones = **~3 días calendario**.

---

### B. Backend — cambios de modelo y reglas aprobadas por Luis

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| BE-01 | 🟢 Backfill `tour.porcentaje_tourya = 0.15` (38 tours en dev) + `ALTER COLUMN SET DEFAULT 0.15` + limpiar comment "DEPRECATED". Campo YA existía desde 043 pero marcado como deprecated en 050; se reactivó | P1 | S | — | RN-015, PR #163 (migración 071). Nombre queda en español (`porcentaje_tourya`) — rename a inglés descartado por alto costo/bajo valor |
| BE-02 | 🟢 `TourScheduleConfigGeneralService.buildSlots()` y `manageSlotsUpdate()` heredan `tour.porcentaje_tourya` al crear slot nuevo. Antes se hardcodeaba a `ZERO` | P1 | S | BE-01 | RN-015, PR #163 |
| BE-03 | Migración: eliminar `isUnlimitedCapacity` de `tour_schedule` (drop column) | P1 | XS | — | RN-021 |
| BE-04 | Ajustar `TourScheduleConfigGeneralService` para no leer/escribir el campo eliminado | P1 | S | BE-03 | RN-021 |
| BE-05 | 🟢 Agregar razones `LEGAL_OBLIGATIONS`, `CHANGE_OF_PLANS` al enum `CancellationReasonEnum` | P1 | XS | — | RN-030, PR #166. Sin migración SQL (columna `reservation.cancellation_reason` es VARCHAR(20) y ambos valores caben) |
| BE-06 | 🟢 Refactor `holdMinutes` (carrito) a `app_config` — leer de BD, no de property | P1 | S | — | RN-022, PR #160 |
| BE-07 | 🟢 Refactor buffer de payout (2 días) a `app_config` | P1 | S | — | RN-040, PR #160 |
| BE-08 | 🟢 Refactor expiración de créditos a `app_config` (6 meses en código real, no 1 año) | P1 | S | — | RN-036, PR #160 |
| BE-09 | 🟢 Endpoint GET/PUT `/config/{key}` para ADMIN gestione `app_config` desde backoffice | P1 | S | BE-06, BE-07, BE-08 | RN-022, PR #160. GET ya existía; se agregó PUT con `@PreAuthorize("hasRole('ADMIN')")` |
| BE-10 | 🟢 Validador de galería aplicado en `POST /tours/{id}/gallery/sync` y `/syncWithUpdate` (7 imgs / 5 MB / landscape / 800px min) | P1 | S | — | RN-013, PR #161. Se descartó "ancho recomendado 1920px" (era solo recomendación, no obligación). Umbrales configurables vía `app_config` |
| BE-11 | 🟢 Validación RN-045 en `PUT /requestProvider/user/send` con feature flag `KYB_REQUIRE_MANDATORY_DOCS` (default OFF). Cuando ON, rechaza el submit con 400 + lista de docs faltantes | P1 | S | — | RN-045, PR #162. Flag default 0 para no bloquear QA in-flight; se activa con PUT `/config/KYB_REQUIRE_MANDATORY_DOCS` cuando Luis diga |
| BE-12 | 🟢 Migración: crear tabla `refresh_token` con `jti`, `family_id`, `previous_jti`, `expires_at`, `revoked_at` | P1 | S | — | RN-005, PR #159 (migración 067) |
| BE-13 | 🟢 Refactor `JwtService` → emitir `access_token` + `refresh_token` con expiraciones configurables | P1 | M | BE-12 | RN-005, PR #159. Duración por rol pospuesta (default: access 24h, refresh 30d) |
| BE-14 | 🟢 Endpoint `POST /auth/refresh` con rotación + detección de reuso (revoca familia) | P1 | S | BE-13 | RN-005, PR #159 |
| BE-15 | 🟢 Endpoint `POST /auth/logout` que revoca la familia entera de tokens | P1 | XS | BE-14 | RN-005, PR #159 |
| BE-16 | 🟢 Webhook server-side Wompi: endpoint `POST /public/wompi/webhook` con verificación de firma | P1 | M | — | RN-025, PR #157 (código) + #156 (workflow) |
| BE-17 | 🟢 Reconciliación de eventos Wompi (job) — matchea contra Payments y detecta huérfanos | P1 | S | BE-16 | RN-025, PR #158. Nota: en este PR solo alerta el huérfano; el matcheo automático a reservas TEMPORAL requiere agregar `wompi_reference` a `shopping_cart` (backlog futuro) |
| BE-18 | Correo automático "crédito por expirar" (30 días y 7 días antes) | P2 | S | — | RN-036 |
| BE-19 | Correo automático "crédito expirado" | P2 | XS | BE-18 | RN-036 |
| BE-20 | Verificar y completar flujo de reagendamiento (3 casos: igual/menor/mayor precio) | P1 | S | — | RN-033 |

**Subtotal backend**: ~3 días de agente + revisiones + testing en staging = **~5–7 días calendario**.

---

### C. Frontend Web (Angular)

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| FE-01 | Adaptar login: guardar `refresh_token` en cookie HttpOnly + auto-refresh transparente en interceptor | P1 | S | BE-13 | RN-005 |
| FE-02 | Migrar login social: quitar Firebase SDK, integrar `@abacritt/angularx-social-login` u equivalente compatible Angular 19 | P0 | M | SEC-06 | — |
| FE-03 | 🟢 Validación pre-upload en el uploader de galería (formato/tamaño/orientación/ancho mínimo/cuenta total), umbrales leídos de `app_config` | P1 | S | RN-013 revisada, BE-10 | RN-013, PR tourya-front #59. Backend sigue como defensa en profundidad; frontend evita el round-trip y muestra errores del backend en el mismo Swal |
| FE-04 | 🟢 UI para ADMIN de `app_config` (holdMinutes, buffer payout, expiración créditos, gallery, KYB flag, rate limit y lockout) | P1 | S | BE-09 | RN-022, PR tourya-front #58. 12 configs en 5 grupos (Reservas, Payouts, Galería, KYB, Auth). `CANCELLATION_POLICY` queda fuera del v1 (requiere editor JSON dedicado) |
| FE-05 | UI backoffice: gestionar `Tour.percentageTourya` en formulario de aprobación de tour | P1 | S | BE-01 | RN-015 |
| FE-06 | 🟢 Agregar UI para las 2 nuevas razones de cancelación en cliente (Angular) | P1 | XS | BE-05 | RN-030, PR tourya-front #56. Aprovechó para cerrar bug preexistente: `INABILITY_TO_TRAVEL` faltaba en `formatCancellationReason` |
| FE-07 | Quitar campo `isUnlimitedCapacity` en config de schedule (mover a formulario de tour) | P1 | XS | BE-03 | RN-021 |
| FE-08 | Dashboard financiero del backoffice: GMV, net revenue, cuentas por pagar, CAC | P2 | L | — | Doc 01 roadmap |
| FE-09 | Dashboard operativo del backoffice: tasa conversión/cancelación, tours de alto riesgo | P2 | L | — | Doc 01 roadmap |
| FE-10 | Gestor de disputas | P2 | L | — | Doc 01 roadmap |

**Subtotal frontend web**: ~3 días de agente + revisiones + prueba visual = **~5–7 días calendario** (los dashboards `FE-08/09/10` no cuentan aquí — van a Fase 4).

---

### D. Mobile (MAUI Android)

Basado en el roadmap de [15 — MVP mobile estado](15-mvp-mobile-estado.md).

#### D.0 — Higiene (bloqueante)

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-00 | **Crear repo GitHub `tourya-mobile`** y subir el código (hoy solo local — riesgo alto) | P0 | XS | — | [15](15-mvp-mobile-estado.md) |
| MO-01 | Actualizar URL del backend en `Constants.cs` (hoy apunta a IP AWS legacy `44.203.38.85`) | P0 | XS | — | [15](15-mvp-mobile-estado.md) |
| MO-02 | Registrar Syncfusion License en `MauiProgram.cs` | P0 | XS | — | [15](15-mvp-mobile-estado.md) |
| MO-03 | CI/CD: GitHub Actions → build APK firmado → Play Store internal track | P1 | M | MO-00, MO-02 | [15](15-mvp-mobile-estado.md) |

#### D.1 — Brechas del turista

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-10 | UI de crear reseña con cámara (existe `ReviewService`, falta UI) | P1 | S | — | Doc 15 Ciclo 1 |
| MO-11 | Wishlist page (lista de deseos) en tab del turista | P1 | S | — | Doc 15 Ciclo 1 |
| MO-12 | UI dedicada de créditos: saldo, historial, transferir a otro turista | P1 | M | — | Doc 15 Ciclo 1 |
| MO-13 | Integrar login social sin Firebase (Google + Facebook nativos MAUI) | P1 | M | SEC-06 | Doc 15 |

#### D.2 — Brechas del proveedor

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-20 | Responder reseñas desde `ProviderReviewsPage` | P1 | S | — | Doc 15 Ciclo 2 |
| MO-21 | `PayoutsPage`: ver payouts + descargar comprobantes | P1 | M | — | Doc 15 Ciclo 2 |
| MO-22 | Crear / editar / eliminar operarios (`PROVIDER_OPERATOR`) | P1 | M | — | Doc 14 alcance Luis |
| MO-23 | Compartir contraseña temporal por WhatsApp / SMS (deep-link) | P1 | S | MO-22 | Doc 14 UX |
| MO-24 | Resetear contraseña de operarios desde app | P1 | S | MO-22 | Doc 14 |

#### D.3 — Refinar UX del proveedor (calidad crítica para adopción en San Andrés)

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-30 | Autosave / borradores en el wizard de creación de tour | P1 | M | — | Doc 14 UX |
| MO-31 | Compresión de imágenes en cliente antes de subir | P1 | S | — | Doc 14 UX |
| MO-32 | Upload resumable / en background con progreso | P1 | M | MO-31 | Doc 14 UX |
| MO-33 | Vista de calendario mejorada en `ScheduleCalendarPage` (día/semana/mes) | P2 | M | — | Doc 14 UX |
| MO-34 | "Copiar precios de otro tour" al configurar schedule | P2 | S | — | Doc 14 UX |
| MO-35 | Dictado de voz opcional en descripciones largas | P3 | S | — | Doc 14 UX |

#### D.4 — Features "solo mobile" (valor incremental)

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-40 | Firebase Cloud Messaging (push notifications) — turista, proveedor, operario | P1 | M | — | Doc 14 |
| MO-41 | Geolocalización: "tours cerca de mí" en `ExplorePage` | P2 | S | — | Doc 14 |
| MO-42 | Geolocalización: "cómo llegar al punto de encuentro" en `TourDetailPage` (Syncfusion.Maps ya importado) | P2 | S | — | Doc 14 |
| MO-43 | Deep-linking: compartir tour por WhatsApp con link universal | P2 | S | — | Doc 14 |
| MO-44 | Wallet integration: Apple Pay / Google Pay vía Wompi | P3 | M | — | Doc 14 |
| MO-45 | Widget "próxima reserva" (Android app widget) | P3 | M | — | Doc 14 |

#### D.5 — Robustecer rol operario

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-50 | Modo offline: cache local de reservas del día para operario | P2 | M | — | Doc 15 Ciclo 5 |
| MO-51 | Vista de reservas del operario mejorada (filtro por tour asignado, agrupación por hora) | P2 | S | — | Doc 15 Ciclo 5 |

**Subtotal mobile**: ~5–6 días de agente + revisiones + testing en dispositivo real + release cycles Play Store = **~2 semanas calendario** para el bloque D.1 + D.2 + D.3. Los D.4 y D.5 van a fases posteriores.

---

### E. Infraestructura y observabilidad

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| INF-01 | 🟢 Configurar 8 alertas mínimas en Cloud Monitoring (5xx backend/frontend, latencia P95, DB conn/CPU/disco, cost anomaly, Cloud Run throttled) | P0 | S | — | 7/8 completadas en dev (2026-07-08). Falta cost anomaly (billing budget). Ver [13](13-despliegue-cicd.md) |
| INF-02 | 🟢 Habilitar backup automático de Cloud SQL (retention 30 días) | P0 | XS | — | Completado en dev (2026-07-08). PITR activo. Ver [13](13-despliegue-cicd.md) |
| INF-03 | Crear Cloud Build trigger para `main` (deploy productivo automatizado) | P1 | S | — | [13](13-despliegue-cicd.md) |
| INF-04 | Coverage gate 80% con JaCoCo | P2 | S | — | `cicd-improvement-plan.md` |
| INF-05 | SpotBugs + PMD en CI | P2 | S | INF-04 | `cicd-improvement-plan.md` |
| INF-06 | Snyk Free en CI (solo push a main/develop) | P2 | S | — | `cicd-improvement-plan.md` |
| INF-07 | Branch Protection en `develop` y `main` (requiere PR + CI verde) | P1 | XS | INF-04, INF-06 | `cicd-improvement-plan.md` |
| INF-08 | 🟢 Migrar `.env` completo a env vars documentadas + Secret Manager | P0 | S | SEC-01, SEC-02 | PRs #151+#152 (dev) |
| INF-09 | Limpieza de ramas viejas en GitHub (`Touryapp/tourya-api` tiene ~80 ramas) | P3 | XS | — | — |

**Subtotal infra**: ~1 día de agente + configuración manual en GCP Console (que Franklin hace) = **~2 días calendario**.

---

### F. Agentes IA

Ver diseño completo en [16](16-agentes-ia.md).

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| IA-00 | Migración: crear tabla `agent_audit_log` (prompt, modelo, tokens, costo, decisión, entidad afectada) | P1 | S | — | [16](16-agentes-ia.md) principio #4 |
| IA-01 | Paquete `com.tourya.api.agents.shared`: `ILlmClient`, `AnthropicClient`, `PromptTemplate`, `BudgetGuard`, `AgentAuditWriter` | P1 | M | IA-00 | [16](16-agentes-ia.md) arq |
| IA-02 | Agente 1: **Travel Concierge** (búsqueda + carrito + FAQ) | P1 | M | IA-01 | [16](16-agentes-ia.md) |
| IA-03 | Cuenta Twilio + WABA aprobado + plantillas de mensaje | P1 | M | — | [11](11-integraciones.md) |
| IA-04 | Servicio `TwilioMessagingService` (envío + webhook inbound) | P1 | M | IA-03 | [11](11-integraciones.md) |
| IA-05 | Agente 2: **Support 24/7** (WhatsApp + cancelación/reagendamiento) | P1 | L | IA-04, IA-01 | [16](16-agentes-ia.md) |
| IA-06 | Agente 3: **Desert Shopping Cart** (recuperación carrito) | P2 | S | IA-04 (o fallback email) | [16](16-agentes-ia.md) |
| IA-07 | Agente 4: **Operator Support** (asistir wizard tour + borrador reseña) | P1 | M | IA-01 | [16](16-agentes-ia.md) |
| IA-08 | Agente 5: **Backoffice Support** (pre-verificación KYB + tour + manifiesto DIMAR) | P2 | M | IA-01 | [16](16-agentes-ia.md) |
| IA-09 | Traducción automática es → en/pt-BR con Google Cloud Translation (integrar con `TourService.saveAll`) | P2 | S | — | [11](11-integraciones.md), `traduccion-automatica-tours.md` |
| IA-10 | Agente 6: **Moderación de reseñas** (Haiku 4.5) | P2 | M | IA-01 | RN-050 |
| IA-11 | Dashboard de observabilidad de agentes (uso, latencia, override rate, costo) | P2 | S | IA-01 | [16](16-agentes-ia.md) |

**Subtotal agentes**: ~3–4 días de agente + iteración de prompts con datos reales + espera de aprobación Twilio WABA = **~2–3 semanas calendario** (el tiempo de Twilio WABA es el más largo y NO se puede acelerar).

---

## Plan por fases (con estimaciones agente + calendario real)

### 🚨 Fase 0 — Higiene y seguridad crítica

**Objetivo**: **cerrar el techo antes de invitar gente a la fiesta**. Sin esto no salimos a producción.

| Ítem | Talla | Trabajo | Nota |
|------|:-----:|---------|------|
| MO-00 Repo GitHub mobile | XS | Franklin | Solo crear repo + push. Necesita acceso a `Touryapp/` |
| MO-01 URL backend mobile | XS | Agente | Cambio en `Constants.cs` |
| MO-02 Syncfusion license | XS | Franklin | Manual — pegar license key |
| 🟢 SEC-01 JWT secret a Secret Manager | S | Agente + Franklin | Rotado y movido (PRs #151+#152) |
| 🟢 SEC-02 Wompi secret a Secret Manager | S | Agente + Franklin | Movido (PRs #151+#152) |
| 🟢 SEC-03 Cerrar Actuator | XS | Agente | PR #146 |
| 🟢 SEC-04 Fix replay token activación | XS | Agente | PR #147 |
| 🟢 SEC-05 PII leak `/public/bookings` | S | Agente | PR #148 |
| 🟢 SEC-07 Migrar a WIF | XS→M | Franklin + Agente | Pool + provider + binding + workflow OIDC (PR #153) |
| 🟢 SEC-08 Eliminar `println` de password | XS | Agente | PR #149 |
| 🟢 INF-01 Alertas Cloud Monitoring | S | Agente + Franklin | 7/8 alertas activas en dev via REST API (2026-07-08). Cost anomaly pendiente |
| 🟢 INF-02 Backup Cloud SQL | XS | Agente | Diarios 03:00, retention 30 días, PITR activo (2026-07-08) |
| 🟢 INF-08 Env vars + Secret Manager completo | S | Agente + Franklin | Idem SEC-01/02 |

**Estimación real**:
- **Agente**: ~4–6 horas de trabajo real.
- **Franklin**: ~4–6 horas (rotar secretos GCP, crear config Cloud Monitoring, revisar PRs).
- **Total calendario**: **3–5 días** (asumiendo que Franklin puede darle 1-2 horas por día).

**Resultado**: producción segura, código respaldado, observabilidad mínima.

---

### 🏗️ Fase 1 — MVP core

**Objetivo**: implementar los cambios de modelo aprobados por Luis y refactors habilitantes.

**Bloques de trabajo** (se pueden hacer secuencialmente o en paralelo si Franklin aprueba varios PRs al día):

| Bloque | Items | Talla agregada |
|--------|-------|----------------|
| Backend cambios de modelo | BE-01 a BE-11 | ~1 día agente |
| Backend refresh tokens | BE-12 a BE-15 | ~1 día agente |
| Backend Wompi webhook + reschedule | BE-16, BE-17, BE-20 | ~1 día agente |
| Backend security fixes | SEC-06, SEC-09, SEC-10, SEC-11 | ~1 día agente |
| Frontend web | FE-01 a FE-07 | ~1 día agente |
| Mobile brechas turista | MO-10 a MO-13 | ~1 día agente |
| Mobile brechas proveedor | MO-20 a MO-24 | ~1 día agente |
| Mobile UX refinado | MO-30 a MO-32 | ~1 día agente |
| Mobile CI/CD | MO-03 | ~½ día agente + Play Store setup Franklin |
| Infra | INF-03, INF-07 | ~½ día |

**Estimación real**:
- **Agente**: ~8–10 días de trabajo distribuidos.
- **Franklin (review + merge + testing en staging)**: ~2 horas/día × 15 días = ~30 horas.
- **Testing con turistas / operadores reales**: 3–5 días (Luis coordina con los 3 operadores negociados).
- **Deploy productivo + hotfixes**: 2–3 días.
- **Total calendario**: **~2–3 semanas**.

**Resultado**: **MVP listo para salir a producción** con los 3 operadores negociados.

**Fecha estimada de MVP en producción**: ~2 a 3 semanas después de arrancar Fase 0.

---

### 🤖 Fase 2 — Agentes IA prioritarios (sin dependencia externa)

**Objetivo**: soltar los 2 agentes que no dependen de Twilio para empezar a validar el patrón de IA con usuarios reales.

| Item | Talla | Nota |
|------|:-----:|------|
| IA-00: tabla `agent_audit_log` | S | Migración + entidad JPA |
| IA-01: paquete shared (`ILlmClient`, budget, audit) | M | Base para todos los agentes |
| IA-02: Travel Concierge | M | Prompt + tests + integración con búsqueda |
| IA-07: Operator Support | M | Prompt + integración con wizard tour |
| IA-09: Traducción Google Cloud Translation | S | Ya presupuestado |
| MO-40 Firebase Cloud Messaging (push) | M | Setup FCM + integración backend + mobile |
| IA-03, IA-04: Twilio onboarding en paralelo | — | **Bloqueado por aprobación Meta — arrancar ASAP** |

**Estimación real**:
- **Agente**: ~3–4 días trabajo.
- **Iteración de prompts** (probar Travel Concierge con búsquedas reales, ajustar): 3–5 días.
- **Franklin (review + observar métricas)**: 2–3 horas/día × 10 días.
- **Twilio WABA en paralelo**: se arranca aquí, se aprueba durante Fase 3.
- **Total calendario**: **~1.5–2 semanas**.

**Resultado**: 2 agentes IA en producción + canal WhatsApp aprobándose.

---

### 📱 Fase 3 — Twilio + agentes WhatsApp + mobile capabilities

**Objetivo**: cerrar el loop de comunicación con turistas y operadores + capabilidades solo-mobile.

| Item | Talla | Nota |
|------|:-----:|------|
| IA-05: Support 24/7 (WhatsApp + Twilio) | L | Prompt + testing exhaustivo (maneja dinero via reagendamiento) |
| IA-06: Desert Shopping Cart | S | Más simple que Support 24/7 |
| MO-41, MO-42: geolocalización | S+S | "Cerca de mí" + "cómo llegar" |
| MO-43: deep-linking | S | Universal links + WhatsApp share |

**Bloqueo real de esta fase**: **aprobación de Twilio WABA + plantillas por Meta**.
- Típico: 2 días – 2 semanas (Meta es imprevisible).
- Recomendación: iniciar el proceso al final de Fase 1 para que esté listo aquí.

**Estimación real**:
- **Agente**: ~2 días de trabajo.
- **Franklin review + iteración**: ~3–5 días.
- **Testing con Twilio**: ~2 días.
- **Total calendario si Twilio ya está aprobado**: **~1 semana**.
- **Total calendario si Twilio aún en revisión**: **hasta 2–3 semanas** (bloqueado por Meta).

---

### 📈 Fase 4 — Post-MVP: dashboards, backoffice avanzado, refinamiento

**Objetivo**: dar el salto de "MVP funcional" a "producto que retiene y escala".

| Bloque | Items | Talla |
|--------|-------|-------|
| Dashboards backoffice | FE-08, FE-09, FE-10 | ~2 días agente |
| Mobile refinamiento | MO-33, MO-34 | ~1 día agente |
| Mobile operario robusto | MO-50, MO-51 | ~1 día agente |
| Agente Backoffice Support | IA-08 | ~1 día agente |
| Agente moderación reseñas | IA-10 | ~1 día agente + iteración con datos reales |
| Dashboard observabilidad agentes | IA-11 | ~½ día |
| Correos crédito por expirar | BE-18, BE-19 | ~½ día |

**Estimación real**:
- **Agente**: ~6–8 días trabajo.
- **Franklin (review, testing dashboards, observar moderación IA)**: ~3–5 días.
- **Total calendario**: **~2 semanas**.

---

### 🌐 Fase 5 — Nice-to-have y roadmap 12m+

Estos items **no tienen deadline** — se hacen cuando el MVP esté estable y validado en producción.

| Bloque | Estimación agente |
|--------|-------------------|
| Mobile solo-mobile avanzado (MO-35, MO-44, MO-45) | ~3 días agente |
| iOS: portar todo lo mobile | ~5–7 días agente + testing en iOS device |
| Marketplace B2B | ~1 semana agente |
| Módulo Hot Sale (InDriver-like) | ~1 semana agente |
| Publicidad para negocios locales | ~1 semana agente |
| Duty Free / Click & Collect | ~1 semana agente |
| Herramienta gestión reservas del operador (integraciones Viator/Airbnb/Booking) | ~2 semanas agente |
| Integración DIMAR PDF → auto-generado | ~1 día agente |
| Payouts automatizados vía APIs pasarelas | ~1 semana agente |

**Estimación real de todo el bloque**: **~6–10 semanas de agente + validación + rollout controlado**.

---

## Diagrama de dependencias (esencial)

```
                           ┌──────────────────────────────┐
                           │  FASE 0 — Higiene y Seguridad │
                           │  (7 días, todo el equipo)     │
                           └───────────────┬───────────────┘
                                           │
                    ┌──────────────────────┼──────────────────────┐
                    ▼                      ▼                      ▼
        ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
        │  Backend         │  │  Frontend Web    │  │  Mobile          │
        │  BE-01..BE-20    │  │  FE-01..FE-07    │  │  MO-03..MO-32    │
        │  SEC-06,09,10,11 │  │                  │  │                  │
        └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
                 │                     │                     │
                 └─────────────────────┼─────────────────────┘
                                       ▼
                           ┌──────────────────────────┐
                           │  FASE 1 — MVP core        │
                           │  (4 semanas calendario)   │
                           └──────────────┬───────────┘
                                          │
                                          ▼
                           ┌──────────────────────────┐
                           │  FASE 2 — Agentes IA sin   │
                           │  dependencia Twilio        │
                           │  IA-00,01,02,07,09         │
                           │  + IA-03,04 en paralelo    │
                           └──────────────┬───────────┘
                                          │
                                          ▼
                           ┌──────────────────────────┐
                           │  FASE 3 — Twilio + Mobile  │
                           │  incremental               │
                           │  IA-05,06 + MO-40,41,42,43 │
                           └──────────────┬───────────┘
                                          │
                                          ▼
                           ┌──────────────────────────┐
                           │  FASE 4 — Post-MVP         │
                           │  Dashboards + refinamiento │
                           └──────────────┬───────────┘
                                          │
                                          ▼
                           ┌──────────────────────────┐
                           │  FASE 5 — Roadmap 12m+     │
                           │  iOS, B2B, Duty Free, etc. │
                           └──────────────────────────┘
```

---

## Cronograma resumido (con agente IA)

Asumiendo arranque **2026-07-08** con dedicación de **~2 horas/día de Franklin** para review + validación:

| Fase | Duración calendario | Fecha estimada de cierre | Entregable |
|------|---------------------|--------------------------|------------|
| **Fase 0** | 3–5 días | ~2026-07-13 | Producción segura + código respaldado + alertas |
| **Fase 1** | 2–3 semanas | **~2026-08-03** | **MVP en productivo** con los 3 operadores 🎯 |
| **Fase 2** | 1.5–2 semanas | ~2026-08-17 | 2 agentes IA en producción, Twilio en aprobación |
| **Fase 3** | 1–3 semanas | ~2026-09-07 | WhatsApp + geolocalización + deep-links |
| **Fase 4** | 2 semanas | ~2026-09-21 | Dashboards + operario robusto + moderación IA |
| **Fase 5** | continuo | 2026-Q4 en adelante | Roadmap 12m: iOS, B2B, expansión |

**Comparación con estimación humana (3 devs full-time)**:

| Fase | Con 3 devs humanos | Con agente IA |
|------|:------------------:|:-------------:|
| Fase 0 | 2 semanas | ~1 semana |
| Fase 1 | 4 semanas | **~3 semanas** |
| Fase 2 | 4 semanas | ~2 semanas |
| Fase 3 | 4 semanas | ~2 semanas |
| Fase 4 | 6 semanas | ~2 semanas |
| **Total hasta Fase 4** | **20 semanas (~5 meses)** | **~10 semanas (~2.5 meses)** |

El **factor real de aceleración** no es 10× ni 20× (aunque el código puro sí lo es) porque hay techos:
- Review humano de Franklin (cuello de botella real).
- Aprobaciones externas (Twilio, Play Store).
- Testing con operadores y turistas reales.
- Deployment cuidadoso a producción.

---

## Techos NO comprimibles por el agente

| Actividad | Tiempo típico | ¿Por qué no se acelera? |
|-----------|:-------------:|--------------------------|
| Aprobación WABA por Meta (via Twilio) | 2 días – 2 semanas | Depende del reviewer de Meta |
| Aprobación plantillas WhatsApp | 24–72 h por template | Meta review manual |
| Play Store internal track publish | 2–4 h | Google review automático |
| Play Store production release | 1–3 días | Google review manual + testing tracks |
| Testing con turistas reales | Días–semanas | Necesita turistas reales, no simulados |
| Onboarding de operadores | 1–2 semanas por operador | Reuniones, entrenamiento, ajustes |
| Rotación de secretos + verificación producción | Horas | Cuidado para no romper prod |
| Reunión de decisiones con Luis | Variable | Disponibilidad de Luis |

**Recomendación**: mientras el agente construye Fase 1, **arrancar en paralelo**:
- Solicitud Twilio + WABA (para tener aprobación cuando llegue Fase 3).
- Coordinar con los 3 operadores negociados para testing de Fase 1 (agenda, canal de feedback).
- Preparar Play Store console para el primer upload de APK.

---

## Métricas de éxito por fase

| Fase | Métrica objetivo |
|------|------------------|
| Fase 0 | 0 vulnerabilidades CRITICAL abiertas. Backup verificado. Alertas emiten. |
| Fase 1 | Primera reserva pagada en productivo. `refresh_token` rotando sin fricción. |
| Fase 2 | Travel Concierge respondiendo ≥ 60% de queries sin escalar. Override rate < 20%. |
| Fase 3 | Reagendamientos autónomos por Support 24/7 ≥ 40%. Deep-link → app open ≥ 30%. |
| Fase 4 | GMV mensual > 100M COP. NPS operador ≥ 30. Reservas recurrentes > 10%. |
| Fase 5 | Meta 12m del [doc 01](01-vision-y-negocio.md): 70 operadores, 150 tours, 400M GMV/mes. |

---

## Total de esfuerzo estimado (agente + review)

| Área | Trabajo agente | Review + validación Franklin | Techos externos |
|------|:--------------:|:---------------------------:|-----------------|
| Seguridad | ~4–6 h | ~4–6 h | — |
| Backend | ~3 días | ~10 h review + staging | — |
| Frontend Web | ~3 días | ~10 h review + prueba visual | — |
| Mobile | ~5–6 días | ~15 h review + testing en device | Play Store (~2–4 h por release) |
| Infra | ~1 día | ~1 día config manual GCP | — |
| Agentes IA | ~3–4 días | ~3–5 días iteración prompts | Twilio WABA (2 días – 2 semanas) |
| **Total agente** | **~18–22 días de trabajo agente** | **~40–60 horas de Franklin** | Techos externos |

**En calendario real** (arrancando 2026-07-08 con Franklin dando ~2h/día):

- **Hasta MVP en producción (Fase 1 cerrada)**: **~3 semanas** → primera reserva pagada a inicios de agosto.
- **Hasta agentes IA soltos (Fase 2+3)**: **~6–7 semanas** → mediados de septiembre.
- **Hasta producto post-MVP maduro (Fase 4)**: **~9–10 semanas** → finales de septiembre / inicios de octubre.
- **Fase 5** (roadmap 12m+): continuo, en función del feedback de los primeros meses productivos.

---

## Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Twilio demora en aprobar WABA | Bloquea Fase 3 | Fallback email para Desert Cart (ya considerado). Adelantar tramites en Fase 1 |
| El operador no adopta la app (prefiere WhatsApp del negocio) | MVP no valida | Onboarding 1-a-1 con los 3 primeros operadores. Feedback semanal |
| Costos de LLM se disparan con volumen inesperado | Presupuesto sale de rango | `BudgetGuard` que corta a los $200/mes. Reevaluar mensualmente |
| Firebase migration rompe login social existente | Usuarios legítimos bloqueados | Migrar por email match (Token Exchange busca por email, respeta usuario existente) |
| Cambio de `isUnlimitedCapacity` rompe schedules existentes | Datos corruptos | Migración con backfill + verificar en staging antes de prod |
| Refresh token bug en producción → todos los usuarios deslogueados | Incidente serio | Feature flag para activar refresh solo a % de usuarios. Rollback plan |
| Wompi webhook falla silenciosamente | Pagos huérfanos | Reconciliación por job (BE-17) como safety net |
| Cuello de botella en review de Franklin bloquea al agente | Retraso en cronograma | Priorizar PRs P0/P1. Franklin puede aprobar cambios triviales rápido con confianza en el agente para código de bajo riesgo |
| Agente propone algo incorrecto y Franklin lo aprueba sin ver | Bug en producción | Testing en staging antes de prod obligatorio. Rollback plan por commit |

---

## Puntos abiertos que aún requieren decisión

| # | Punto | Quién decide | Cuándo |
|---|-------|--------------|--------|
| 1 | Tamaño exacto de imágenes (validar 1920px vs template Angular real) | Frontend + Luis | Antes de BE-10 / FE-03 |
| 2 | Prioridad relativa entre "Backoffice avanzado" y "Mobile refinado" en Fase 4 | Luis | Al terminar Fase 3 |
| 3 | iOS: ¿en Fase 5 o antes? | Luis | Al terminar Fase 2 |
| 4 | Push provider para FCM (Firebase Messaging vs alternativas serverless) | Franklin | Al iniciar MO-40 |
| 5 | Política de retención de logs de agente | Franklin + Luis | Al iniciar IA-00 |

---

## Uso de este documento

Este backlog es **vivo**. Al cerrar un ítem:
1. Marcar 🟢 en la tabla del área correspondiente.
2. Enlazar el commit o PR que lo resuelve.
3. Actualizar el cronograma si hay desviación significativa.

Al aparecer un nuevo ítem no contemplado:
1. Agregarlo al área que corresponda.
2. Asignar prioridad y talla.
3. Verificar si desplaza algo del cronograma.

---

## Referencias

- [00 — README](00-README.md) — log de decisiones vivas.
- [05 — Reglas de negocio](05-reglas-de-negocio.md) — origen de la mayoría de items BE.
- [12 — Seguridad y autenticación](12-seguridad-y-auth.md) — origen de items SEC.
- [13 — Despliegue y CI/CD](13-despliegue-cicd.md) — origen de items INF.
- [14 — Web vs Mobile](14-gap-web-mobile.md) + [15 — MVP mobile estado](15-mvp-mobile-estado.md) — origen de items MO.
- [16 — Agentes IA](16-agentes-ia.md) — origen de items IA.
- `security-remediation-plan.md` (interno, fuera del repo) — detalle de vulnerabilidades.
- `cicd-improvement-plan.md` (interno, fuera del repo) — plan CI/CD detallado.
