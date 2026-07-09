# Tourya — Documentación maestra

Este directorio contiene la **documentación de referencia** de la plataforma Tourya, generada a partir del código real (backend Spring Boot, frontend Angular, app MAUI Android, base de datos PostgreSQL, infraestructura GCP).

**Objetivo**: que cualquier desarrollador, PM, agente IA o stakeholder nuevo pueda leer estos documentos en orden y entender el sistema sin necesidad de explicaciones verbales.

---

## Cómo usar esta documentación

- **Leer en orden numérico la primera vez** (00 → 13). Toma ~2–3 horas.
- Después usar como referencia.
- Cualquier cambio en una regla de negocio, entidad o decisión técnica **se documenta primero acá** y después se implementa en código.
- Si el código contradice la doc, la doc gana (corregimos código) — salvo cuando el código ya está en producción, en cuyo caso se actualiza la doc.

---

## Convención de marcas

Cada afirmación en estos documentos está marcada con su origen:

| Marca | Significado |
|-------|-------------|
| ✅ **DEDUCIDO DEL CÓDIGO** | Inferido leyendo el código fuente, migraciones, configuraciones |
| 📌 **PENDIENTE LUIS** | Requiere validación o información del cliente (PO) |
| ❓ **ASUNCIÓN** | Hipótesis basada en el dominio turístico — confirmar con Luis |
| ⚠️ **RIESGO / TECH DEBT** | Algo que requiere atención (vulnerabilidad, código viejo, configuración insegura) |

---

## Índice

### Fundamentos del negocio

| # | Documento | De qué trata |
|---|-----------|--------------|
| [01](01-vision-y-negocio.md) | Visión y negocio | Qué es Tourya, mercado objetivo (Colombia turismo), monetización, modelo de comisión |
| [02](02-glosario.md) | Glosario | Diccionario de términos: tour, schedule, slot, operador, RNT, Wompi, override, payout |
| [03](03-roles-y-actores.md) | Roles y actores | USER, PROVIDER, PROVIDER_OPERATOR, ADMIN, BACKOFFICE_OPERATION, AUTH externos |

### Dominio y reglas

| # | Documento | De qué trata |
|---|-----------|--------------|
| [04](04-entidades-dominio.md) | Entidades de dominio | 54 entidades JPA agrupadas en 10 bounded contexts, con relaciones |
| [05](05-reglas-de-negocio.md) | Reglas de negocio | Comisión Tourya, cancelaciones/refund, overrides, créditos, payouts, KYB, activación |
| [06](06-flujos-y-eventos.md) | Flujos y eventos | 8 flujos críticos: registro, creación de tour, schedule + precios, cart, checkout Wompi, cancelación, payout, KYB |

### Arquitectura

| # | Documento | De qué trata |
|---|-----------|--------------|
| [07](07-arquitectura-tecnica.md) | Arquitectura técnica | Stack: Spring Boot 3.4.4 + Angular 19 + MAUI .NET 10 + PostgreSQL + GCP (Cloud Run, Cloud SQL, GCS, Load Balancer) |
| [08](08-modelo-de-datos.md) | Modelo de datos | Schema PostgreSQL: 68 tablas, 61 migraciones, 20 SPs, JSONB para i18n, índices |

### Interfaces

| # | Documento | De qué trata |
|---|-----------|--------------|
| [09](09-api-design.md) | API design | 150+ endpoints en 35 controllers, convenciones REST, autenticación JWT, multipart |
| [10](10-mobile-spec.md) | Mobile spec (MAUI) | App Android: 25 vistas, 26 ViewModels, Syncfusion, Wompi WebView, QR scanner |

### Operación y entrega

| # | Documento | De qué trata |
|---|-----------|--------------|
| [11](11-integraciones.md) | Integraciones | Wompi (pagos), SMTP Gmail Workspace, GCS/S3 (storage), Firebase Auth (Google/Facebook), Google Translation (futuro) |
| [12](12-seguridad-y-auth.md) | Seguridad y autenticación | JWT, BCrypt, roles, login social, CORS, vulnerabilidades pendientes |
| [13](13-despliegue-cicd.md) | Despliegue GCP y CI/CD | Cloud Run, Cloud Build, GitHub Actions, ramas, dominios (tourya.co), variables de entorno |

