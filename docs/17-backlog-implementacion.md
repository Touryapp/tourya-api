# 17 — Backlog de implementación y plan de ejecución

Consolida todo el trabajo pendiente que emergió de los documentos [00–16](00-README.md) en un **backlog priorizado** con estimación de esfuerzo, dependencias y **plan por fases**.

> **Objetivo**: llevar Tourya de "documentación completa" a "MVP productivo" con un roadmap ejecutable y realista para el equipo actual (1 backend, 1 frontend, 1 mobile — ver equipo en [01](01-vision-y-negocio.md)).

---

## Convenciones

### Prioridad

| Nivel | Significado |
|-------|-------------|
| **P0** | Bloqueante de producción. No podemos salir en productivo sin esto. |
| **P1** | Necesario para el MVP funcional. |
| **P2** | Post-MVP importante — 3-6 meses después de salir. |
| **P3** | Nice-to-have / futuro / roadmap 12m+. |

### Esfuerzo estimado

| Talla | Días de trabajo |
|-------|-----------------|
| **XS** | < 1 día |
| **S** | 1–3 días |
| **M** | 3–7 días |
| **L** | 1–2 semanas |
| **XL** | 2–4 semanas |

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
| SEC-01 | Rotar JWT secret y mover a **GCP Secret Manager** | P0 | S | — | C-1, [12](12-seguridad-y-auth.md) |
| SEC-02 | Rotar Wompi integrity secret y mover a Secret Manager | P0 | S | — | C-2 |
| SEC-03 | Cerrar exposición de Actuator (solo `/health`, `/info`) | P0 | XS | — | C-3 |
| SEC-04 | Fix bug de replay en token de activación (invalidar tras uso) | P0 | XS | — | C-4, RN-003 |
| SEC-05 | Cerrar filtración de PII en `/public/bookings/{id}` | P0 | S | — | C-5 |
| SEC-06 | Reemplazar Firebase social login por **Token Exchange** (Google + Facebook) | P0 | M | — | C-6, RN-006, [social-login-google-facebook.md](../social-login-google-facebook.md) |
| SEC-07 | Rotar service account key GCS + eliminarlo del repo + `.gitignore` | P0 | XS | — | H-1 |
| SEC-08 | Eliminar `System.out.println` de password temporal | P0 | XS | — | H-2 |
| SEC-09 | Rate limiting en `/auth/authenticate` y `/auth/register` | P1 | S | — | H-3 |
| SEC-10 | Lockout tras 5 intentos fallidos (con backoff exponencial) | P1 | S | SEC-09 | H-4 |
| SEC-11 | Limpiar CORS: quitar IPs AWS legacy, agregar `tourya.co` | P1 | XS | — | H-7 |

**Subtotal seguridad**: ~15 días.

---

