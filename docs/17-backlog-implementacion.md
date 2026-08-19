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
| BE-03 | 🟢 Eliminar `isUnlimitedCapacity` de `tour_schedule` (drop column) | P1 | XS | — | RN-021, **migración 029 (2026-04-08)**. Descubrimiento 2026-07-10: el trabajo ya estaba hecho hace 3 meses, el backlog quedó desactualizado. Ver auditoría en doc 00 |
| BE-04 | 🟢 Ajustar `TourScheduleConfigGeneralService` para no leer/escribir el campo eliminado | P1 | S | BE-03 | RN-021, **ya hecho hace tiempo**. Verificación 2026-07-10: `TourScheduleConfigGeneralService:523` tiene solo un comentario del método antiguo; ningún service del backend lee `schedule.isUnlimitedCapacity` (todos usan `tour.isUnlimitedCapacity`, que sigue siendo la fuente de verdad correcta) |
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
| BE-18 | 🟢 Correo automático "crédito por expirar" (30 días y 7 días antes) | P2 | S | — | RN-036, PR #173. Job `CreditExpirationJob` cron 5am Bogotá, idempotencia por timestamps (`reminder_30d_sent_at`, `reminder_7d_sent_at`). 7 tests JUnit con Mockito |
| BE-19 | 🟢 Correo automático "crédito expirado" | P2 | XS | BE-18 | RN-036, PR #173. Nuevo valor `EXPIRED` en `CreditStatusEnum` + migración 074 con backfill (marca EXPIRED los ya vencidos sin reenviar correo). `expired_notified_at` timestamp para no reenviar |
| BE-20 | 🟢 Verificar y completar flujo de reagendamiento (3 casos: igual/menor/mayor precio) | P1 | S | — | RN-033, PR #170. Auditoría 2026-07-11: los 3 casos ya estaban sólidos (LOWER/EQUAL en `handleRescheduleEqualOrLower`, HIGHER en `handleRescheduleHigher`, con `CREDIT_EXPIRATION_MONTHS` de `app_config` + `@Transactional` + recálculo de availability). Fixes: guard `newDate >= today` + renumeración de comentario. Primera suite JUnit real del proyecto: 14 tests con Mockito (`ReservationServiceRescheduleTest`). Deuda futura BE-20b: test integrado con `@SpringBootTest` + Testcontainers para verificar los 3 flows end-to-end |
| BE-21 🟢 | Asignar tours a `PROVIDER_OPERATOR` — CERRADA 2026-07-18 (auditoría). Ya implementado por Donnaly en migración `044_provider_users_roles_and_tour_assignment.sql` (~feb/2026): tabla `provider_user_tour`, endpoints `POST/PUT /provider/users` con `tourIds[]`. Datos vivos en dev: 5 operadores, 11 asignaciones. Angular y mobile ya consumen | P1 | M | — | Doc 05 (Luis 17-jul), Doc 03 |
| BE-22 🟢 | Contacto principal por tour — backend CERRADO 2026-07-18. Endpoint `PUT /provider/users/{providerUserId}/principal-tour?tourId=X` con `clearPrincipalForTour(tourId)` antes del save garantiza "0 o 1 principal por tour". Servicio `TourPrincipalOperatorService.resolveForTourId()` resuelve con fallback al provider. En dev: 4 tours con principal marcado | P1 | S | BE-21 | Doc 05 (Luis 17-jul) |
| BE-22b 🟢 | UX del selector "Contacto principal" desde el card del tour — CERRADA 2026-07-19 (tourya-front PR #67). Nuevo `TourPrincipalOperatorModalComponent` (bootstrap modal). Link "Contacto principal" en el card del `tour-list`. Radio button por operador + badge "Actual" + case sin operadores con link a `/providers/users`. Guarda vía `PUT /provider/users/{providerUserId}/principal-tour?tourId=X`. i18n completa del `tour-list` (deuda vieja de labels hardcoded resuelta de paso) en es/en/pt. Mobile: paridad opcional pendiente | P1 | S | BE-22, BE-22c | Doc 05 (Luis 18-jul), P1 confirmada Luis 19-jul |
| BE-22c 🟢 | Endpoint `GET /provider/users/tour/{tourId}` — CERRADA 2026-07-19 (tourya-api PR #185). Nuevo query `ProviderUserTourRepository.findByTourIdWithProviderUserAndUser` con `JOIN FETCH`, orden `isPrincipal DESC, id ASC`. `ProviderUserService.listOperatorsByTour` con validación de ownership. Devuelve `List<ProviderOperatorResponse>` (reusa DTO) | P1 | XS | BE-21 | Derivada BE-22b |
| BE-22d 🟢 | Campo `phone` en `PROVIDER_OPERATOR` — CERRADA 2026-07-19 (tourya-api PR #185 + tourya-front PR #67). Migración `077_user_phone.sql` aplicada a Cloud SQL dev. `User.phone` nullable. Create/Update requests + Response backend. Angular: FormControl phone en create + edit modales de `provider-users`. `TourPrincipalOperatorService.fromOperatorLink()` con fallback retrocompatible `user.phone ?? provider.phone` (impacta `TourOperatorResponse` en reservas — RN-026). Mobile: paridad opcional pendiente | P1 | S | BE-21 | Doc 05 (Luis 19-jul WhatsApp) |
| BE-22d-mobile | Paridad mobile MAUI del campo phone — agregar `Phone` a `OperatorModels.cs` (Create + Update + Response) + `OperatorFormViewModel` + `<Entry>` en `OperatorFormPage.xaml`. Talla XS. No bloqueante — sin phone, el mobile hoy sigue funcionando sin ese campo | P2 | XS | BE-22d | Paridad opcional |
| BE-23 🟢 | Job DIMAR bandera roja → cancelación retroactiva + créditos — CERRADA 2026-07-19 (PR #184). Nuevo `services.maritime.events` con `MaritimeAlertCreatedEvent` + `MaritimeAlertEventListener` (@Async @AfterCommit). `MaritimActivityReportService.create()` publica el evento si flag=RED. `ReservationService.cancelAffectedByRedAlert()` busca reservas por subcategory + location + rango de fechas, cancela con reason RAIN, crea Credit por cada una, publica push. Idempotente. Nuevo `PushDomainEvent.ReservationCanceledByRain`. **Deuda BE-23b**: test integrado con Testcontainers para verificar cadena end-to-end | P1 | S | MO-40b | Doc 05 (Luis 17-jul), RN-054 |
| BE-24 🟢 | **Provider Decline Fase 1 — CERRADA 2026-07-23 (tourya-api PR #197)**. Endpoint `PUT /reservations/{id}/decline` con guards (ownership provider dueño, estado terminal, ya declinada). Al declinar: `cancellationReason = PROVIDER_DECLINED` + `providerDeclinedAt` timestamp + `recalculate` del slot (patrón BE-27) + anula `AccountPayable` (fix colateral BE-23 que también hacía double-payment en `cancelReservationByRain`) + crea `Credit` reusando BE-23 + envía email con `provider_declined_notification.html` que lista tours alternativos por `subCategory` ordenados por rating. **Deuda BE-24b**: test integrado con Testcontainers (30+ mocks del ReservationService lo hacen impráctico como unit — mismo caso que BE-20b y BE-23b, se cierran juntos). Fase 1 no incluye UI para llamar el endpoint — botón "No puedo atender esta reserva" cerrado en **FE-24** (Angular, tourya-front PR #77, 2026-07-23) + **FE-24-mobile** pendiente (paridad MAUI, talla XS, no bloqueante). **Fase 2 (roadmap, sin timeline)**: sistema de penalizaciones por ventana temporal (48h/24-48h/<24h/no-show) con multas económicas, descenso en ranking de búsquedas (7-15 días), cupones al turista (5%-20%), suspensión/expulsión por reincidencia — talla XL | P1 | M | — | RN-055 (doc 05) + issue #193 |
| BE-25 🟢 | Cross-provider view del BACKOFFICE — backend CERRADO 2026-07-18. `Utils.isTouryaBackoffice()` incluye ADMIN + BACKOFFICE_OPERATION. SP `sp_get_provider_reservations` (migración 061) tiene guard `(p_provider_id IS NULL OR t.provider_id = p_provider_id)`. `ReservationService.getProviderReservations` línea 957-958 pasa `finalProviderId = requestedProviderId` (null si no viene en query) para backoffice → SP no filtra → ve todas | P2 | XS | — | Doc 03 (Luis 17-jul, P2 confirmada 18-jul) |
| FE-15 🟢 | Angular reconocer rol BACKOFFICE_OPERATION — andamio CERRADO 2026-07-19 (tourya-front PR #66). `Roles.BACKOFFICE_OPERATION = 35` en enum + `AuthService.isBackofficeOperation()` + `AuthService.isTouryaBackoffice()`. Aditivo, sin reemplazar llamadas a `isAdmin()` | P1 | S | BE-25 | Doc 03 (Luis 17-jul) |
| FE-15b 🟢 | Subset BACKOFFICE_OPERATION habilitado — CERRADA 2026-07-21 (tourya-api PR #186 + tourya-front PR #69). **Backend**: `ProviderPayoutOrderService` (3 métodos) usa `Utils.isTouryaBackoffice()`. Cerrado **H2** (agujero preexistente): `MaritimActivityReport.create/update/delete` requieren guard `Utils.isTouryaBackoffice()` (antes cualquier autenticado podía crear/borrar). **Frontend**: nuevo `BackofficeGuard` + `admin-routing` con guards por ruta (dashboard/tour-admin = AdminGuard, bookings-management = BackofficeGuard). 5 componentes migrados a `isTouryaBackoffice()`. `home-redirect` para BACKOFFICE → `/admin/bookings-management` | P1 | S | FE-15 | Doc 03 + P2 confirmada Luis 19-jul (opción C) |
| FE-15c 🟢 | UI backoffice para reportes DIMAR expuesta — CERRADA 2026-07-21 (tourya-front PR #70). Descubierto que la UI YA existía (`maritime-activity-reports.component.ts`) pero embebida en dashboard admin bajo `AdminGuard` → BACKOFFICE no la veía. Trabajo real: nueva ruta `/admin/maritime-reports` con `BackofficeGuard` (lazy `loadComponent`, componente ya standalone), enlace desde `bookings-management`, **modal de confirmación explícita** cuando `flag=RED` (evita click descuidado que dispara BE-23 = cancelación retroactiva de reservas + créditos), i18n es/en/pt. Sin cambios en lógica del componente ni regresión en el embed del dashboard. Talla real XS (audit-before-propose evitó estimarlo como S) | P2 | XS | FE-15b | Derivada FE-15b |
| BE-26 🟢 | **CERRADO 2026-07-24 (auditoría no-op)**. Reportado por Luis 19-jul: reservas con tour `GRUPO` fallan tras pago Wompi con mensaje `"Error en la reserva..."`. Auditoría: el mensaje no es específico de GRUPO — es el catch-all del `handlePaymentResult` en `cart-summary.component.ts:912` para cualquier error tras Wompi APPROVED. Logs Cloud Run dev últimos 14d: los 7 errores 500 en `POST /payment` fueron entre 15-17 jul (todos causados por #179 / migración 075 device_token_fcm faltante). Desde 17-jul (fix MO-40b + HOTFIX 075) cero 500. Luis reportó el bug 2 días después de que ya estaba arreglado. Cero errores del `PaymentService` en 7d. Lógica GRUPO en `ReservationService.java:1945/2012/2485` sin fallos en producción. **Resuelto implícitamente por MO-40b** | P1 | — | — | Doc 05 §RN-026 (Luis 19-jul), auditoría logs 24-jul |
| BE-30..38 | **Códigos de descuento partners B2B (doc 18)** — feature grande talla L. Ver [doc 18](18-codigos-descuento-partners.md) para modelo completo. Sugerencia de 3 fases secuenciales: (BE-30) CRUD backoffice de `Partner` + `DiscountCode` + validación en cart sin efecto en pago (dry-run del descuento) — talla M; (BE-31) integración con `PaymentService.processPayment` + `DiscountCodeRedemption` + RN-B03 tope de comisión + RN-B05 combinación con créditos + RN-B07 cancelaciones — talla M **riesgo alto** (toca la ruta que rompió MO-40, requiere `AFTER_COMMIT` + Testcontainers + feature flag); (BE-32) `PartnerPayoutOrderJob` mensual + attachment + reportería backoffice — talla S. Frontend: FE-16 backoffice partners/códigos + FE-17 input código en cart/checkout. Preguntas técnicas pendientes de decisión antes de arrancar BE-31: momento de registrar `Redemption` (pre/post pago), revalidación del código en checkout, modelo del array `applicableTourIds` (tabla intermedia recomendada) | P1 | L | MO-40b | Doc 18 (Luis 19-jul) |
| TC-009 🟢 | `document_type` en `tourist_profile` — CERRADO 2026-08-06 (migración 083 aplicada a Cloud SQL dev). Antes vivía solo en `payment.payer_document_type`, se re-preguntaba en cada compra. Ahora se persiste en el perfil para pre-llenar futuras compras (varchar 50 nullable, valores `CC/CE/PP/NIT/TI`). Frontend consumido en TC-020 #235 (tourya-front PR #105) para mandar el `docType` real a Wompi en vez del hardcode `"CC"`. **Portado a mobile 2026-08-13 (Sprint 2)** — Picker CC/CE/PA/NIT en `ProfilePage` + `CheckoutViewModel` pre-fill desde profile + `AuthResponse.documentType` + endpoint client `GET/PUT /users/me/profile` en `AuthService` | P1 | XS | — | Issue #196 (Luis) |
| TC-011 🟢 | Job `NO_SHOW` — CERRADO 2026-08-07 (PRs #217, #221, #222). Reabierto 2 veces tras el cierre inicial. **Iteraciones**: (a) PR #217 incluye reservas `RESCHEDULED` además de `PENDING` en el barrido de `PendingReservationNoShowJob` (7am Bogotá); (b) PR #221 aísla la tx de `recalculate` en `TransactionTemplate REQUIRES_NEW` — antes el `recalculate` reventaba y revertía la marca NO_SHOW quedando reservas atascadas; (c) PR #222 endpoint temporal `POST /reservations/no-show-job/trigger` protegido con `Utils.isTouryaBackoffice()` para dispararlo manualmente sin esperar el cron | P1 | S | — | Issue #206 |
| TC-014 🟢 | Auto-purga de `shopping_cart_item` con `scheduleDate` vencido — CERRADO 2026-08-04 (PR #216). En `GET /shopping-cart` cada vez que el turista abre el carrito, los items cuyo `schedule_date < today (Bogotá)` se eliminan silenciosamente antes de responder. Evita que un item quede huérfano cuando el turista dejó el carrito abierto por días. **Portado a mobile 2026-08-13 (Sprint 2)** — detección client-side + alert al turista en `CartViewModel` (diff pre/post-refresh) + banner en `CartPage.xaml` | P1 | XS | — | Issue #215 |
| TC-015 🟢 | Exponer `porcentajeTourya` en `TourFullDataResponse` — CERRADO 2026-08-07 (PR #223). Complemento de BE-01/FE-05 para que el backoffice vea el valor actual del tour en la vista de detalle (no solo en el modal de aprobación). Frontend TC-015 en tourya-front PR #97 (UI editar post-aceptación) + PR #103 (refinamiento lista admin, quitar `hero-image` del detalle) | P1 | XS | BE-01 | Issue #218 |
| TC-016 🟢 | Datos completos en detalle de reserva — CERRADO 2026-08-07. **Parte A** (PR #224): `countryName/stateName/cityName` en `TourAddressResponse` para render de meeting point en 4 vistas (frontend PR #98). **Parte B** (PR #225): `travelerBreakdown` (array por `ageType` con `quantity`) expuesto en `sp_get_provider_reservations` — el detalle vivía en `shopping_cart_item_detail` pero requería llamadas extra; ahora viene en el mismo response (frontend PRs #99, #102 renderizan en modal reserva + floating cart + 4 vistas). **Portado a mobile 2026-08-13 (Sprint 2)** — nuevo `TravelerBreakdownDto` + `TravelerBreakdownConverter` con pluralización via `I18nService` + chip "2 Adultos · 1 Niño" en `ProviderReservationsPage` + desglose en ambos `ReservationDetailPage` | P1 | S | — | Issue #219 |
| TC-017 🟡 | Hotel Pickup — **cerrado backend + FE (mapa/priceType/UX list-tours+details + cierre selector orden), esperando validación final Luis PR #114 (2026-08-13)**. Backend: (a) PR #226 agrega valor `HOTEL_PICKUP` a `AddressTypeEnum` + relaja `country/state/city` en Java; (b) PR #228 guard null en lookups + enum matcher tolerante (el enum guarda `"Hotel Pickup"` display value, no la key); (c) PR #242 **migración 087** hace NULLABLE `country_id/state_id/city_id` en `tour_address` + CHECK constraint `tour_address_geo_required_unless_hotel_pickup` (solo `address_type='Hotel Pickup'` admite geo NULL). **Bug de proceso**: PRs #226/#228 se mergearon sin la migración → fallaba con `null value violates not-null constraint`; #242 la puso 2 días después. Frontend: PR #100 (flag HOTEL_PICKUP en editor + mensaje en detalle y modal reserva) + PR #107 (**opción B**: siempre guardar campos i18n del tour en slot `es` regardless del idioma UI — evita perder el nombre en español si el PROVIDER creaba el tour con UI en inglés) + **PR #111** (mensaje Hotel Pickup extendido a 4 vistas del turista: `tours-detail`, `tour-booking-confirmation`, `list-tours` grid+list, `cart-summary` via i18n `tour.hotelPickup.{longMessage,shortMessage}` ES/EN/PT; bonus colateral removidos fallbacks fake en `cart.service.ts` — Cartagena/Bolívar/Colombia/Plaza de la Aduana — que enmascaraban tours Hotel Pickup) + **PR #112** (fix mapa Hotel Pickup: getter `isHotelPickup` + `*ngIf="!isHotelPickup"` en `tours-detail` — el guard previo `*ngIf="mapCenter"` no bastaba porque backend envía coords por defecto de la ciudad; i18n priceType: normalización case-insensitive en `getPriceTypeLabel()` porque backend envía minúsculas `individual/grupo` no `INDIVIDUAL/GROUP` + nuevas keys `tour.priceType.{individual,group}` en ES/EN/PT + formato `"{label} (N)"` con N=`tour.maxPeople`; bonus: `tour-grid-view.component` usaba `maxCapacity` inexistente en `TourDto` → reemplazado por `maxPeople`) + **PR #113** (refinamientos UX: selector "Ordenar por" en `list-tours` con 3 opciones exclusivas Recomendado / Precio menor→mayor / Precio mayor→menor — sort client-side sobre `tour.priceFrom`, "Recomendado" restaura orden backend; eliminados toggles grid/list, default queda grid; botón "Volver" arriba del breadcrumb en `tour-details` con `location.back()` + fallback a `routes.listTours` cuando `window.history.length <= 1`; nuevas keys i18n `toursList.priceLowToHigh`, `toursList.priceHighToLow`, `common.back`) + **PR #114** (cierre selector orden tras feedback Luis 13-ago 12:07: eliminada opción "Recomendado" del dropdown; default = vacío — cuando no se elige nada la lista respeta el orden del backend; solo quedan 2 radios "Precio menor→mayor" / "Precio mayor→menor"; tipo `sortOption` pasa de `'recommended'\|'price_asc'\|'price_desc'` a `''\|'price_asc'\|'price_desc'`; 2 archivos `list-tours.component.{ts,html}` +10/-15). **Estado**: esperando validación final de Luis del PR #114 (cierra ciclo TC-017 tras 8 iteraciones acumuladas). **Display portado a mobile 2026-08-13 (Sprint 2)** — Hotel Pickup en 4 vistas (`TourDetailPage`, `ExplorePage`, `CartPage`, `ReservationDetailPage`) + i18n `priceType` + nueva API `I18nService.T(key)` con tablas es/en/pt. La **creación** con `addressType=HOTEL_PICKUP` en `TourFormPage` mobile queda como **deuda P6 aprobada Luis 2026-08-13** para futuro ciclo | P1 | S | — | Issue #220 |
| TC-018 🟢 | Reportes DIMAR con flag RED bloquean add-to-cart + cierre del hook — CERRADO 2026-08-12 (Luis cerró issue #227 04:33) tras **5 iteraciones** dolorosas. **Bug A** PR #229: dispara el hook en `update` (no solo `create`) + incluye `RESCHEDULED` + fail-loud si el listener falla. **Bug B** PR #230 + **migración 085**: expone `blockedByMaritimeReport` en `sp_get_tour_schedule_json` (`EXISTS` contra `maritim_activity_report` con flag=RED y rango de fechas) + guard duro en `ShoppingCartService.addItemToCart` que rechaza si el frontend logra bypassear. **Reabrió**: hook seguía sin ejecutar. **Iteración 3** PR #234 (`diag`): endpoint admin `POST /maritime-activity-reports/{id}/trigger-alert` + logs INFO. **Iteración 4** PR #237: listener síncrono + instrumentación completa. **Iteración 5** PR #239 (fix final): **direct call** a `ReservationService.cancelAffectedByRedAlert` desde el service — bypass del listener asíncrono que nunca disparó por interacciones con `@Transactional` + `@Async`. Frontend PR #101 (modal deshabilita días bajo alerta) + PR #108 (rediseño columnas listado + botones por flag + fix precarga subcategoría). Lección: eventos `AFTER_COMMIT + @Async` son frágiles cuando el flujo también es asíncrono — a veces el direct call gana. **Portado a mobile 2026-08-13 (Sprint 2)** — `blockedByMaritimeReport` en `TourDetailResponse` mobile + guard 400 backend respetado + banner rojo en `TourDetailPage` bloqueando add-to-cart | P1 | M | BE-23 | Issue #227 |
| TC-019 🟡 backend | `sub_category` en templates + regresiones de precio + booking count reagendamiento + slotPct editable — **backend CERRADO 2026-08-12; pendiente validación UI Luis**. **Feature** (PR #233 + **migración 086**): columna `tour_schedule_config.sub_category` (`tour_subcategory_enum` nullable) + `get_templates_by_provider` recibe `p_sub_category` opcional y filtra + expone `subCategory` en el JSON de salida. Frontend PR #104: dropdown filtra templates por subcat del tour + campo Subcategoría en form template. **Hotfix cast** PR #238: `@ColumnTransformer(write = "?::tour_subcategory_enum")` porque JPA no castea el enum PG automáticamente. **Regresión precio batch** PR #232: `POST /tour-schedules/batch` respondía 404 "Slot not found" tras cambios de BE-27. **Regresión mayor** PR #243 + **migración 088** (3 bugs en cadena): (R1) `slot_porcentaje_tourya=0` en muchos slots legacy → BE-02 solo heredaba en slots NUEVOS, backfill de 444 slots al margen del tour; (R2) reagendamiento mostraba `price=providerPrice` porque `SearchTourScheduleSlotPercentageEnricher` ignoraba `slot.slot_porcentaje_tourya`; (R3) disponibilidad `bookings=0` porque migración 085 (TC-018b) **reescribió `sp_get_tour_schedule_json` usando `sl.bookings` en vez del helper `fn_slot_booked_units_on_schedule` introducido en TC-004 (mig 078)** → regresión accidental al copiar el SP. Backfill: 444 slots + 783 prices + restore del SP. **Bug 3** PR #246 + **migración 089**: tras un reagendamiento la UI mostraba drift en ambos lados (día viejo con `bookings` inflado, día nuevo con `bookings=0`). Causa: helper `fn_slot_booked_units_on_schedule` (mig 078) excluía `RESCHEDULED` del `IN` de delivery_status. Fix: helper recreado incluyendo `RESCHEDULED` + backfill idempotente 792 slot.bookings + 404 slot.availability; `ReservationRepository.countActiveBookingUnitsForSlotOnDate` alineado. **Nueva RN-061**. **Bug 4** PR #247 + **migración 090**: al asignar valor a `slotPorcentajeTourya` en un slot, se guardaba pero `price` base no se recalculaba. Causa: `TourScheduleConfigSlotDto` NO tenía el campo. Fix: campo agregado (0-100, nullable), `manageSlotsUpdate` lo honra cuando el rol es BACKOFFICE (RN-015 refinamiento), migración 090 backfill 4 filas drift. Endpoint per-schedule `PUT /percentage/{slotId}` no se tocó (ya funcionaba). Lección repetida: al reescribir SPs auditar los fixes previos que preservar. **Pendiente**: validación UI de Luis (bug 4: probar POST/PUT config y ver que `price` refleje el nuevo pct) antes de que Luis marque el issue como CERRADO — advertencia del agente. **Portado parcial a mobile 2026-08-13 (Sprint 2)** — multi-ageType + `providerPrice` (margen) en `ScheduleTemplateFormViewModel` (antes hardcoded `AgeType = "ADULT"` línea 233). Batch schedule create UI mobile (MO-DT19c) queda pendiente respuesta Luis (P2) | P1 | M | TC-004, TC-018 | Issue #231 |
| TC-020 🟢 | Zona horaria America/Bogota — CERRADO 2026-08-12 (Luis cerró issue #235 12:44) en 2 pasadas. **Fix parcial** PR #240: `LocalDateTime.now()` en `ReservationService` (fechas de reserva post-pago) pasa a `LocalDateTime.now(BOGOTA)`. Frontend PR #105: manda `docType` real (aprovecha TC-009) a Wompi + botón "Ver Reservas" en confirmación + refresh profile. **Fix real** PR #244: forzar TZ Bogota en JVM (`Dockerfile` instala `tzdata` + `ENV TZ=America/Bogota` + `ENTRYPOINT -Duser.timezone=America/Bogota`) + `JpaAuditingConfig` con `DateTimeProvider` que retorna `LocalDateTime.now(BOGOTA)` para columnas `@CreatedDate/@LastModifiedDate`. Antes Cloud Run corría el container en UTC → cualquier `LocalDateTime.now()` "bare" (~28 sitios en services + jobs) resolvía a UTC. **Nueva RN-060** en doc 05. Ver también doc 13 §Dockerfile. **Portado a mobile 2026-08-13 (Sprint 2)** — `docType` real (Picker CC/CE/PA/NIT en `CheckoutPage.xaml` reemplazando el hardcode `"CC"`) mandado a Wompi + botón "Ver mis reservas" en `PaymentConfirmationPage` + refresh de profile en `ProfileViewModel.OnAppearing` post-tx | P1 | S | — | Issue #235 bug c |
| TC-021 🟢 | Scrub datos cliente al PROVIDER hasta 1 día antes — CERRADO 2026-08-10 (PR #241). En `sp_get_provider_reservations` (y responses derivados), cuando el rol es PROVIDER puro y falta más de 1 día para el tour (`reservation_date - today > 1`), los campos `payerName/payerEmail/payerPhone/payerDocumentNumber/serviceResponsibleName/Email/Phone` se devuelven ofuscados o vacíos. A partir del día anterior al tour, el PROVIDER ve los datos completos para coordinar con el turista. ADMIN y BACKOFFICE_OPERATION siempre ven todo. Frontend PR #106 hace mirror en UI. **Nueva RN-058** en doc 05. **Portado a mobile 2026-08-13 (Sprint 2)** — placeholder "🔒 Datos disponibles el día del tour" en `ProviderReservationsPage.xaml` + `ReservationDetailPage` | P1 | S | — | Issue #236 |
| TC-022 🟢 | Devolución en efectivo de crédito — CERRADO 2026-08-13 en los **3 frentes** en un día + emails asincrónicos al turista el mismo día. **Backend** (PR #254 + **migración 091**): 2 estados nuevos en `CreditStatusEnum` (`REFUND_REQUESTED`, `REFUNDED`) + 3 columnas nuevas en `credit` (`refund_requested_at`, `refunded_at`, `refund_proof_url`) + 3 endpoints (`POST /credits/{id}/request-refund` turista, `POST /admin/credits/{id}/upload-refund-proof` ADMIN/BACKOFFICE multipart, `GET /admin/credits` paginado global con `touristName/touristEmail` embebidos). Uso de `IStorageService` (S3 o GCS) al path `credit-refund-proofs/{creditId}/...`. Nuevo `AdminCreditController` separado. Migración 091 aplicada a Cloud SQL dev. **Bonus fix**: la mig 091 normaliza el CHECK constraint `credit_status_check` con el catálogo completo (`CREATED, RESERVED, CONSUMED, CANCELED, DELETED, EXPIRED, REFUND_REQUESTED, REFUNDED`) — cierra la deuda latente donde la mig 074 (BE-19) empezó a usar `'EXPIRED'` sin actualizar el CHECK. **Frontend web** (tourya-front PR #115): turista con botón "Solicitar devolución" + SweetAlert2 confirm en `/clients/my-profile?section=credits`. Nueva ruta `/admin/credits` con `AdminCreditsComponent` (tabla paginada + modal upload). Item "Créditos" al sidebar admin. i18n ES/EN/PT. **Mobile MAUI** (commit local `2d770f8` mergeado a develop): turista con botón + confirm en `CreditsPage`. Chip visual REFUND_REQUESTED (amarillo) y REFUNDED (azul) + link "Ver comprobante". Flujo admin queda web-only (coherente con doc 14). **Emails asincrónicos** (PR #257, mismo día): nuevo package `services.credit.events` con `CreditRefundEvent` (sealed + 2 records `RefundRequested`/`RefundCompleted`) publicado desde `CreditService.requestRefund` y `uploadRefundProof`; `CreditRefundEventListener` con `@Async @TransactionalEventListener(AFTER_COMMIT)` — mismo patrón que MO-40b (el email jamás se manda si la tx hace rollback, y un fallo de SMTP nunca aborta la actualización de estado). 2 templates Thymeleaf nuevos: `credit_refund_requested.html` (azul "Solicitud recibida") y `credit_refund_completed.html` (verde con CTA "Ver comprobante" enlazado a `refund_proof_url`). Solo ES. **Nueva RN-062** en doc 05. Deudas nuevas TC-022b (columna `reason` en `Credit` para que el email mencione motivo original) y TC-022c (multi-idioma en emails ES/EN/PT — impacta también BE-18/19 y otros correos) registradas abajo | P1 | M | — | Issue #253 |
| TC-022b | Agregar columna `reason` a `Credit` entity para que los emails de refund mencionen el motivo original del crédito. Hoy los templates de PR #257 solo muestran "devolución de tu crédito" porque `Credit` sólo tiene `type` (CANCELATION/RESCHEDULE/BONUS) y no un free-text. Requiere migración BD + backfill NULL + actualización de los publishers y templates para pasar el `reason` al event snapshot y renderizarlo | P2 | S | TC-022 | Doc 15 §Post-Sprint 2 |
| TC-022c | Multi-idioma en emails transaccionales del backend (hoy solo ES). Los 2 templates de PR #257 y los de BE-18/19 son solo español. Migrar a ES/EN/PT es ítem transversal — impacta todos los correos que hoy asumen ES en el `MailSender`. Fuera de scope de TC-022; se hace cuando se decida i18n de comunicaciones | P3 | M | — | Doc 15 §Post-Sprint 2 |
| BE-EXPOSE-018 🟢 | Exponer `blockedByMaritimeReport` en `TourFullDataResponse` — CERRADO 2026-08-13 (tourya-api PR #255). Cierra MO-DT18 lado backend: mobile ya puede deshabilitar el CTA "Reservar" desde el detail sin depender del 400 defensivo del carrito. Semántica idéntica al helper `MaritimActivityReportRepository.findActiveRedReportsForSubcategoryAndLocation` — `true` si hay MaritimActivityReport RED activo hoy (`America/Bogota`) matcheando la subcategoría y alguna location con geo del tour; Hotel Pickup y locations sin geo se ignoran (DIMAR no las cubre) → `false`. Aplica a todos los endpoints del DTO. Cero migración. Doc 09 sincronizado en PR #258 | P1 | XS | TC-018 | Doc 15 §Deudas MO-DT18 |
| BE-EXPOSE-017 🟢 | Exponer `addressType` en `SearchTourScheduleFullResponse.AddressResponse` y `ShoppingCartItemResponse` — CERRADO 2026-08-13 (tourya-api PR #255). Cierra MO-DT17 lado backend: search/list-tours y cart identifican tours Hotel Pickup y renderizan chip/mensaje sin round-trip al detail. Nuevo enricher batch `SearchTourAddressTypeEnricher` — un solo query IN-clause post-search para todas las direcciones (sin N+1). Cero migración. Doc 09 sincronizado en PR #258 | P1 | XS | TC-017 | Doc 15 §Deudas MO-DT17 |
| BE-EXPOSE-016 🟢 | Exponer `travelerBreakdown` en `ReservationResponse` (detalle turista) — CERRADO 2026-08-13 (tourya-api PR #255). Cierra la parte turista del MO-DT16 backend: el detalle de reserva del turista ahora incluye `List<TravelerBreakdownDto>` por `ageType` (equivale al que TC-016 parte B ya expuso vía `sp_get_provider_reservations` para el provider). Cero migración. Doc 09 sincronizado en PR #258 | P1 | XS | TC-016 | Doc 15 §Deudas MO-DT16 |

**Subtotal backend**: ~3 días de agente + revisiones + testing en staging = **~5–7 días calendario**.

---

### C. Frontend Web (Angular)

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| FE-01 | 🟢 Auto-refresh transparente en `AuthInterceptor` con mutex — `accessToken`+`refreshToken` en localStorage (opción B), NO cookie HttpOnly. `logout()` server-side. | P1 | S | BE-13 | RN-005, PR tourya-front #60. HttpOnly queda como FE-01b futuro (endurecimiento). Ver decisión en doc 00 (2026-07-10) |
| FE-02 | Migrar login social: quitar Firebase SDK, integrar `@abacritt/angularx-social-login` u equivalente compatible Angular 19 | P0 | M | SEC-06 | — |
| FE-03 | 🟢 Validación pre-upload en el uploader de galería (formato/tamaño/orientación/ancho mínimo/cuenta total), umbrales leídos de `app_config` | P1 | S | RN-013 revisada, BE-10 | RN-013, PR tourya-front #59. Backend sigue como defensa en profundidad; frontend evita el round-trip y muestra errores del backend en el mismo Swal |
| FE-04 | 🟢 UI para ADMIN de `app_config` (holdMinutes, buffer payout, expiración créditos, gallery, KYB flag, rate limit y lockout) | P1 | S | BE-09 | RN-022, PR tourya-front #58. 12 configs en 5 grupos (Reservas, Payouts, Galería, KYB, Auth). `CANCELLATION_POLICY` queda fuera del v1 (requiere editor JSON dedicado) |
| FE-05 | 🟢 UI backoffice: gestionar `Tour.porcentajeTourya` en modal de aprobación de tour | P1 | S | BE-01 | RN-015, PR tourya-front #63 (+ PR tourya-api #169 backend). Input pre-poblado con el valor actual del tour (o 15% default); si el ADMIN cambia el valor se llama PATCH `/tours/admin/{id}/porcentajeTourya` antes del PUT accept, si no cambia solo accept. Edición post-aprobación queda como FE-05b futuro |
| FE-06 | 🟢 Agregar UI para las 2 nuevas razones de cancelación en cliente (Angular) | P1 | XS | BE-05 | RN-030, PR tourya-front #56. Aprovechó para cerrar bug preexistente: `INABILITY_TO_TRAVEL` faltaba en `formatCancellationReason` |
| FE-07 | 🟢 Quitar campo `isUnlimitedCapacity` en config de schedule (mover a formulario de tour) | P1 | XS | BE-03 | RN-021, **ya hecho**. Verificación 2026-07-10: `tour-schedule.component.ts:144` tiene un getter `isUnlimitedCapacity()` que lee de `this.tour?.isUnlimitedCapacity` (no del schedule); el HTML usa ese getter solo para readonly/placeholder. No hay input del campo en el schedule, ya está en el form del tour |
| FE-08 | Dashboard financiero del backoffice: GMV, net revenue, cuentas por pagar, CAC | P2 | L | — | Doc 01 roadmap |
| FE-09 | Dashboard operativo del backoffice: tasa conversión/cancelación, tours de alto riesgo | P2 | L | — | Doc 01 roadmap |
| FE-10 | Gestor de disputas | P2 | L | — | Doc 01 roadmap |
| FE-11 🟢 | i18n de `/providers/requestproviders` — CERRADO 2026-07-15. Nueva sección `request-provider` con ~50 claves en `public/i18n/{es,en,pt}.json` + HTML/TS con pipes translate. Sin cambios funcionales. PR pending merge | P2 | S | — | WhatsApp Luis 2026-07-15 |
| TC-002 🟢 | Título galería `Gallery [object Object]` — CERRADO 2026-07-18 (tourya-front PR #65). Preexistente desde jun/2025 (Hector) — nunca se actualizó tras cambio de `tour.name` a `TranslatedField` en dic/2025. Fix: `i18nService.getValue()` + 4 claves i18n × 3 idiomas | P2 | XS | — | Issue #182 (Luis 17-jul) |
| TC-003 🟢 | Modal "Ver reserva" con 6 campos rotos — CERRADO 2026-07-18 (tourya-front PR #65). Preexistente desde may/2026. Root cause: `mapReservationToBooking()` leía nombres del DTO viejo pero el endpoint devuelve `ReservationDetailsResponse` del SP con nombres distintos. Fix solo en el mapper `.ts`; template intacto. Análisis inicial en issue #183 era incorrecto — corregido con 2do comentario | P1 | S | — | Issue #183 (Luis 17-jul) |
| TC-008-fe 🟢 | Sidebar admin persistente + ADMIN mantiene menú completo en todas las vistas admin + header duplicado retirado — CERRADO 2026-08-12 (tourya-front PRs #96, #109, **#110**). Al desplegar FE-15b/c (BackofficeGuard), el sidebar se perdía en algunas vistas y ADMIN veía solo los 3 ítems del subset BACKOFFICE. Fix: sidebar admin persistente en shell + guards mantienen los items visibles según rol (ADMIN ve todo, BACKOFFICE_OPERATION solo 3: reservas, payouts, DIMAR). Refinamiento **PR #110**: eliminado `<h2>Maritime Activity Reports</h2>` hardcodeado en `maritime-activity-reports.component.html` — el shell admin ya renderiza su propio breadcrumb, el header quedaba duplicado cuando se entraba por sub-flujos. **Issue #195 cerrado por Luis 2026-08-12 15:33**. Ver actualización en doc 12 §Roles y layout admin | P1 | S | FE-15b | Issue #195 |
| FE-12 | **Geocoding Google Maps direcciones incompletas**. Reportado por Luis: dirección del tour y del hospedaje en checkout no muestran ubicaciones (ej. "no aparece ningún hotel en San Andrés islas"). Diagnóstico pendiente: (a) API key sin cobertura de Places para CO? (b) restricciones de referrer del key en dev vs prod? (c) `regionCode`/`componentRestrictions` mal configurados? (d) fallback vacío cuando el place no matchea el bias. Talla real depende del diagnóstico | P2 | S | — | Doc 05 (Luis 17-jul) |
| ~~FE-13~~ 🚫 | ~~UI ADMIN reasignar reserva~~ **CANCELADO 2026-07-23** — con el rediseño de RN-055 (Luis 23-jul en issue #193), ya no hay UI de reasignación manual. El decline dispara todo automáticamente. No se requiere UI nueva para el ADMIN. La lista existente de reservas puede opcionalmente mostrar la marca `providerDeclinedAt` para auditoría, pero no es item del backlog | — | — | — | Reemplazado por lógica automática en BE-24 |
| FE-14 🟢 | UI PROVIDER asignar tours a operador — CERRADA 2026-07-18 (auditoría). Ya existe en `provider-users.component`: `FormControl` `tourIds` (checkbox multi-select) + `principalTourId` (dropdown), con `Validators.required` y modal separado para cambiar principal (líneas 266-295 crear, 376-399 editar, 536+ cambiar principal). Sin trabajo pendiente | P1 | S | BE-21, BE-22 | Doc 05 (Luis 17-jul) |

**Subtotal frontend web**: ~3 días de agente + revisiones + prueba visual = **~5–7 días calendario** (los dashboards `FE-08/09/10` no cuentan aquí — van a Fase 4).

---

### D. Mobile (MAUI Android)

Basado en el roadmap de [15 — MVP mobile estado](15-mvp-mobile-estado.md).

#### D.0 — Higiene (bloqueante)

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-00 | **Crear repo GitHub `tourya-mobile`** y subir el código (hoy solo local — riesgo alto) | P0 | XS | — | [15](15-mvp-mobile-estado.md) |
| MO-01 | 🟢 Actualizar URL del backend en `Constants.cs` (hoy apunta a IP AWS legacy `44.203.38.85`) | P0 | XS | — | [15](15-mvp-mobile-estado.md). Cambio 2026-07-13: apunta al LB GCP `http://34.160.22.16/api/v1/` — mismo patrón que el frontend web (todo el tráfico por el LB). Migrar a HTTPS cuando se agregue forwarding rule 443 al LB dev |
| MO-02 | Registrar Syncfusion License en `MauiProgram.cs` | P0 | XS | — | [15](15-mvp-mobile-estado.md) |
| MO-03 | CI/CD: GitHub Actions → build APK firmado → Play Store internal track | P1 | M | MO-00, MO-02 | [15](15-mvp-mobile-estado.md) |

#### D.1 — Brechas del turista

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-10 | UI de crear reseña con cámara (existe `ReviewService`, falta UI) | P1 | S | — | Doc 15 Ciclo 1 |
| MO-11 🟢 | Wishlist page (lista de deseos) en tab del turista — CERRADO 2026-07-15 | P1 | S | — | Doc 15 Ciclo 1 |
| MO-12 🟢 | UI dedicada de créditos: saldo, historial, transferir a otro turista — CERRADO 2026-07-15 | P1 | M | — | Doc 15 Ciclo 1 |
| MO-13 | Integrar login social sin Firebase (Google + Facebook nativos MAUI) | P1 | M | SEC-06 | Doc 15 |

#### D.2 — Brechas del proveedor

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-20 | 🟢 Responder reseñas desde `ProviderReviewsPage` | P1 | S | — | Doc 15 Ciclo 2. Auditoría 2026-07-13: fix del bug latente en `ReviewService.ReplyToReviewAsync` (multipart en vez de JSON). **Sprint 5a cerrado 2026-08-14**: nueva `ProviderReviewsViewModel` + Page con flujo completo de responder reseñas — TextEditor + Enviar/Cancelar + nuevo `StringNotEmptyToBoolConverter` + 7 keys i18n (es/en/pt), reusando `ReviewService.ReplyToReviewAsync` (sin cambios backend ni service). Cierra el gap ❌ del doc 14 "Responder reseñas provider" |
| MO-21 🟢 | `PayoutsPage`: ver payouts + descargar comprobantes — CERRADO 2026-07-15. Lista con 4 totales + filtro por status + detalle con reservas + descarga via Launcher. Sin filtros de fecha (deuda MO-21b) | P1 | M | — | Doc 15 Ciclo 2 |
| MO-22 🟢 | Crear / editar operarios (`PROVIDER_OPERATOR`) — CERRADO 2026-07-15. Sin "eliminar" (el backend no expone DELETE; los desactiva vía otro flujo) | P1 | M | — | Doc 14 alcance Luis |
| MO-23 🟢 | Compartir contraseña temporal por WhatsApp / otra app — CERRADO 2026-07-15 via `Share.Default.RequestAsync` (share sheet nativo, más flexible que deep-link a WhatsApp) | P1 | S | MO-22 | Doc 14 UX |
| MO-24 🟢 | Resetear contraseña de operarios desde app — CERRADO 2026-07-15 | P1 | S | MO-22 | Doc 14 |
| FE-24-mobile | **Paridad MAUI del botón "No puedo atender esta reserva"** — la web ya expone la acción en `provider-tour-management` desde tourya-front PR #77 (2026-07-23). Falta paridad mobile: (a) nuevo método `DeclineReservationAsync` en `ReservationService.cs` que llame `PUT /reservations/{id}/decline`; (b) botón / MenuItem en `ProviderReservationsPage` visible solo si `Status ∈ {Pending, Rescheduled}` (mismo criterio que la web); (c) `DisplayAlert` de confirmación explícita con el impacto completo (cancel + crédito + email + no se deshace) antes de llamar el endpoint; (d) refrescar la lista tras éxito. No bloqueante — el provider puede usar la web mientras tanto. Ver `provider-tour-management.component.ts:openDeclineConfirmModal` como referencia | P2 | XS | BE-24, FE-24 | RN-055 (doc 05) + issue #193 |

#### D.3 — Refinar UX del proveedor (calidad crítica para adopción en San Andrés)

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-30 🟢 | Autosave / borradores en el wizard de creación de tour — **CERRADO 2026-08-14 (Sprint 3 A, mobile local)**. Nuevo `TourDraftService` persiste el estado del wizard como snapshot JSON en `AppDataDirectory/tour_draft.json`. Guarda cada cambio del form, recupera al reabrir si hubo crash o cierre inesperado | P1 | M | — | Doc 14 UX |
| MO-31 🟢 | Compresión de imágenes en cliente antes de subir — CERRADO 2026-07-15. SkiaSharp 3.116.1 + `ImageCompressionService` (1920px max / JPEG 80). Integrado en `CreateReviewViewModel`. Reduce fotos de celular 3-8MB → 300-800KB (~85% ahorro). TourFormPage no sube imágenes desde mobile, KYB usa FilePicker para PDFs (skip) | P1 | S | — | Doc 14 UX |
| MO-32 🟢 | Upload resumable / en background con progreso — **CERRADO 2026-08-14 (Sprint 3 A, mobile local)**. Nuevo `TourGalleryUploadService` con `SemaphoreSlim(3)` (3 uploads paralelos máx) + progress per-item (0-100%) + retry en fallo transitorio. El wizard ya no bloquea al provider mientras suben las imágenes. `TourFormPage` extendido con Step 7 Galería que muestra progress bar por imagen | P1 | M | MO-31 | Doc 14 UX |
| MO-33 🟢 | Vista de calendario mejorada en `ScheduleCalendarPage` (día/semana/mes) — **CERRADO 2026-08-14 (Sprint 3 B, mobile local)**. `SfCalendar` (Syncfusion) + toggle Día/Semana/Mes vía botones + special dates predicate (marca los días con slots configurados) | P2 | M | — | Doc 14 UX |
| MO-34 🟢 | "Copiar precios de otro tour" al configurar schedule — CERRADO **2026-08-14 (Sprint 3 B, mobile local)** completando el alcance original del backlog. Iteración previa (2026-07-15) quedó como "Copiar de otra plantilla" mostrando TODAS porque el response `/tour-schedules/templates` no incluía `tourId`. Con **TC-019** el response ahora sí lo incluye, entonces se agregó botón "💰 Copiar precios de otro tour" en `ScheduleTemplateFormPage` con nuevo método `ScheduleService.GetTemplatesForTourAsync` que consume `GET /tour-schedules/templates?tourId={id}` — filtra plantillas por el tour destino, cerrando el gap original | P2 | S | TC-019 | Doc 14 UX |
| MO-35 | Dictado de voz opcional en descripciones largas | P3 | S | — | Doc 14 UX |
| MO-P6 🟢 | `TourFormPage` mobile crea tours `addressType=HOTEL_PICKUP` — CERRADO 2026-08-13 (mobile local, commit `35c205c` + merge `33dc18a`). Cumple decisión Luis P6 = SÍ (doc 15 §Preguntas abiertas). Picker `addressType` con opciones `MEETING_POINT`/`HOTEL_PICKUP` que oculta los campos geo cuando el provider elige HOTEL_PICKUP — coherente con el CHECK backend `tour_address_geo_required_unless_hotel_pickup` (migración 087). `TourFormDtos.cs` agregado el campo. Sin cambios backend | P1 | S | TC-017 | Doc 15 §Post-Sprint 2 |
| MO-P7 🟢 | `TourFormPage` mobile Meeting Point con Country/State/City en cascada + geolocation — CERRADO 2026-08-13 (mobile local, commit `0a10ef7` + merge). Cierra bug latente del CHECK backend `tour_address_geo_required_unless_hotel_pickup`: antes de este cierre el mobile no capturaba geo cuando el provider elegía `MEETING_POINT` → los INSERT reventaban con violación de constraint (`country_id/state_id/city_id/lat/lng` vacíos). Piezas: 3 `Picker` en cascada + nuevo `LocationCatalogService` (consume `/public/country/getAllCountryList`, `/public/state/getAllStateByCountryIdList/{id}`, `/public/city/getAllCityByStateIdList/{id}` — sin endpoints nuevos, reusa los del web) + botón "Usar mi ubicación actual" reusando el `ILocationHelperService` de MO-41 (Geolocation con permission on-demand) + comandos VM `LoadCountriesCommand`/`OnCountryChangedCommand`/`OnStateChangedCommand`/`UseCurrentLocationCommand`. Sin cambios backend | P1 | M | — | Doc 15 §Post-Sprint 2 |
| MO-P7b | `Syncfusion.Maui.Maps` embebido en `TourFormPage` con pin arrastrable para ajustar el punto exacto del meeting point — hoy el provider ve lat/lng como números o resuelve una vez con "usar mi ubicación". Deuda de UX; depende de Syncfusion License (MO-02 cerrada) pero no bloqueante | P2 | M | MO-P7, MO-02 | Doc 15 §Post-Sprint 2 |
| MO-P7c | Multi-location por tour en `TourFormPage` mobile — hoy captura sólo **una** location; el backend y el web soportan varias `TourLocation` por tour. Hay tours reales con múltiples meeting points (multi-punto en San Andrés) que el provider no puede crear desde mobile. Deuda funcional real | P2 | M | MO-P7 | Doc 15 §Post-Sprint 2 |
| MO-P7d | Cleanup de `AddLocationCommand` / `RemoveLocationCommand` no-op en `TourFormViewModel` — comandos heredados que no hacen nada útil ahora que la UI es single-location. Eliminar el código muerto para no confundir en próximas iteraciones. Se puede hacer cuando se aborde MO-P7c o antes si molesta | P3 | XS | MO-P7 | Doc 15 §Post-Sprint 2 |

#### D.4 — Features "solo mobile" (valor incremental)

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-40 🟢 | Firebase Cloud Messaging (push notifications) — turista, proveedor, operario. **4 fases cerradas 2026-07-15:** A backend base ✅ (MERGED), B mobile registro ✅, C mobile recepción ✅, D backend hooks ✅ — 5 flows: (1) turista reserva confirmada post-pago, (2) provider nueva reserva recibida, (3) turista recordatorio 24h antes (nuevo `TourReminder24hJob` cron 8am Bogotá), (4) turista crédito por expirar/expirado (agregado al `CreditExpirationJob`), (5) turista respuesta de review del provider. Secret Manager wired a Cloud Run. Falta validación end-to-end en device físico. **2026-07-17 HOTFIX #179**: migración 075 nunca corrió en Cloud SQL dev — el hook de Fase D consultaba `device_token` y explotaba con `relation does not exist` dentro de `@Transactional`, marcando la tx del pago como rollback-only → POST /payment 500 desde el 2026-07-15T21:24Z. Migración aplicada manualmente el 2026-07-17. | P1 | M | — | Doc 14 |
| MO-40b 🟢 | Deuda hotfix #179 CERRADA 2026-07-17 (PR #181). Nuevo package `services.push` con `PushDomainEvent` (sealed + 6 records) y `PushDomainEventListener` (`@Async @TransactionalEventListener(AFTER_COMMIT)`). 5 sitios migrados de llamada directa a `eventPublisher.publishEvent`. 8 tests dedicados al listener + invariante que previene regresión de fase. Ningún fallo del stack de push puede tumbar la tx de negocio | P1 | M | MO-40 | — |
| INF-04 🟢 | HOTFIX #179b `TemporalReservationExpiryJob` — CERRADO 2026-07-17 (PR #180). Guard `itemId == null` + cada iteración en `TransactionTemplate` `REQUIRES_NEW`. Desatasca el job del loop de `UnexpectedRollbackException` cuando encuentra reservas TEMPORAL huérfanas del FK ON DELETE SET NULL (migración 012). Anota bug de superficie: el FK cascade + `removeActiveItemsFromUserCart` fabrica huérfanas silenciosamente en el flujo de reschedule. 4 tests JUnit verdes | P1 | S | — | — |
| OP-179c 🟢 | CERRADA 2026-07-17. Wompi dev es sandbox — no hay plata real, no aplica refund. Aplicadas migraciones 074 y 076 que faltaban en dev (074 desatascaba las 5 restantes por `credit.expired_notified_at` faltante; 076 no rompía aún pero también estaba pendiente). Las 8 reservas quedaron CANCELED por el job | P1 | XS | INF-04 | — |
| INF-05 | **Automatizar migraciones SQL en deploy**. Ya van 3 migraciones que se salteran en dev (074, 075, 076): el flujo actual mergea `.sql` pero no las corre. Opciones: (a) Flyway/Liquibase al startup de Spring Boot con lock distribuido; (b) step aparte en Cloud Build antes del deploy de Cloud Run; (c) job manual documentado. La (a) es la más resiliente y estándar. Debe cubrir tanto dev como prod (con dry-run/checksum antes de correr en prod). Sin esto vamos a repetir el patrón #179 cada 2-3 semanas | P0 | M | — | 07 |
| MO-41 🟢 | Geolocalización: "tours cerca de mí" en `ExplorePage` — CERRADO 2026-07-15. Chip toggle + permission on-demand + Haversine client-side + distancia visible en cada card. Backend intacto | P2 | S | — | Doc 14 |
| MO-42 🟢 | Geolocalización: "cómo llegar al punto de encuentro" en `TourDetailPage` — CERRADO 2026-07-15. Sección meeting point + botón "Cómo llegar" via `Microsoft.Maui.ApplicationModel.Map.Default.OpenAsync` (respeta app default del sistema: Google Maps, Waze, etc.). Sin embedded map (MO-42b futuro) | P2 | S | — | Doc 14 |
| MO-43 🟢 | Deep-linking: compartir tour por WhatsApp con link universal — CERRADO 2026-07-15. Share sheet nativo + intent-filter capture | P2 | S | — | Doc 14 |
| MO-43b 🟢 | AutoVerify deep-links — **CERRADO 2026-08-14 (Sprint 4a)**. `MainActivity.cs` con `AutoVerify=true` en el `IntentFilter` del deep-link `https://dev.tourya.co/clients/tours-detail/*`. Android ahora abre la app **sin chooser**. Depende de `tourya-front` PR #117 mergeado que publica `assetlinks.json` en `public/.well-known/` con el fingerprint SHA-256 del keystore | P2 | XS | MO-43 | Doc 15 §Post-Sprint 3/4a/5a |
| MO-44 | Wallet integration: Apple Pay / Google Pay vía Wompi | P3 | M | — | Doc 14 |
| MO-45 | Widget "próxima reserva" (Android app widget) | P3 | M | — | Doc 14 |
| MO-53 🟢 | **Reagendar reserva turista con 3 casuísticas (igual/menor/mayor precio)** — **CERRADO 2026-08-14 (Sprint 6b, mobile local, commit `58d7a4f` + merge `1af997a`)**. Cero cambios backend — primer consumo desde mobile del `PUT /reservations/{id}/reschedule` que BE-20 ya había cerrado. Piezas mobile: `IReservationService.RescheduleReservationAsync`, DTOs `RescheduleReservationRequest`/`Response` con `priceComparison` (EQUAL/LOWER/HIGHER) en `Models/Reservation/`, botón "Reagendar" en `ReservationDetailPage` (visible sólo si `canReschedule==true`), nueva `RescheduleReservationPage` + `RescheduleReservationViewModel` con lista de días disponibles bounded por `MaxReschedulingDate` + dispatch por casuística, 12 keys i18n `reservation.reschedule.*` + `common.ok`. **Sorpresa del contrato**: para HIGHER el backend NO abre Wompi directo por el delta — cancela la reserva anterior, abre crédito por el valor pagado y agrega el nuevo tour al carrito (`status=CANCELLED_AND_ADDED_TO_CART`). Mobile hace dispatch: EQUAL/LOWER → alert + `//tourist/my-trips`; HIGHER → alert + `//tourist/cart` (el turista completa el pago via checkout normal). **Mismo patrón que web**, intencional del backend | P1 | S | BE-20 | Doc 15 §Post-Sprint 6 |
| MO-54 🟢 | **Forzar cambio password primer login operario (`mustChangePassword`)** — **CERRADO 2026-08-14 (Sprint 6a, mobile local, commit `8f50f1b` + merge `8693c97`)**. Cierra security issue moderado del operario recién creado (via MO-24 `ResetPassword` del PROVIDER) con clave temporal. **Cero cambios backend** — el backend ya exponía todo lo necesario: `AuthenticationResponse.mustChangePassword` en la respuesta del `POST /auth/authenticate` + endpoint self-service `PATCH /users` en `UserController.java:30-38`. Este sprint es el primer consumo desde mobile. Piezas mobile: campo `mustChangePassword` en `AuthResponse` DTO, guard en `LoginViewModel.LoginAsync` que navega a `//change-password` cuando el flag viene `true` (bloquea el home hasta cambiar la clave), nueva `ChangePasswordPage` + `ChangePasswordViewModel` + `ChangePasswordRequest` DTO, nuevo helper generic `PatchAsync<TRequest>` en `ApiService` (antes sólo existían el 2-genéricos y el multipart), `AuthService.ChangeMyPasswordAsync`, ruta `change-password` en `AppShell.xaml`, DI en `MauiProgram.cs`, 11 keys i18n `auth.changePassword.*` (es/en/pt) | P1 | S | — | Doc 15 §Post-Sprint 6 |
| MO-55 🟢 | **WompiHelper cleanup — hardening del checkout mobile** — CERRADO 2026-08-14 (Sprint 6, mobile local, commit `2cecbb2` + merge). Cierre técnico chico del hardening abierto por Sprint 4a. Reemplaza `EscapeJs` manual (que sólo escapaba `'` y newlines) por `System.Text.Json.JsonSerializer.Serialize` para cubrir XSS/escape completo (`<`, `\`, `</script>`, unicode) del user-input del turista embebido en el HTML/JS de `WompiHelper.BuildCheckoutHtml`. `redirectUrl` pasa de hardcode a la URL de producción → `Constants.WebBaseUrl` (dev-aware, coherente con MO-01b). `CheckoutViewModel.cs:176` | P2 | XS | MO-01b | Doc 15 §Post-Sprint 6 |

#### D.5 — Robustecer rol operario

| ID | Item | Prio | Talla | Depende | Referencia |
|----|------|:----:|:-----:|---------|------------|
| MO-50 🟢 | Modo offline: cache local de reservas del día para operario — CERRADO 2026-07-17. `IReservationCacheService` con JSON en `FileSystem.CacheDirectory`. Dashboard hace save-on-success + fallback-on-error con banner "📴 Modo offline · última actualización hace X min". **Extendido por MO-56 (Sprint 7) al `ProviderReservationsPage`** — ver fila siguiente | P2 | M | — | Doc 15 Ciclo 5 |
| MO-51 🟢 | Vista de reservas del operario mejorada — CERRADO 2026-07-17. Chips filtro por tour (auto-derivados) + agrupación por día (`CollectionView.IsGrouped` con `ReservationDayGroup`) + ordenamiento ascendente por `ScheduleDate + SlotTimeStart` | P2 | S | — | Doc 15 Ciclo 5 |
| MO-56 🟢 | **Modo offline extendido: cache local de la lista paginada del `ProviderReservationsPage`** — **CERRADO 2026-08-14 (Sprint 7, mobile local, commit `34d32ae` + merge `29dd8d6`)**. Extiende `IReservationCacheService` (que MO-50 solo aplicaba al Dashboard) para cachear también la lista completa que el operario ya scrolleó en `ProviderReservationsPage`. Cierra el gap del operario en el bote/muelle sin señal que hasta el Sprint 6 quedaba ciego a la lista paginada aunque el Dashboard sí funcionaba offline. Piezas mobile: 2 métodos nuevos en `IReservationCacheService` (`SaveProviderReservationsAsync` / `LoadProviderReservationsAsync`) + nuevo path `FileSystem.CacheDirectory/provider_reservations_page.json` (separado del `reservations_today.json` de MO-50 para no colisionar) + save-on-success en cada `LoadMore` exitoso + fallback-on-error activa `IsOfflineMode=true` + banner amarillo arriba del CollectionView reusando estilo Dashboard MO-50 + `_hasMorePages=false` en fallback evita paginar contra backend caído (pull-to-refresh reinicia el flow) + 2 keys i18n `reservation.offline.banner{.recent}` es/en/pt. `ProviderReservationsPage.xaml` Grid reestructurado a `RowDefinitions="Auto,Auto,*"` para alojar el banner. **Cero cambios backend** (todo es client-side caching sobre `GET /provider/reservations` que ya existía) | P2 | S | MO-50 | Doc 15 §Post-Sprint 7 |
| MO-56b | Sub-grouping por franja horaria (mañana / mediodía / tarde) en `ProviderReservationsPage`. Hoy MO-51 hace `OrderBy(ScheduleDate).ThenBy(SlotTimeStart).GroupBy(ScheduleDate)` — agrupación por día + orden ascendente por hora dentro del día, pero **sin sub-grupos por franja horaria**. Iteración corta que agregaría claridad al operario que ve muchas reservas del mismo día. **Decisión pendiente Franklin** si vale ejecutar — sino queda como nice-to-have | P3 | XS | MO-51 | Doc 15 §Post-Sprint 7 |

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
| IA-00 🟢 | Migración: crear tabla `agent_audit_log` (prompt, modelo, tokens, costo, decisión, entidad afectada) — CERRADO 2026-07-15. Migración 076 + entidad JPA + repository. 3 índices (agent+date, entity, user+date). Sin service/endpoint (eso es IA-01). Habilitador del bloque IA | P1 | S | — | [16](16-agentes-ia.md) principio #4 |
| IA-01 🟢 | Paquete `com.tourya.api.agents.shared` — CERRADO 2026-07-16. `ILlmClient` + `AnthropicClient` (REST, no-op sin API key) + `AgentLlmResponse` record + `ModelPricing` + `PromptTemplate` (classpath) + `AgentRunResult<T>` sealed + `AgentAuditEntry` + `AgentAuditWriter @Async` + `BudgetGuard` (por agente en app_config). 13 unit tests JUnit + Mockito (7+6). Franklin sube `ANTHROPIC_API_KEY` a Secret Manager cuando active | P1 | M | IA-00 | [16](16-agentes-ia.md) arq |
| IA-02 🟢 | Agente 1: **Travel Concierge** (búsqueda + carrito + FAQ) — CERRADO 2026-08-14. `TravelConciergeService` con loop de function calling (max 5 iter) contra `TourService`/`ShoppingCartService`/`SearchTourScheduleFullService`; 4 funciones expuestas: `search_tours`, `get_tour_detail`, `add_to_cart`, `get_cart`. Endpoint `POST /agents/travel-concierge/chat` (JWT). **Cambio de provider**: se implementa con Vertex AI Gemini (2.5 Pro por default) en vez de Anthropic — reusa GCP infra + ~40% mas barato + `ILlmClient` era ya provider-agnostic. Nuevo `GeminiClient implements ILlmClient` (auth ADC, modo no-op degradado) + `LlmClientConfig` con `@Primary` selector por `agents.provider` — Anthropic queda como impl alternativa. Guardrails en codigo (no solo prompt): deny-list `WOMPI_INTEGRITY_SECRET`/`JWT_SECRET` → escala humano; scrub `providerPrice`/`slotPercentageTourya` del contexto pre-envio (defense-in-depth aunque los DTOs ya no los exponen); contador in-memory de pagos fallidos por sessionId con umbral 3 → `fraud_suspected=true` en metadata (nunca al turista). Migracion 092 agrega columna `metadata JSONB` + GIN index a `agent_audit_log` para `session_id`/`fraud_suspected`/`actions_executed`. 9 tests JUnit + Mockito con `MockLlmClient` (queue de responses hardcoded) cubriendo 5 escenarios del brief + budget + fraud + scrub. **Franklin habilita**: Vertex AI API ya activa, SA con `roles/aiplatform.user`, region `us-central1`. Docs 00, 09, 16, 17 sincronizados | P1 | M | IA-01 | [16](16-agentes-ia.md) |
| IA-02-FE 🟢 | **FE-Concierge widget Angular** — CERRADO 2026-08-14 (tourya-front PR #118 mergeado). Consume `POST /agents/travel-concierge/chat` de IA-02. Nuevo `TravelConciergeService` con `sessionId` UUID persistido en `sessionStorage` (multi-tab separadas) + locale del `TranslateService`. `ConciergeChatWidget` (FAB flotante bottom-right expandible a panel 380×500 desktop / full-screen mobile <640px) con historial burbujas usuario/asistente + chips clickeables para acciones ejecutadas ("🔍 3 tours encontrados", "✅ Agregado al carrito") + banner amarillo si `escalatedToHuman=true`. Integrado en `list-tours` (Explore, sin context), `tours-detail` (con `tourId`), `cart-summary` (backend infiere `cartId` del JWT). 10 keys i18n `concierge.*` en `public/i18n/{es,en,pt}.json`. Build production verde, 0 warnings nuevos. **Adaptaciones vs brief**: se respetó la convención real del repo (`src/app/shared/{services,models,common}/` con `standalone: false` en `SharedModule`, no `src/app/{services,models,components}/` standalone) | P1 | S | IA-02 | Doc 15 §Post-Sprint 8 |
| IA-02-MO 🟢 | **MO-Concierge widget MAUI** — CERRADO 2026-08-14 (tourya-mobile local commit `7c76f39` + merge `190f1ba`). Consume el mismo endpoint del IA-02. Nuevo `IConciergeService` + `ConciergeService` con `sessionId` persistido en `SecureStorage` (sobrevive app restart), locale de `I18nService`, POST vía `ApiService.PostAsync<T,R>`. `ConciergeChatPage` modal (`CollectionView` burbujas + Entry + `ActivityIndicator` + chips por acción + banner escalado) navegable con `Shell.Current.GoToAsync("concierge?tourId=X&cartId=Y")`. FAB "Concierge Tourya" en `TourDetailPage.xaml`, `CartPage.xaml`, `ExplorePage.xaml` (bottom-right, padding 16dp). Ruta `concierge` en `AppShell.xaml` fuera de tabs. DI singleton service + transient VM/Page. **Rotación de session en logout**: `AuthService.LogoutAsync` invoca `_storage.RemoveConciergeSessionIdAsync()` ANTES de `ClearAllAsync` (defense-in-depth doble llamada) — próximo user arranca con hilo fresco. 10 keys i18n `concierge.*` en `I18nService`. **Divergencia patrón**: los 2 comandos nuevos expuestos como propiedades explícitas `new AsyncRelayCommand(...)` en el constructor en vez de `[RelayCommand]` — necesario para respetar baseline 140 warnings (RelayCommand + XAML source-gen no se coordinan y generan MAUIG2045). Blueprint para futuro cleanup de los ~100 MAUIG2045 pre-existentes del codebase. Build verde 0 errores 0 warnings nuevos | P1 | S | IA-02 | Doc 15 §Post-Sprint 8 |
| IA-03 | Cuenta Twilio + WABA aprobado + plantillas de mensaje | P1 | M | — | [11](11-integraciones.md) |
| IA-04 | Servicio `TwilioMessagingService` (envío + webhook inbound) | P1 | M | IA-03 | [11](11-integraciones.md) |
| IA-05 | Agente 2: **Support 24/7** (WhatsApp + cancelación/reagendamiento) | P1 | L | IA-04, IA-01 | [16](16-agentes-ia.md) |
| IA-06 | Agente 3: **Desert Shopping Cart** (recuperación carrito) | P2 | S | IA-04 (o fallback email) | [16](16-agentes-ia.md) |
| IA-07 🟢 | Agente 4: **Operator Support** backend CERRADO 2026-08-14 (rama `feature/ia-07-operator-support-backend`). 4 endpoints action-specific bajo `/agents/operator-support/*`: `POST /suggest-tour-content` (nombres SEO + descripción 200-400 palabras es + tags del catálogo), `POST /price-alert/{tourId}` (severity OK/WARN/CRITICAL/UNKNOWN + rango min/max/median comparables — heurística fallback +/-15%/±35%, RN-014 solo alerta), `POST /draft-review-reply/{reviewId}` (borrador + tone PROFESSIONAL/WARM/APOLOGETIC + detectedLocale, operador aprueba antes de `PATCH /public/save/review/{reviewId}`), `POST /validate-gallery` (cero costo LLM — reusa reglas de `GalleryValidator` sobre metadata pre-upload). `OperatorSupportService` con guardrails idénticos a IA-02 (deny-list secretos + scrub `providerPrice`/`slotPercentageTourya`/`slotPorcentajeTourya`/`porcentajeTourya` + budget guard + audit siempre). Autorización owner-based: `requireTourOwnership` valida antes de cualquier call. **Modelo Gemini 2.5 Pro** (mismo motivo que IA-02: reusa GCP ADC, ~40% más barato). Costo esperado **$1-2/mes** (baja vs $2-4 con Sonnet). Nuevo `OperatorSupportRepository` con 2 native queries usando `price` público (nunca `provider_price` interno entre operadores). 3 prompts versionados `prompts/operator-support/*.v1.txt`. 11 unit tests con `MockLlmClient`. Cero migración BD (reusa `agent_audit_log.metadata JSONB` de IA-02 mig 092). Cero cambios en `TravelConciergeService` / `AnthropicClient`. Feature flag `AGENTS_OPERATOR_ENABLED`. **Traducción es→en/pt DEFERRED a IA-09** (TODO en el service, 1 wire-up). Docs 00, 09, 16, 17 sincronizados | P1 | M | IA-01 | [16](16-agentes-ia.md) |
| IA-07-FE 🟢 | **FE Operator Support Angular** — CERRADO 2026-08-15 (tourya-front PR #119 mergeado, commit `383a4c6`). Integra los 4 endpoints IA-07 como touchpoints aditivos en flujos existentes del provider — nada nuevo aparte, cero regresión. **Touchpoint 1** (sugerir contenido): botón `✨ Sugerir con IA` en el header de la sección Descripción del `add-tour` (component monolítico, no wizard) → modal con 3 nombres candidatos + descripción SEO + tags multi-select, todo requiere click explícito. **Touchpoint 2** (price alert): badge único al top de la sección Prices de `tour-schedule` (multi-slot × multi-age — endpoint idempotente por tourId), refrescado on-blur. Verde OK / amarillo WARN / rojo CRITICAL. **Touchpoint 3** (validate-gallery): pre-upload en `tour-gallery` con metadata local (`img.onload` + `file.size/type`) → renderiza issues estructurados + bloquea si CRITICAL. Fallback silencioso si el endpoint no responde. **Touchpoint 4** (draft-review-reply): botón `✨ Borrador con IA` en cada card de reseña sin responder de `provider-reviews` → pre-llena el TextEditor existente + badge "Tono detectado". Nuevo `OperatorSupportService` + models en `src/app/shared/{services,models}/`. 20 keys i18n `operatorSupport.*` en `public/i18n/{es,en,pt}.json`. Build production verde, 0 warnings nuevos. **Deudas detectadas**: (a) FE solo tiene `subCategory: code` string, envía `subcategoryId: null` — deuda alineación tipos con backend si baja calidad de sugerencias; (b) `ProviderReview.id` es `string?`, `Number(reviewId)` con guard | P1 | S | IA-07 | Doc 15 §Post-Sprint 9 |
| IA-07-MO 🟢 | **MO Operator Support MAUI** — CERRADO 2026-08-15 (tourya-mobile local commit `b6de801` + merge `cc45149`). Mismos 4 touchpoints adaptados a MAUI 10 Android. **Descubrimiento estructural**: en mobile los precios del tour viven en `ScheduleTemplateForm` (no en `TourFormPage` como el web) — el agente descubrió la estructura real y puso el price-alert donde tiene sentido: banner con `DataTriggers` para OK/WARN/CRITICAL en la parte superior del form de plantilla + `EventToCommandBehavior` con `EventName="Unfocused"` en cada Entry de Price (multi-slot × multi-age). **Touchpoint 1** (sugerir contenido): botón "✨ Sugerir con IA" en el step Descripción del `TourFormPage` → navega a nueva `OperatorSuggestModalPage` en `Views/Agents/` con 3 secciones apilables (nombres/descripción/tags). **Touchpoint 3** (validate-gallery): pre-upload usando `SKBitmap.Decode` de SkiaSharp (ya en el proyecto por MO-31) para leer metadata local + `ValidateGalleryAsync` server-side. Fallback silencioso. **Touchpoint 4** (draft-review-reply): botón "✨ Borrador con IA" en cada reseña de `ProviderReviewsPage` → pre-llena el TextEditor del Sprint 5a. Nuevo `IOperatorSupportService` + `OperatorSupportService` reusando `ApiService.PostAsync<T,R>`. DI singleton en `MauiProgram.cs`. 21 keys i18n `operatorSupport.*` en `I18nService`. **Patrón comandos**: `AsyncRelayCommand` como propiedad explícita en constructor (NO `[RelayCommand]`) — blueprint Sprint 8 MO-Concierge conservado, baseline 140 warnings intacto. **Nota flow**: el agente stopped por corte de proceso durante Sprint 9 alcanzó a hacer todo el trabajo sobre `develop` compartido (sin worktree) — recovery manual: build verde 0/0, crear rama `feature/mobile-sprint9-operator-support`, commit 14 archivos (9 mod + 5 nuevos, 803 líneas), merge local no-ff a `develop`, cleanup rama. Cero cambios en `ConciergeService`/`WompiHelper`/checkout | P1 | S | IA-07 | Doc 15 §Post-Sprint 9 |
| IA-08 | Agente 5: **Backoffice Support** (pre-verificación KYB + tour + manifiesto DIMAR) | P2 | M | IA-01 | [16](16-agentes-ia.md) |
| IA-09 🟢 | Traducción automática es → en/pt-BR con **Google Cloud Translation v3** CERRADO 2026-08-15 (rama `feature/ia-09-google-cloud-translation`). Nuevo paquete `com.tourya.api.services.translation` con `ITranslationService` + `GoogleCloudTranslationService` (auth ADC, SA `tourya-dev-cloud-run` con `roles/cloudtranslate.user`, API `translate.googleapis.com` habilitada). Hook en `TourService.saveCreateOrUpdateFullData` publica `TourTranslationEvent`; `TourTranslationEventListener` (`@TransactionalEventListener(AFTER_COMMIT)`) delega en `TourTranslationApplier.translateTourAsync` (`@Async @Transactional`). El applier recorre todos los campos JSONB del tour (`name`, `description`, `TourAddress.location`, `TourMainAttraction`, `TourIncludesExcludes`, `TourFaq.question`+`answer`, `TourItinerary.title`+`description`, `TourCancellationPolicy.observations`, `TourGallery.description`). Guardrail: solo rellena `en`/`pt` vacíos, no sobrescribe lo que el provider escribió manualmente (o aceptó de IA-07). Modo no-op degradado si `agents.translation.enabled=false` o el SDK falla al inicializar. Convención `pt` en el JSONB (mismo que el resto del proyecto — ver grep `\"pt\":` en migración 056); internamente se mapea `pt`→`pt-BR` para el call a Google (portugués de Brasil, decisión de Luis en `traduccion-automatica-tours.md`). Wire-up con IA-07: el TODO deferred de `OperatorSupportService` (`// TODO IA-09`) se cerró — el agente sigue devolviendo solo español (RN-011: aprobar en su idioma) y la traducción ocurre downstream al guardar, sin acoplamiento directo. Nuevo endpoint admin `POST /admin/tours/{tourId}/retranslate` (solo ADMIN) para backfill de tours legacy sin en/pt + testing. 17 unit tests (`TranslationServiceTest` 9/9 + `TourTranslationApplierTest` 8/8) con `MockTranslationClient` — cero llamadas a Google en el pipeline. Cero migración BD (los JSONB ya existen). Cero cambios en `TravelConciergeService`, `AnthropicClient`, `GeminiClient`, ni en el core de `OperatorSupportService` fuera del wire-up del TODO. Docs 00, 11, 16, 17 sincronizados. Feature flag `AGENTS_TRANSLATION_ENABLED` para apagar rápido sin re-deploy | P2 | S | — | [11](11-integraciones.md), `traduccion-automatica-tours.md` |
| IA-10 | Agente 6: **Moderación de reseñas** (Haiku 4.5) | P2 | M | IA-01 | RN-050 |
| IA-11-BE 🟢 | Dashboard de observabilidad de agentes — **backend** CERRADO 2026-08-15 (rama `feature/ia-11-agents-observability-backend`). Nuevo paquete `com.tourya.api.agents.observability` con `AgentObservabilityService` + `AgentObservabilityRepository` (JDBC + `percentile_cont` + `date_trunc` + operadores JSONB `->>`, `@>`). Nuevo `AgentObservabilityController` bajo `/admin/agents` con 6 endpoints: `GET /summary` (calls/tokens/cost/latencia/successRate/escalatedRate por agente), `GET /timeseries?granularity=day\|week\|month` (bucket por fecha + agente), `GET /latency?capability=` (p50/p95/p99/min/max), `GET /top-consumers` (top-N usuarios con `LEFT JOIN _user`, límite max=100), `GET /result-types` (distribución success/error/rejected), `GET /overrides` (proxy MVP `escalatedRate + errorRate`). Todos autenticados JWT rol **ADMIN** o **BACKOFFICE_OPERATION** (guard en service vía `Utils.isTouryaBackoffice`, mismo patrón que `AdminCreditController` — TC-022 #253) — rol insuficiente → `InsufficientPrivilegesException` (401 via `GlobalExceptionHandler`). Nunca expone `prompt_input`, `result_json` ni el `metadata` completo — solo agregados. `Cache-Control: private, max-age=60` para amortiguar refresh. Cero migración BD — reutiliza los 3 índices de `076_agent_audit_log.sql` (`idx_agent_audit_log_agent_date`, `idx_agent_audit_log_user_date`, `idx_agent_audit_log_metadata` GIN). 14 tests JUnit + Mockito con repositorio mockeado cubriendo guard de rol (ADMIN, BACKOFFICE_OPERATION, otro, null), default de rango 30 días, filtros pass-through, normalización de granularity inválida → `day`, blank → null. Cero cambios en `TravelConciergeService`, `OperatorSupportService`, `GoogleCloudTranslationService`, `AnthropicClient`, `GeminiClient`, `AgentAuditWriter`. Docs 00, 09, 16, 17 sincronizados | P2 | S | IA-01 | [16](16-agentes-ia.md) §Observabilidad |
| IA-11-FE | Dashboard de observabilidad de agentes — **frontend Angular admin** (sprint siguiente): tabla resumen por agente + gráficos time series + selector de rango + gauge de latencia + tabla top consumers + heatmap result-types. Consume los 6 endpoints IA-11-BE. Solo ADMIN/BACKOFFICE_OPERATION | P2 | S | IA-11-BE | [16](16-agentes-ia.md) §Observabilidad |
| IA-11b | **Deuda Opción B override rate**: agregar columna `human_outcome VARCHAR(20) NULL` a `agent_audit_log` (dominio `KEPT\|EDITED\|DISCARDED`) + hook en el frontend admin/provider que reporta el outcome cuando el humano guarda tras una sugerencia (Operator Support: comparar la `descriptionSuggestion` con lo que el provider terminó guardando). Migración 093 + endpoint PATCH `/admin/agents/audit/{id}/outcome`. Reemplaza el proxy `escalatedRate + errorRate` del MVP por `overrideRate = 100 * (EDITED + DISCARDED) / (KEPT + EDITED + DISCARDED)` real. Bloqueado por IA-11-FE (no tiene sentido tracking sin UI que lo dispare) | P3 | M | IA-11-FE | [16](16-agentes-ia.md) §Observabilidad |

**Subtotal agentes**: ~3–4 días de agente + iteración de prompts con datos reales + espera de aprobación Twilio WABA = **~2–3 semanas calendario** (el tiempo de Twilio WABA es el más largo y NO se puede acelerar).

---

## Plan por fases (con estimaciones agente + calendario real)

### 🚨 Fase 0 — Higiene y seguridad crítica

**Objetivo**: **cerrar el techo antes de invitar gente a la fiesta**. Sin esto no salimos a producción.

| Ítem | Talla | Trabajo | Nota |
|------|:-----:|---------|------|
| MO-00 Repo GitHub mobile | XS | Franklin | Solo crear repo + push. Necesita acceso a `Touryapp/` |
| 🟢 MO-01 URL backend mobile | XS | Agente | Cambio en `Constants.cs` → `http://34.160.22.16/api/v1/` (LB GCP) |
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
| 4 | ✅ ~~Push provider para FCM~~ **DECIDIDO 2026-07-15**: FCM (Firebase Cloud Messaging). Reusar proyecto Firebase `tourya-169d6` donde Franklin ya tiene acceso. Rechazadas: OneSignal (SaaS, pago), AWS SNS (nos ata a AWS que estamos saliendo) | Franklin | Decidido |
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