### Análisis y roadmap

| # | Documento | De qué trata |
|---|-----------|--------------|
| [14](14-gap-web-mobile.md) | Análisis Gap Web vs Mobile | Qué debe existir en cada plataforma y por qué — evitar duplicar trabajo que no aporta valor |
| [15](15-mvp-mobile-estado.md) | Estado del MVP mobile vs alcance | Gap analysis granular del código MAUI actual contra el alcance del doc 14 + roadmap por ciclos |
| [16](16-agentes-ia.md) | Agentes IA | Travel Concierge, moderador de reseñas y otros agentes IA del roadmap (📌 pendiente contenido de Luis) |
| [17](17-backlog-implementacion.md) | Backlog de implementación y plan de ejecución | Backlog priorizado (P0–P3) por área + plan por fases + cronograma + riesgos |

---

## Status de actualización

Generación inicial: **2026-06-27**. Consolidación con aportes de Luis: **2026-06-28**.

Documentos basados en el estado del código en las ramas:

| Repo | Rama | Commit |
|------|------|--------|
| tourya-api | `develop` | `e6ecca7` |
| tourya-front | `develop` | `983f73a` |
| tourya-mobile | `master` | `0bc518f` (sin remoto GitHub) |

| Doc | Status | Notas |
|-----|--------|-------|
| 00 README maestro | ✅ Sync | Este índice |
| 01 Visión y negocio | ✅ Sync | Consolidado con aportes de Luis (mercado, metas 12 meses, roadmap) |
| 02 Glosario | ✅ Sync | Términos y aclaraciones de Luis integrados |
| 03 Roles y actores | ✅ Sync | ADMIN único, BACKOFFICE_OPERATION asignable, matriz corregida |
| 04 Entidades de dominio | ✅ Sync | 54 entidades; `TourReservation` legacy eliminado (commit `076f006`) |
| 05 Reglas de negocio | ✅ Sync | Políticas de cancelación, refresh tokens por rol, KYB docs, moderación IA en roadmap |
| 06 Flujos y eventos | ✅ Sync | 8 flujos completos |
| 07 Arquitectura técnica | ✅ Sync | Stack y deploy confirmado |
| 08 Modelo de datos | ✅ Sync | 68 tablas, 20 SPs, 61 migraciones + refactor pendiente `Tour.percentageTourya` |
| 09 API design | ✅ Sync | 150+ endpoints en 35 controllers |
| 10 Mobile spec | ✅ Sync | Estructura MAUI mapeada |
| 11 Integraciones | ✅ Sync | Wompi (webhook en roadmap), SMTP, GCS, Firebase |
| 12 Seguridad y autenticación | ⚠️ Críticos pendientes | Refresh tokens por rol definidos; vulnerabilidades en plan aparte |
| 13 Despliegue GCP y CI/CD | ✅ Sync | Pipelines AWS (legacy) + GCP confirmados |
| 14 Web vs Mobile — alcance | ✅ Sync (v2.0) | Reescrito con el alcance definido por Luis: mobile cubre crear tour, schedule y operarios porque los operadores están en la calle |
| 15 MVP mobile — estado | ✅ Sync | Gap analysis granular actualizado según el nuevo alcance del doc 14 |
| 16 Agentes IA | ✅ Sync (v1.0) | Luis subió contenido completo: 5 agentes core (Travel Concierge, Support 24/7, Desert Shopping Cart, Operator Support, Backoffice Support) + arquitectura + roadmap |
| 17 Backlog de implementación | ✅ Sync (v1.1) | Consolida trabajo pendiente en 6 áreas + plan por fases con estimaciones **agente IA + review humano** (~3 semanas hasta MVP, ~10 semanas hasta Fase 4) |

---

## Documentos relacionados (fuera de este directorio)

Estos documentos viven fuera del repo (en `D:/Users/Usuario/source/repos/tourya/`) por ser internos o sensibles:

| Documento | Ubicación | Naturaleza |
|-----------|-----------|------------|
| Plan de remediación de seguridad | `../../security-remediation-plan.md` | **Interno** — mapa de vulnerabilidades, no compartir |
| Plan de mejora CI/CD | `../../cicd-improvement-plan.md` | Interno — JaCoCo, SpotBugs, Snyk |
| Propuesta social login sin Firebase | `../../social-login-google-facebook.md` | Para cliente |
| Propuesta traducción automática | `../../traduccion-automatica-tours.md` | Para cliente |