### B. Backend — cambios de modelo y reglas aprobadas por Luis

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| BE-01 | Nueva migración: agregar `Tour.percentageTourya` + backfill = 15 (default) | P1 | S | — | RN-015 |
| BE-02 | Refactor: al crear slot, usar `Tour.percentageTourya` como default para `slotPercentageTourya` | P1 | S | BE-01 | RN-015 |
| BE-03 | Migración: eliminar `isUnlimitedCapacity` de `tour_schedule` (drop column) | P1 | XS | — | RN-021 |
| BE-04 | Ajustar `TourScheduleConfigGeneralService` para no leer/escribir el campo eliminado | P1 | S | BE-03 | RN-021 |
| BE-05 | Agregar razones `LEGAL_OBLIGATIONS`, `CHANGE_OF_PLANS` al enum `CancellationReasonEnum` | P1 | XS | — | RN-030 |
| BE-06 | Refactor `holdMinutes` (carrito) a `app_config` — leer de BD, no de property | P1 | S | — | RN-022 |
| BE-07 | Refactor buffer de payout (2 días) a `app_config` | P1 | S | — | RN-040 |
| BE-08 | Refactor expiración de créditos (1 año) a `app_config` | P1 | S | — | RN-036 |
| BE-09 | Endpoint GET/PUT `/config` para ADMIN gestione `app_config` desde backoffice | P1 | S | BE-06, BE-07, BE-08 | RN-022 |
| BE-10 | Endpoint validador de galería del tour (7 imgs / 5 MB / horizontal 1920px) | P1 | S | RN-013 revisada | RN-013 |
| BE-11 | Validación: RN-045 documentos KYB obligatorios (RUT, RNT, cert bancaria, cédula RL, cámara comercio, pólizas) | P1 | S | — | RN-045 |
| BE-12 | Migración: crear tabla `refresh_token` con `jti`, `family_id`, `previous_jti`, `expires_at`, `revoked_at` | P1 | S | — | RN-005 |
| BE-13 | Refactor `JwtService` → emitir `access_token` + `refresh_token` con expiraciones por rol | P1 | M | BE-12 | RN-005 |
| BE-14 | Endpoint `POST /auth/refresh` con rotación + detección de reuso (revoca familia) | P1 | S | BE-13 | RN-005 |
| BE-15 | Endpoint `POST /auth/logout` que revoca la familia entera de tokens | P1 | XS | BE-14 | RN-005 |
| BE-16 | Webhook server-side Wompi: endpoint `POST /public/wompi/webhook` con verificación de firma | P1 | M | — | RN-025 |
| BE-17 | Reconciliación de pagos huérfanos (job) — busca `TEMPORAL` que Wompi confirmó pero no llegó | P1 | S | BE-16 | RN-025 |
| BE-18 | Correo automático "crédito por expirar" (30 días y 7 días antes) | P2 | S | — | RN-036 |
| BE-19 | Correo automático "crédito expirado" | P2 | XS | BE-18 | RN-036 |
| BE-20 | Verificar y completar flujo de reagendamiento (3 casos: igual/menor/mayor precio) | P1 | S | — | RN-033 |

**Subtotal backend**: ~30 días.

---

### C. Frontend Web (Angular)

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| FE-01 | Adaptar login: guardar `refresh_token` en cookie HttpOnly + auto-refresh transparente en interceptor | P1 | S | BE-13 | RN-005 |
| FE-02 | Migrar login social: quitar Firebase SDK, integrar `@abacritt/angularx-social-login` u equivalente compatible Angular 19 | P0 | M | SEC-06 | — |
| FE-03 | Validación en el uploader de galería: 7 imgs máx, 5 MB máx, formato horizontal 1920px | P1 | S | RN-013 revisada | RN-013 |
| FE-04 | UI para ADMIN de `app_config` (holdMinutes, buffer payout, expiración créditos, presupuesto agentes) | P1 | S | BE-09 | RN-022 |
| FE-05 | UI backoffice: gestionar `Tour.percentageTourya` en formulario de aprobación de tour | P1 | S | BE-01 | RN-015 |
| FE-06 | Agregar UI para las 2 nuevas razones de cancelación en cliente | P1 | XS | BE-05 | RN-030 |
| FE-07 | Quitar campo `isUnlimitedCapacity` en config de schedule (mover a formulario de tour) | P1 | XS | BE-03 | RN-021 |
| FE-08 | Dashboard financiero del backoffice: GMV, net revenue, cuentas por pagar, CAC | P2 | L | — | Doc 01 roadmap |
| FE-09 | Dashboard operativo del backoffice: tasa conversión/cancelación, tours de alto riesgo | P2 | L | — | Doc 01 roadmap |
| FE-10 | Gestor de disputas | P2 | L | — | Doc 01 roadmap |

**Subtotal frontend web**: ~35 días.

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

**Subtotal mobile**: ~55 días.

---

