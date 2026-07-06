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
| 16 Agentes IA | 📌 Pendiente Luis | Esqueleto inicial — Luis aporta contenido detallado |

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

## Changelog

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 2026-06-27 | Generación inicial completa basada en código existente |
| 1.1 | 2026-06-28 | Consolidación con aportes de Luis: visión/mercado (San Andrés), metas 12m, políticas de cancelación, refresh tokens por rol, refactor `Tour.percentageTourya` e `isUnlimitedCapacity`, moderación IA, webhook Wompi, KYB docs obligatorios, eliminación `TourReservation` legacy |
| 1.2 | 2026-06-28 | Nuevo doc `14-gap-web-mobile.md` — análisis funcional web vs mobile y propuesta de matizar la meta de iso-funcionalidad |
| 1.3 | 2026-07-06 | Nuevo doc `15-mvp-mobile-estado.md` — gap analysis del código MAUI actual contra la matriz del doc 14 + roadmap por ciclos |
| 1.4 | 2026-07-06 | Doc `14-gap-web-mobile.md` reescrito v2.0 con el alcance definido por Luis (operadores en la calle → mobile cubre crear tour, schedule y operarios). Doc `15-mvp-mobile-estado.md` actualizado en consecuencia. Doc `16-agentes-ia.md` esqueleto (Luis creó el placeholder, pendiente el contenido detallado) |