---

## Convenciones de la documentación

- **Idioma**: español (mismo idioma del cliente y del dominio turístico colombiano).
- **Tono**: directo, sin academicismos. Mejor "el proveedor crea el tour" que "el actor de tipo proveedor genera el evento de creación del agregado tour".
- **Ejemplos concretos**: cuando sea posible, con nombres reales del dominio (Cartagena, Islas del Rosario, RNT 263565).
- **Referencias cruzadas**: si un doc menciona una entidad, link al doc donde se define.
- **Decisiones marcadas explícitamente** con `> **Decisión**: X — porque Y` para que sean fácil de auditar.

---

## Mantenimiento

Estos documentos son **vivos**. Reglas:

1. **Cambio menor** (typo, aclaración): editar directo + commit.
2. **Cambio de regla de negocio**: actualizar el doc + agregar entrada en changelog del doc + verificar que el código aún sea consistente.
3. **Cambio de arquitectura o entidad nueva**: discusión con equipo + actualización doc + plan de migración si afecta datos existentes.

Nadie escribe código que contradiga estos documentos sin antes actualizar el documento.

---

## Log de decisiones

Bitácora cronológica de decisiones aprobadas por el equipo. Sirve como registro rápido del "por qué" cuando el tiempo pasa y las decisiones parecen obvias.