### E. Infraestructura y observabilidad

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| INF-01 | Configurar 8 alertas mínimas en Cloud Monitoring (5xx backend/frontend, latencia P95, DB conn/CPU/disco, cost anomaly, Cloud Run throttled) | P0 | S | — | [13](13-despliegue-cicd.md) |
| INF-02 | Habilitar backup automático de Cloud SQL (retention 30 días) | P0 | XS | — | [13](13-despliegue-cicd.md) |
| INF-03 | Crear Cloud Build trigger para `main` (deploy productivo automatizado) | P1 | S | — | [13](13-despliegue-cicd.md) |
| INF-04 | Coverage gate 80% con JaCoCo | P2 | S | — | `cicd-improvement-plan.md` |
| INF-05 | SpotBugs + PMD en CI | P2 | S | INF-04 | `cicd-improvement-plan.md` |
| INF-06 | Snyk Free en CI (solo push a main/develop) | P2 | S | — | `cicd-improvement-plan.md` |
| INF-07 | Branch Protection en `develop` y `main` (requiere PR + CI verde) | P1 | XS | INF-04, INF-06 | `cicd-improvement-plan.md` |
| INF-08 | Migrar `.env` completo a env vars documentadas + Secret Manager | P0 | S | SEC-01, SEC-02 | [13](13-despliegue-cicd.md) |
| INF-09 | Limpieza de ramas viejas en GitHub (`Touryapp/tourya-api` tiene ~80 ramas) | P3 | XS | — | — |

**Subtotal infra**: ~10 días.

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

**Subtotal agentes**: ~30 días.

---

## Plan por fases

### 🚨 Fase 0 — Higiene y seguridad crítica (semanas 1–2)

**Objetivo**: **cerrar el techo antes de invitar gente a la fiesta**. Sin esto no salimos a producción.

| Ítem | Talla | Owner sugerido |
|------|:-----:|----------------|
| MO-00 Repo GitHub mobile | XS | Mobile dev |
| MO-01 URL backend mobile | XS | Mobile dev |
| MO-02 Syncfusion license | XS | Mobile dev |
| SEC-01 JWT secret a Secret Manager | S | Backend |
| SEC-02 Wompi secret a Secret Manager | S | Backend |
| SEC-03 Cerrar Actuator | XS | Backend |
| SEC-04 Fix replay token activación | XS | Backend |
| SEC-05 PII leak `/public/bookings` | S | Backend |
| SEC-07 Rotar y eliminar SA key | XS | Backend + DevOps |
| SEC-08 Eliminar `println` de password | XS | Backend |
| INF-01 Alertas Cloud Monitoring | S | DevOps |
| INF-02 Backup Cloud SQL | XS | DevOps |
| INF-08 Env vars + Secret Manager completo | S | DevOps |

**Total Fase 0**: ~7 días de trabajo distribuido.

**Resultado**: producción segura, código respaldado, observabilidad mínima.

---

### 🏗️ Fase 1 — MVP core (semanas 3–6)

**Objetivo**: implementar los cambios de modelo aprobados por Luis y refactors habilitantes.

**Backend (paralelo con frontend/mobile)**:
- BE-01, BE-02: `percentageTourya`
- BE-03, BE-04: eliminar `isUnlimitedCapacity` de schedule
- BE-05: razones nuevas cancelación
- BE-06 → BE-09: `holdMinutes`/buffer/expiración créditos configurables + endpoint
- BE-10: validador galería (esperar validación template Angular RN-013)
- BE-11: docs KYB obligatorios
- BE-12 → BE-15: **Access + Refresh tokens** (tabla, `JwtService`, `/auth/refresh`, `/auth/logout`)
- BE-16, BE-17: **Webhook Wompi** + reconciliación
- BE-20: verificar flujo reschedule
- SEC-06: **Token Exchange** Google/Facebook (elimina Firebase)
- SEC-09, SEC-10: rate limiting + lockout
- SEC-11: limpiar CORS

**Frontend web**:
- FE-01: cookies HttpOnly + auto-refresh interceptor
- FE-02: quitar Firebase SDK
- FE-03: validación galería
- FE-04: UI ADMIN de `app_config`
- FE-05: UI backoffice `Tour.percentageTourya`
- FE-06: razones nuevas UI
- FE-07: quitar campo `isUnlimitedCapacity` schedule

**Mobile**:
- MO-03: CI/CD APK firmado
- MO-10 → MO-13: brechas turista (reseña, wishlist, créditos, login social)
- MO-20 → MO-24: brechas proveedor (responder reseña, payouts, operarios)
- MO-30 → MO-32: refinar UX (autosave, compresión, upload background)

**Infra**:
- INF-03: Cloud Build trigger main
- INF-07: Branch Protection

**Total Fase 1**: ~50 días distribuidos entre 3 devs → ~4 semanas de calendario.

**Resultado**: **MVP listo para salir a producción** con los 3 operadores negociados.

---

### 🤖 Fase 2 — Agentes IA prioritarios (semanas 7–10)

**Objetivo**: soltar los 2 agentes que **no dependen de Twilio** para empezar a validar el patrón de IA con usuarios reales.

- IA-00: tabla `agent_audit_log`
- IA-01: paquete shared (`ILlmClient`, budget guard, audit)
- IA-02: **Travel Concierge** (turista)
- IA-07: **Operator Support** (proveedor durante wizard)
- IA-09: traducción automática Google Cloud Translation (integrar con `Tour.saveAll`)

**En paralelo**:
- IA-03, IA-04: onboarding Twilio + servicio de mensajería
- MO-40: Firebase Cloud Messaging (push)

**Total Fase 2**: ~15 días.

**Resultado**: 2 agentes IA en producción + canal WhatsApp listo para los siguientes.

---

### 📱 Fase 3 — Twilio + agentes WhatsApp + mobile capabilities (semanas 11–14)

**Objetivo**: cerrar el loop de comunicación con turistas y operadores + darle al mobile las capacidades que solo el móvil hace bien.

- IA-05: **Support 24/7** (WhatsApp, Twilio)
- IA-06: **Desert Shopping Cart**
- MO-41, MO-42: geolocalización
- MO-43: deep-linking

**Total Fase 3**: ~15 días.

---

### 📈 Fase 4 — Post-MVP: dashboards, backoffice avanzado, refinamiento mobile (semanas 15–20)

**Objetivo**: dar el salto de "MVP funcional" a "producto que retiene y escala".

- FE-08: dashboard financiero backoffice
- FE-09: dashboard operativo backoffice
- FE-10: gestor de disputas
- MO-33, MO-34: UX proveedor refinada
- MO-50, MO-51: modo offline + vista operario mejorada
- IA-08: Backoffice Support agent
- IA-10: moderación de reseñas IA
- IA-11: dashboard observabilidad agentes
- BE-18, BE-19: correos de expiración de crédito

**Total Fase 4**: ~30 días.

---

### 🌐 Fase 5 — Nice-to-have y roadmap 12m+ (semanas 21+)

- MO-35: dictado de voz
- MO-44: wallet integration
- MO-45: widget próxima reserva
- iOS (todo el trabajo mobile portado)
- Marketplace B2B
- Módulo Hot Sale (InDriver-like)
- Publicidad
- Duty Free / Click & Collect
- Herramienta de gestión de reservas del operador (integraciones Viator/Airbnb/Booking)
- Integración DIMAR PDF → `MaritimActivityReport` automático
- Payouts automatizados vía APIs Wompi/Mercado Pago

**Total Fase 5**: 3–6 meses de trabajo distribuido.

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

## Cronograma resumido

| Fase | Duración | Fecha estimada de cierre | Entregable |
|------|----------|--------------------------|------------|
| **Fase 0** | 2 semanas | 2026-07-21 | Producción segura + código respaldado |
| **Fase 1** | 4 semanas | 2026-08-18 | **MVP en productivo** con los 3 operadores |
| **Fase 2** | 4 semanas | 2026-09-15 | 2 agentes IA en producción + Twilio listo |
| **Fase 3** | 4 semanas | 2026-10-13 | WhatsApp + geolocalización + deep-links |
| **Fase 4** | 6 semanas | 2026-11-24 | Dashboards + rol operario robusto + moderación IA |
| **Fase 5** | continuo | 2027+ | Roadmap 12m: iOS, B2B, expansión |

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

## Total de esfuerzo estimado

| Área | Días |
|------|------|
| Seguridad | 15 |
| Backend | 30 |
| Frontend Web | 35 |
| Mobile | 55 |
| Infraestructura | 10 |
| Agentes IA | 30 |
| **Total** | **~175 días de trabajo** |

Con equipo de **3 devs a full-time**: ~60 días de calendario → **~3 meses hasta cerrar Fase 4**.
Con equipo de 3 devs a **half-time** (situación real dado que hay otras responsabilidades): ~6 meses.

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