| Fecha | Decisión | Aprobado por | Referencia |
|-------|----------|--------------|------------|
| 2026-06-28 | Meta iso-funcionalidad web/mobile ajustada: mobile cubre el ciclo operativo completo del proveedor (operadores en la calle en San Andrés) | Luis | [14](14-gap-web-mobile.md) |
| 2026-06-28 | `TourReservation` legacy eliminado | Equipo | migración 065 (commit `076f006`) |
| 2026-06-28 | Nuevas razones de cancelación: `LEGAL_OBLIGATIONS`, `CHANGE_OF_PLANS` | Luis | RN-030 |
| 2026-06-28 | Políticas de cancelación: Flexible/Estándar/Moderado/Estricto (100% refund si se cumple) | Luis | RN-031 |
| 2026-07-07 | `isUnlimitedCapacity` es LIMPIEZA (eliminar de `tour_schedule`), no migración — la fuente de verdad está en `Tour` | Luis | RN-021 |
| 2026-07-07 | Nuevo campo `Tour.percentageTourya` (default de comisión al aprobar el tour) | Luis | RN-015 |
| 2026-07-07 | `holdMinutes` del carrito debe ser configurable por ADMIN vía `app_config` | Luis | RN-022 |
| 2026-07-07 | Avanzar con **webhook server-side de Wompi** — prioridad alta para no perder pagos | Luis | RN-025 |
| 2026-07-07 | Refresh tokens diferenciados por rol (USER 60m/30d, PROVIDER 30m/7d/4h idle, ADMIN 15m/8h/15m idle). Rotación estricta + detección de reuso | Franklin | RN-005 |
| 2026-07-07 | WhatsApp vía **Twilio** (no integración directa con Meta) — velocidad de implementación sobre costo | Luis + Franklin | [16](16-agentes-ia.md), [11](11-integraciones.md) |
| 2026-07-07 | Tabla `agent_audit_log` es prerequisito para dar autonomía a cualquier agente IA | Franklin | [16](16-agentes-ia.md) |
| 2026-07-07 | Alertas básicas de Cloud Monitoring son prerequisito antes de salir a producción | Franklin | [13](13-despliegue-cicd.md) |
| 2026-07-07 | Presupuesto mensual agentes IA: **$100–200 USD/mes** para inferencia LLM | Luis | [16](16-agentes-ia.md) |
| 2026-07-08 | **Fase 0 higiene y seguridad crítica ejecutada en dev**: 5 fixes de código (SEC-03 Actuator, SEC-04 token replay, SEC-05 PII leak, SEC-08 password log, SEC-11 CORS) | Franklin | PRs #146–#150 |
| 2026-07-08 | **Migración de secretos runtime a GCP Secret Manager** en `tourya-project-dev`: JWT (rotado), Wompi, DB, SMTP. Fallbacks en `application.properties` como puente. | Franklin | PRs #151, #152 |
| 2026-07-08 | **Autenticación GitHub Actions → GCP migrada a Workload Identity Federation (WIF)**. Ya no se usan service account keys JSON. Key user-managed peligroso `df67...` deshabilitado. GitHub Secret `GCP_SA_KEY_DEV` eliminado. | Franklin | PR #153 |
| 2026-07-08 | **7 alertas de Cloud Monitoring activas** en dev (5xx backend/frontend, latencia P95, Cloud SQL CPU/disk/connections, container CPU). Canal: email a owner. Cost anomaly (8ª) queda pendiente por requerir Billing Budget separado. | Franklin | INF-01 |
| 2026-07-08 | **Backups automáticos de Cloud SQL habilitados** en dev: diarios 03:00 UTC, retention 30 días, multi-region `us`, Point-in-Time Recovery activo (7 días de transaction logs). | Franklin | INF-02 |
| 2026-07-08 | **🎉 Fase 0 completa en dev**: 5 fixes de código, Secret Manager, WIF, alertas y backups. Solo pendiente: cost anomaly alert (billing budget). Listos para arrancar Fase 1 (MVP core). | Franklin | Fase 0 cierre |
| 2026-07-08 | QR baseUrl AWS legacy resuelto — `ReservationQrService` lee de env var con fallback a `https://tourya.co/home` | Franklin | PR #154 |
| 2026-07-08 | Fallbacks hardcoded de JWT y Wompi secrets **removidos** de `application.properties`. Fail-fast en producción: si Cloud Run pierde acceso a Secret Manager, la app falla al arrancar en vez de usar valores viejos | Franklin | PR #155 |
| 2026-07-08 | **Fase 1 arrancada — BE-16/17 Wompi webhook + reconciliación operacional en dev**: endpoint `POST /public/wompi/webhook` verifica firma HMAC-SHA256 con events secret, persiste todos los eventos en tabla `wompi_webhook_event`. Job cada 5 min matchea contra Payments y detecta pagos huérfanos. URL configurada en dashboard Wompi Sandbox por Luis. Verificado end-to-end. | Franklin | PRs #156 + #157 + #158 |
| 2026-07-08 | **BE-12/13/14/15 refresh tokens implementados**: nueva tabla `refresh_token` + endpoints `/auth/refresh` y `/auth/logout` + rotación con detección de reuso + logout server-side. Backwards compat: response de `/auth/authenticate` mantiene `token` (legacy alias) + agrega `accessToken` y `refreshToken`. Angular/MAUI existentes siguen funcionando sin cambios. | Franklin | PR #159 |
| 2026-07-09 | **BE-06/07/08/09 configs a `app_config`**: 3 valores hardcoded (holdMinutes, buffer payout, expiración crédito) migrados a la tabla `app_config` con endpoint `PUT /config/{key}` protegido con `@PreAuthorize("hasRole('ADMIN')")`. ADMIN puede ajustar sin re-deploy. Corrección detectada: expiración de créditos usa 6 meses en código (no 1 año como decía el backlog); se mantuvo 6 como default. | Franklin | PR #160 |
| 2026-07-09 | **BE-10 validador RN-013 en galería** aplicado en `POST /tours/{id}/gallery/sync` y `/syncWithUpdate`. All-or-nothing: rechazo con lista de issues + rollback antes de tocar S3. 3 keys nuevas en `app_config` (`GALLERY_MAX_SIZE_MB`, `GALLERY_MIN_WIDTH_PX`, `GALLERY_MAX_IMAGES_PER_TOUR`). Decisión de diseño: tocar los métodos existentes en vez de crear endpoint alterno ("no hay cosas más permanentes que el mientras tanto"). Descartado "ancho recomendado 1920px" por ser solo recomendación. | Franklin | PR #161 |
| 2026-07-09 | **BE-11 RN-045 KYB docs obligatorios con feature flag OFF**: `PUT /requestProvider/user/send` valida (cuando flag=1) que existan galleries adjuntas para los 7 `RequestProviderDocumentType` con `mandatory=true` (Cámara comercio, RUT, RNT, Cédula RL, Seguros operación, Contrato mandato, Contrato vinculación). Response 400 con `missingDocuments[]`. Default OFF para no bloquear los 12 requests en estado Created de QA. Descubrimiento adicional: hay ~5 providers ya aprobados históricamente con solo 2-3 documentos — se resuelven manualmente si Luis lo pide. | Franklin | PR #162 |
| 2026-07-09 | **BE-01/02 RN-015 default % Tourya por tour**: reactivado `tour.porcentaje_tourya` (existía como DEPRECATED desde 050 según decisión previa). Backfill de 38 tours a 0.15 + DEFAULT column = 0.15. `TourScheduleConfigGeneralService` en 2 puntos hereda del tour al crear slot nuevo (antes hardcode ZERO). Nombre queda en español — rename descartado por costo/valor. | Franklin | PR #163 |
| 2026-07-09 | **SEC-09 rate limiting en `/auth/**` con feature flag OFF**: `AuthRateLimitFilter` in-memory por IP, umbral configurable en `app_config` (60/min default). Se activa sin re-deploy con `PUT /config/AUTH_RATE_LIMIT_ENABLED`. Limitación: contador no coordinado entre réplicas de Cloud Run (max-instances=2 → hasta 2×). Aceptable como defensa en profundidad; migrar a Redis si Luis pide coordinación estricta. | Franklin | PR #164 |
| 2026-07-09 | **SEC-10 account lockout con backoff exponencial y feature flag OFF**: 2 columnas nuevas en `_user` (`failed_login_attempts`, `locked_until`). Al superar `AUTH_LOCKOUT_MAX_ATTEMPTS` (5 default), la cuenta se bloquea con backoff exponencial que dobla el tiempo por cada nuevo fallo (60s → 120s → 240s → ... cap 24h). Auto-unlock via `isAccountNonLocked()` cuando `locked_until < now()` — sin cron. Silencioso si el email no existe (evita filtrar cuentas). Complementa SEC-09: rate limit por IP + lockout por cuenta. H-4 resuelto. | Franklin | PR #165 |

---

## Changelog

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 2026-06-27 | Generación inicial completa basada en código existente |
| 1.1 | 2026-06-28 | Consolidación con aportes de Luis: visión/mercado (San Andrés), metas 12m, políticas de cancelación, refresh tokens por rol, refactor `Tour.percentageTourya` e `isUnlimitedCapacity`, moderación IA, webhook Wompi, KYB docs obligatorios, eliminación `TourReservation` legacy |
| 1.2 | 2026-06-28 | Nuevo doc `14-gap-web-mobile.md` — análisis funcional web vs mobile y propuesta de matizar la meta de iso-funcionalidad |
| 1.3 | 2026-07-06 | Nuevo doc `15-mvp-mobile-estado.md` — gap analysis del código MAUI actual contra la matriz del doc 14 + roadmap por ciclos |
| 1.4 | 2026-07-06 | Doc `14-gap-web-mobile.md` reescrito v2.0 con el alcance definido por Luis (operadores en la calle → mobile cubre crear tour, schedule y operarios). Doc `15-mvp-mobile-estado.md` actualizado en consecuencia. Doc `16-agentes-ia.md` esqueleto (Luis creó el placeholder, pendiente el contenido detallado) |
| 1.5 | 2026-07-07 | Luis completó `16-agentes-ia.md` con 5 agentes core, arquitectura y roadmap. Aclaración de Luis sobre `isUnlimitedCapacity`: es una LIMPIEZA (eliminar el campo de `tour_schedule`), no una migración. Ajustados docs 04, 05 y 08 en consecuencia. Correcciones ortográficas menores en 14 y 16 |
| 1.6 | 2026-07-07 | Consolidación de las 10 respuestas de Luis por WhatsApp: RN-005 (refresh tokens definidos con OWASP), RN-013 (galería propuesta a validar), RN-015 (percentageTourya aprobado), RN-022 (holdMinutes configurable aprobado), RN-025 (webhook Wompi aprobado), RN-030 (razones nuevas aprobadas). Decisión: WhatsApp vía Twilio (no Meta directo). Validaciones técnicas: `agent_audit_log` necesario, alertas Cloud Monitoring como prerequisito. Presupuesto agentes $100-200/mes aprobado. Actualizados docs 05, 11, 12, 13, 16 |
| 1.7 | 2026-07-07 | Nuevo doc `17-backlog-implementacion.md` — consolida todo el trabajo pendiente en 90+ items priorizados (P0-P3) distribuidos en 6 áreas (seguridad, backend, frontend web, mobile, infra, agentes IA). Plan por 5 fases con cronograma estimado (Fase 1 MVP → 2026-08-18) |
| 1.8 | 2026-07-07 | Ajustadas estimaciones del backlog al modelo de ejecución real (agente IA implementador + Franklin como revisor). Cronograma comprimido: **MVP en producción ~2026-08-03** (3 semanas en vez de 6). Se explicitan los techos no comprimibles: Twilio WABA (2d-2sem), Play Store, testing con turistas reales |
| 1.9 | 2026-07-08 | Fase 0 completada en dev — 5 fixes de código (SEC-03/04/05/08/11), migración de secretos runtime a GCP Secret Manager, y migración de autenticación GitHub Actions → GCP a Workload Identity Federation. Doc 12 actualizado con estado real de vulnerabilidades CRITICAL y HIGH |
| 2.0 | 2026-07-08 | **🎉 Fase 0 100% cerrada en dev**: código (5 PRs), secretos, WIF, alertas Cloud Monitoring (7/8) y backups Cloud SQL habilitados. Docs 05, 07, 11, 13, 17 sincronizados con estado real. Listos para Fase 1 |
| 2.1 | 2026-07-08 | **Fase 1 — 3 bloques cerrados en dev**: BE-16/17 (Wompi webhook + job de reconciliación) y BE-12/13/14/15 (refresh tokens con rotación + detección de reuso). Verificaciones end-to-end incluyendo webhook Wompi real desde Sandbox. Docs 05, 07, 09, 11, 12, 13, 17 sincronizados con estado real |
| 2.2 | 2026-07-09 | **Fase 1 — 4to bloque cerrado**: BE-06/07/08/09 configs (holdMinutes, buffer payout, expiración crédito) migradas a `app_config` con endpoint admin `PUT /config/{key}`. RN-022, RN-036, RN-040 actualizadas de "a implementar" a "implementado". Docs 05, 09, 17 sincronizados |
| 2.3 | 2026-07-09 | **Fase 1 — 5to bloque cerrado**: BE-10 validador RN-013 aplicado en endpoints existentes de galería (formato, tamaño, orientación, ancho mínimo, cuenta). Umbrales en `app_config` (GALLERY_MAX_SIZE_MB, GALLERY_MIN_WIDTH_PX, GALLERY_MAX_IMAGES_PER_TOUR). RN-013 actualizada de "propuesta a validar" a "implementado". Docs 05, 09, 17 sincronizados |
| 2.4 | 2026-07-09 | **Fase 1 — 6to bloque cerrado**: BE-11 RN-045 documentos KYB obligatorios con feature flag `KYB_REQUIRE_MANDATORY_DOCS` (default OFF). RN-045 actualizada de pendiente a implementado con detalle de activación por config y nota sobre providers históricos. Docs 05, 09, 17 sincronizados |
| 2.5 | 2026-07-09 | **Fase 1 — 7mo bloque cerrado**: BE-01/02 reactivar `tour.porcentaje_tourya` como default heredable en creación de slots. RN-015 actualizada de "aprobado — a implementar" a "implementado" con detalle del rename descartado. Doc 08 modelo de datos: nota sobre "agregar campo" cambia a "reactivado 2026-07-09". Docs 05, 08, 17 sincronizados |
| 2.6 | 2026-07-09 | **Fase 1 — 8vo bloque cerrado**: SEC-09 rate limiting en `/auth/**` con feature flag `AUTH_RATE_LIMIT_ENABLED` (default OFF). H-3 marcado como resuelto en doc 12. Limitación de coordinación entre réplicas documentada. Docs 09, 12, 17 sincronizados |
| 2.7 | 2026-07-09 | **Fase 1 — 9no bloque cerrado**: SEC-10 account lockout con backoff exponencial y feature flag `AUTH_LOCKOUT_ENABLED` (default OFF). Complementa SEC-09 con bloqueo por cuenta. H-4 marcado como resuelto en doc 12. Docs 09, 12, 17 sincronizados |
