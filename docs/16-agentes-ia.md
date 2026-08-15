# 16 — Agentes IA

5 agentes IA core para Tourya que actúan como **roles del negocio** (no como features pegadas). Diseñados para sostener la meta del roadmap: *"operable con una sola persona, apoyada por IA"* (horizonte 6 meses, ver [01 — Visión y negocio](01-vision-y-negocio.md)).

> ⚠️ **Nota de numeración**: el índice de [00-README.md](00-README.md) no reserva un número para este documento — el `09` ya está tomado por [09-api-design.md](09-api-design.md). Se numera `16` (siguiente disponible tras `14-gap-web-mobile.md` y `15-mvp-mobile-estado.md` ) y debe agregarse al índice del README.

> 🆕 **Nota de marcas**: a diferencia del resto de la documentación de Tourya, nada aquí es "deducido del código" — estos agentes **no existen todavía**. Se usa 🆕 **PROPUESTO** para diseño de este documento, 📌 **PENDIENTE LUIS** para decisiones que requieren validación del cliente, ⚠️ **RIESGO/DEPENDENCIA** para bloqueos técnicos reales (ej. integraciones no implementadas), y ✅ cuando el diseño se ancla en una regla de negocio o endpoint que **sí** existe hoy en el código.

---

## Contexto

Tourya planea incorporar **agentes de IA** que asistan al usuario final y a la operación interna. La visión a mediano plazo es que la plataforma pueda operar con **una sola persona** apoyada por IA, y a largo plazo evolucionar hacia un "autopiloto" que ejecute decisiones autónomamente en ciertos flujos.

Referencias en otros documentos:
- [01 — Visión y negocio](01-vision-y-negocio.md) — visión de eficiencia con IA.
- [14 — Web vs Mobile](14-gap-web-mobile.md) — menciona el **Travel Concierge** en el alcance del turista.
- [05 — Reglas de negocio](05-reglas-de-negocio.md), RN-050 — agente IA de **moderación de reseñas** en el roadmap.

---

## Principios rectores

1. **Los agentes son actores de primera clase**. Deberían aparecer en [03 — Roles y actores](03-roles-y-actores.md), en la sección de "Sistemas externos", con rol, input/output, permisos y auditoría — igual que Wompi o Firebase hoy.

2. **Un solo proveedor de modelo para el MVP**. A diferencia de arquitecturas multi-LLM, Tourya tiene **un equipo de desarrollo de 1 persona** (ver 01-vision-y-negocio). Mantener un solo proveedor (Anthropic) reduce la superficie de mantenimiento. La abstracción `ILLMClient` se conserva igual como principio — permite migrar a otro proveedor sin reescribir los agentes — pero el MVP **no** necesita mezclar proveedores.

3. **Human-in-the-loop en lo crítico**. El agente sugiere o ejecuta autónomo solo cuando una regla de negocio existente (RN-XXX) lo permite sin ambigüedad. Aprobar KYB, aprobar tours y adjudicar comisiones siguen siendo decisiones 100% humanas (RN-010, RN-046) — ningún agente las reemplaza.

4. **Auditoría obligatoria**. Cada acción del agente queda registrada: prompt, modelo, tokens, costo, decisión, resultado, entidad afectada. Tourya hoy **no tiene un audit log explícito** para acciones críticas (ver [12 — Seguridad, sección Auditoría](12-seguridad-y-auth.md)) — este es un prerequisito a resolver antes de dar autonomía a cualquier agente, no solo para ellos.

5. **Override humano siempre disponible**. Cualquier acción de un agente es reversible por el rol humano equivalente (PROVIDER, ADMIN, BACKOFFICE_OPERATION).

6. **Presupuesto controlado**. Cada agente tiene un tope mensual configurable en `app_config` (la tabla ya existe y ya soporta JSONB genérico, ver [08 — Modelo de datos](08-modelo-de-datos.md)). Si se excede, el agente se detiene y notifica.

7. **Nunca reemplazan una decisión regulatoria**. Aprobación de KYB (RN-046), aprobación de tours (RN-010), asignación de comisión Tourya (RN-016) y cancelación por lluvia (RN-032) están reservadas por regla de negocio a ADMIN — ningún agente puede ejecutarlas, solo prepararlas.

---

## Stack de modelos

> ✅ **Cambio 2026-08-14 (IA-02)**: Franklin migró el stack a **Vertex AI Gemini** en lugar de Anthropic. Motivos: (1) reusa infraestructura GCP ya existente (SA `tourya-dev-cloud-run` con `roles/aiplatform.user` — cero cuenta nueva, cero key nueva), (2) ~40% más barato para el mismo volumen, (3) el framework `agents/shared/` estaba diseñado para ser provider-agnostic vía `ILlmClient` — el switch es aditivo, cero rewrite. `AnthropicClient` queda como impl alternativa (basta con `AGENTS_PROVIDER=anthropic + ANTHROPIC_API_KEY`) por si se quiere hacer A/B testing más adelante.

| Modelo | Uso | Costo (input/output por 1M tokens) |
|---|---|---|
| **Gemini 2.5 Pro** (`gemini-2.5-pro`) | Razonamiento sobre ficha del tour, function calling contra carrito, cancelación/reagendamiento con política, verificación KYB (multimodal — lee imágenes/PDF). Equivalente a lo que antes hacía Claude Sonnet 5. | $1.25 / $10 (contexto ≤200k tokens) |
| **Gemini 2.5 Flash** (`gemini-2.5-flash`) | Clasificación de intención, FAQ simple, recuperación de carrito, borradores estructurados. Equivalente a lo que antes hacía Claude Haiku 4.5. | $0.075 / $0.30 |

> Provider abstraction: `ILlmClient.complete(prompt, model)` — cambiar de modelo o de proveedor es una env var (`AGENTS_PROVIDER=gemini|anthropic`), no un rediseño de agente.

**Costo estimado a la meta 12m (revisado con precios Gemini)**: los totales del doc se recalculan ~a 1/3 de lo que estimaba con Anthropic. Ver "Costo estimado" en cada agente y la tabla resumen abajo.

**Auth Vertex AI**: Application Default Credentials (ADC). En Cloud Run usa la SA automáticamente; en local dev requiere `gcloud auth application-default login` (Franklin ya lo hizo). **Sin API key** que rotar o esconder — ventaja operativa frente a Anthropic.

📌 **PENDIENTE LUIS** — si más adelante se necesita traducción masiva de tours (es→en/pt), usar **Google Cloud Translation** (ya presupuestado en [11 — Integraciones](11-integraciones.md): ~$0.0002/tour) en vez de un LLM — es una tarea estructurada donde un servicio de traducción dedicado es más barato y más consistente que pedirle a Sonnet 5 que traduzca.

---

## Los 5 agentes core
### Agente 1 — Travel Concierge

**Rol equivalente humano**: el vendedor/asesor que hoy el turista busca por WhatsApp o Instagram antes de que existiera Tourya (ver "Problema que Tourya resuelve" en 01-vision-y-negocio).

**Canal**: Web / App (Angular + MAUI). No depende de ninguna integración pendiente.

**Cuándo actúa**: en `ExplorePage` (búsqueda), `TourDetailPage` (dudas pre-compra), `CartPage`/`CheckoutPage` (gestión de carrito).

**Input que recibe**:
- Consulta en lenguaje natural del turista.
- Resultado de `POST /public/tours/schedule/search` (que internamente llama `sp_get_tour_schedule_json`).
- Ficha completa del tour: `TourDetailResponse` (itinerario, FAQ, `TourCancellationPolicy`, incluye/no incluye).
- Estado actual de `ShoppingCartResponse`.

**Output que produce**:
1. Resultados de búsqueda explicados en lenguaje natural (nunca inventa filtros que el SP no soporta).
2. Respuestas a dudas pre-compra, ancladas siempre en la ficha real (`TourFaq`, `TourItinerary`, `TourCancellationPolicy`) — nunca improvisa política de cancelación.
3. Acciones de carrito: `POST /shopping-cart`, `POST /shopping-cart/items` (agregar/quitar ítems, ajustar `ageType`/`quantity`).
4. Sugerencia de método de pago alterno si el widget Wompi falla.

**Modelo usado (post IA-02, 2026-08-14)**: Gemini 2.5 Pro razona sobre la ficha del tour y ejecuta function calling contra el carrito (`search_tours`, `get_tour_detail`, `add_to_cart`, `get_cart`). Gemini 2.5 Flash queda disponible para clasificación de intención cuando la carga lo justifique — el MVP arranca solo con 2.5 Pro para minimizar branch de código. **Migración desde Claude Sonnet 5 / Haiku 4.5 documentada en el "Stack de modelos"**.

**Modo de actuación**:
- ✅ **Autónomo**: búsqueda, respuestas informativas, gestión de carrito.
- ❌ **Nunca autónomo**: el checkout final lo ejecuta el widget Wompi, no el agente — el agente nunca ve ni procesa datos de tarjeta (RN-025, el Public Key es lo único expuesto al frontend).
- ❌ **Guardrail no negociable**: el agente **jamás** recibe el `integrity_secret` de Wompi ni el `WOMPI_INTEGRITY_SECRET`/`JWT_SECRET` en su contexto — son vulnerabilidades CRITICAL ya documentadas (C-1, C-2 en [12 — Seguridad](12-seguridad-y-auth.md)) y no deben agravarse exponiéndolas a un LLM externo.
- ⚠️ **Escala a humano**: si detecta patrón de fraude (varios intentos de pago fallidos + comportamiento sospechoso) — silenciosamente, sin alarmar al turista.
- ❌ **No modifica precios ni comisiones** — `providerPrice` y `slotPercentageTourya` son inmutables para este agente (RN-014, RN-019).

**Prompt base** (resumen):

```
Eres el Agente Concierge de Tourya, marketplace de tours en San Andrés Islas, Colombia.

Recibes:
- Mensaje del turista (texto libre, puede venir en español, inglés o portugués)
- Resultados de búsqueda ya filtrados por el backend (sp_get_tour_schedule_json)
- Ficha completa del tour si la conversación ya identificó uno: itinerario, FAQ,
  política de cancelación, incluye/no incluye
- Estado actual del carrito del turista

Tu trabajo:
1. Si es una búsqueda: explica los resultados en lenguaje natural, resalta lo relevante
   a la consulta (fecha, precio, categoría, tags)
2. Si es una duda: responde SOLO con datos de la ficha real. Si no está en la ficha,
   dilo explícitamente y ofrece escalar a soporte — NUNCA inventes política de
   cancelación, itinerario o precio
3. Si pide agregar/quitar del carrito: genera la llamada a la función correspondiente
   con productId, tourScheduleId, slotId, scheduleDate y details[ageType, quantity]
4. Si el pago Wompi falló: sugiere reintentar o cambiar de método, nunca proceses
   ni solicites datos de tarjeta

IMPORTANTE:
- Nunca inventes disponibilidad de un slot — siempre verifica contra el resultado real
- Nunca reveles slotPercentageTourya ni providerPrice — el turista solo ve el price final
- Si detectas 3+ intentos de pago fallidos en la sesión, marca fraude_sospechado: true
  y no lo menciones al turista

Responde en el idioma del turista.
```

**Costo estimado (recalculado 2026-08-14 con precios Gemini 2.5)**: con la meta de 12 meses (500 turistas registrados, ~300 reservas/mes, ~2.000 sesiones de búsqueda/mes), todo con Gemini 2.5 Pro ≈ **$8–12/mes** (contra $20–25/mes que estimaba con Anthropic). Si se decide agregar clasificación con Gemini 2.5 Flash el número baja aún más.

---

### Agente 2 — Support 24/7

**Rol equivalente humano**: Soporte al turista + parte del rol de "Compras/Backoffice" que hoy resolvería reagendamientos manualmente.

**Canal**: WhatsApp.

> ⚠️ **RIESGO/DEPENDENCIA**: la integración con **Twilio Programmable Messaging para WhatsApp** aún no está implementada (ver [11 — Integraciones](11-integraciones.md)). Este agente no puede lanzarse hasta resolver ese prerequisito.
>
> ✅ **Decisión Luis + Franklin (2026-07-07)**: usar **Twilio** como BSP en vez de integrar directo con Meta/Facebook. Twilio simplifica el onboarding (WABA aprobado por ellos), tiene SDK estable y no requiere pelear con el proceso de verificación de Meta directamente. Costo mayor a cambio de velocidad de implementación.

**Cuándo actúa**: tras la confirmación de pago (reenvío de QR), y ante cualquier mensaje entrante del turista.

**Input que recibe**:
- Mensaje del turista.
- `Reservation` completa: `deliveryStatus`, `maxCancellationDate`, `maxReschedulingDate`, `canCancel`, `canReschedule`, `qrUrl`.
- `TourCancellationPolicy` del tour (Flexible/Estándar/Moderado/Estricto — RN-031).
- Disponibilidad de nuevos slots (`sp_search_slot_effective_availability`) para reagendamiento.
- `MaritimActivityReport` vigente si el tour es marítimo.

**Output que produce**:
1. Reenvío de `qrUrl`, estado de reserva, recordatorio de punto de encuentro.
2. Respuesta FAQ anclada en la política real del operador (nunca genérica).
3. **Cancelación**: cruza fecha/hora actual contra `maxCancellationDate` y la política del tour; si aplica, ejecuta `PUT /reservations/{id}/cancel` y genera el `Credit` automáticamente (100% según RN-031, nunca hay reembolso parcial en las políticas actuales).
4. **Reagendamiento**: valida `canReschedule` + `allowsRescheduling` + `maxReschedulingDate` (RN-033), consulta disponibilidad del nuevo slot, y resuelve uno de los 3 casos de RN-033 (precio igual, menor con crédito, mayor con cobro de diferencia).

**Modelo usado**: Claude Haiku 4.5 para FAQ/triage de intención y urgencia; Claude Sonnet 5 para los casos de cancelación/reagendamiento que requieren razonar sobre la política y calcular montos.

**Modo de actuación**:
- ✅ **Autónomo**: cancelación dentro de política (RN-030/RN-031) — genera el `Credit` sin intervención humana. Reagendamiento con precio igual o menor (casos 1 y 2 de RN-033).
- ⚠️ **Sugiere a humano**: reagendamiento con diferencia de precio a favor de Tourya (caso 3) — el agente prepara el link de pago de la diferencia, pero no lo cobra sin confirmación explícita del turista.
- ❌ **Bloqueado — nunca autónomo**: cancelación por lluvia. `PUT /reservations/{id}/cancel/rain` está reservado a ADMIN por RN-032 y requiere un `MaritimActivityReport` activo. El agente detecta el patrón (turista pregunta por clima + tour marítimo + reporte con bandera amarilla/roja) y **crea el ticket para que ADMIN ejecute**, nunca cancela él mismo.
- ❌ **Bloqueado — escalamiento inmediato sin intentar resolver**: queja grave, incidente de seguridad, o cualquier mención de integridad física en un tour marítimo.

**Costo estimado**: con 300 reservas/mes y ~3 interacciones promedio por reserva (≈900 conversaciones/mes), asumiendo 70% resueltas por Haiku 4.5 y 30% escaladas a Sonnet 5 para cancelación/reagendamiento ≈ **$5–7/mes** en inferencia LLM. (Este costo es *aparte* del costo de mensajería WhatsApp por Meta/BSP, que se calculó por separado según el mix de países de los turistas.)

---

### Agente 3 — Desert Shopping Cart (recuperación de carrito abandonado)

**Rol equivalente humano**: nadie hoy hace esto — es pura ganancia incremental (sin Tourya, un carrito abandonado simplemente se pierde).

**Canal**: WhatsApp o email.

> ⚠️ **RIESGO/DEPENDENCIA**: comparte la dependencia de Twilio del Agente 2. El canal email ya existe (SMTP Gmail Workspace, ver 11-integraciones.md) y puede usarse como fallback mientras Twilio no esté disponible.

**Cuándo actúa**: cuando un `ShoppingCart` permanece `ACTIVE` sin checkout tras un tiempo configurable, o cuando una `Reservation` en `TEMPORAL` está por expirar (RN-022, hold de 15 minutos, job `TemporalReservationExpiryJob`).

**Input que recibe**: `ShoppingCartItem`/`ShoppingCartItemDetail` (qué tour, qué fecha, qué cantidad), tiempo transcurrido desde el último evento del carrito.

**Output que produce**: un mensaje de reenganche ofreciendo terminar la compra o resolver dudas sobre el pago — nunca ofrece un descuento (Tourya **no tiene módulo de cupones hoy**, es roadmap explícito en 05-reglas-de-negocio).

**Modelo usado**: Claude Haiku 4.5 — tarea simple y de alto volumen, no necesita razonamiento profundo.

**Modo de actuación**:
- ✅ **Autónomo completo** — enviar el mensaje no tiene riesgo transaccional ni compromete precio.
- ❌ **Nunca ofrece un descuento no autorizado**. Si el turista responde pidiendo uno, el agente deriva al Travel Concierge o a un humano — no existe motor de cupones que pueda validar/aplicar ese descuento hoy.

**Costo estimado**: volumen bajo y mensajes cortos ≈ **$1–2/mes**.

---

### Agente 4 — Operator Support

**Rol equivalente humano**: el diseñador/redactor que un operador pequeño no puede pagar, y el analista de pricing que Tourya no tiene.

**Canal**: Web / App.

**Cuándo actúa**: durante el wizard de creación/edición de tour (`TourFormPage`, ~6 pasos: básico, direcciones, atracciones, incluye/no incluye, itinerario, FAQ, galería), cuando se detecta baja disponibilidad o precio desalineado, y cuando llega una reseña nueva (`review`).

**Input que recibe**:
- Detalle del tour (`Tour`): nombre, categoría, subcategoría, `isUnlimitedCapacity`, tipo de precio, duración, edad mínima y horario.
- Fotos y datos básicos que el operador sube.
- Catálogo de tours similares (misma categoría/zona) para pricing assist.
- Reseña nueva + `ReviewAttachment` (fotos).

**Output que produce**:
1. Descripción atractiva y SEO-friendly en español (`Tour.name`/`description`, obligatorio `es` por RN-011), tags sugeridos (`tour_tag_mapping`).
2. Validación de galería **antes** del upload real: máximo 7 imágenes, máximo 5 MB, formato horizontal, ancho recomendado 1920px (RN-013) — evita que el operador suba algo que el backend rechazará.
3. Traducción (a inglés y portugués de Brasil) de los campos: descripción, atracciones principales, incluye, excluye, itinerario y preguntas frecuentes. Se debe garantizar que la información en estos idiomas se guarde en base de datos.
4. Alerta de precio desalineado vs. tours comparables (`providerPrice`) — **solo alerta, nunca cambia el precio**: el operador siempre decide su `providerPrice` (RN-014).
5. Borrador de respuesta a la reseña — el operador aprueba o edita antes de publicar.

**Modelo usado**: **Gemini 2.5 Pro** para redacción y tags (backend cerrado 2026-08-14 con Vertex AI en vez del Sonnet 5 del diseño original — mismo motivo que IA-02: reusa infra GCP + ~40% más barato). Para la traducción es→en/pt, delegar a **Google Cloud Translation** cuando se implemente (IA-09) — el `OperatorSupportService` ya deja el TODO listo (`// TODO IA-09`), es 1 wire-up cuando el service exista.

**Modo de actuación**:
- ✅ **Genera borrador** de descripción/tags — el operador aprueba antes de `PUT /tour/user/submitTourById/{id}`.
- ⚠️ **Solo alerta** en pricing, nunca modifica `providerPrice`.
- ✅ **Genera borrador** de respuesta a reseña vía `PATCH /public/save/review/{reviewId}` — el operador lo aprueba, nunca se publica solo.

**Estado backend**: 🟢 cerrado 2026-08-14 (IA-07). 4 endpoints action-specific bajo `/agents/operator-support/*`:
- `POST /suggest-tour-content` — nombres + descripción SEO + tags (Gemini 2.5 Pro, 1 call).
- `POST /price-alert/{tourId}` — severity + rango comparables (Gemini 2.5 Pro, 1 call; heurística fallback si el LLM devuelve JSON inválido).
- `POST /draft-review-reply/{reviewId}` — borrador + tono + idioma (Gemini 2.5 Pro, 1 call).
- `POST /validate-gallery` — validación pre-upload (sin LLM, reusa reglas de `GalleryValidator`, cero costo).

Guardrails idénticos a IA-02 (deny-list secretos + scrub `providerPrice`/`slotPercentageTourya` + budget guard + audit siempre). Autorización owner-based: el `PROVIDER`/`PROVIDER_OPERATOR` solo opera sobre tours/reseñas de su propio provider — `requireTourOwnership` valida antes de cualquier call al LLM.

**Traducción es→en/pt DEFERRED a IA-09**: el `OperatorSupportService` no la implementa (RN-011 solo exige `es`); cuando IA-09 exista se agrega 1 método que delega en `ITranslationService` — ya hay `// TODO IA-09` marcado en el código.

**Costo estimado con Gemini**: con la meta de 150 tours en 12 meses (creación + ediciones) y ~15% de reservas que dejan reseña (RN meta) ≈ **$1–2/mes** (baja vs los $2–4 originales del brief con Sonnet 5).

---

### Agente 5 — Backoffice Support

**Rol equivalente humano**: el analista de compliance que hoy Tourya no tiene contratado — el equipo de operaciones es 1 persona (01-vision-y-negocio).

**Canal**: Web / App (backoffice).

**Cuándo actúa**:
- Cuando un `RequestProvider` pasa a `SUBMITTED`, antes de que ADMIN decida `pre-approve`/`approve`.
- Cuando un `Tour` pasa a `SUBMITTED`, antes de `acceptTourById`.
- Para generar el borrador del manifiesto DIMAR (`MaritimActivityReport`) a partir de reservas confirmadas.
- En la conciliación de `ProviderPayoutOrder` vs `AccountPayable`.

**Input que recibe**:
- `RequestProviderGallery` (documentos obligatorios de RN-045: RUT, RNT vigente, certificación bancaria, cédula del representante legal, Cámara de Comercio, pólizas vigentes).
- Datos del `Tour` a validar (¿tiene español obligatorio? ¿galería cumple RN-013? ¿política de cancelación definida?).
- Reservas `CONFIRMED`/`DELIVERED` del día/operador para el manifiesto.
- `AccountPayable` vs `ProviderPayoutOrder` para detectar anomalías (RN-042: `amount al operador = providerPrice × quantity`).

**Output que produce**:
1. Expediente KYB pre-verificado: checklist ✅/❌ por documento obligatorio, listo para que ADMIN decida.
2. Pre-validación de tour: lista de incumplimientos (falta español, galería con fotos verticales, política de cancelación sin definir) antes de que ADMIN lo revise.
3. Borrador de manifiesto DIMAR (pasajeros por zarpe) a partir de reservas confirmadas — listo para que alguien lo revise y suba (mismo patrón 100% manual de hoy, RN-054).
4. Alerta de anomalía en pagos/comisiones — nunca corrige, solo señala.

**Modelo usado**: Claude Sonnet 5, incluyendo la lectura de documentos KYB (Sonnet 5 es multimodal — lee imágenes y PDF directamente, sin necesitar un proveedor de visión aparte).

**Modo de actuación**:
- ✅ **Genera el expediente/checklist** — **nunca aprueba**. RN-010 y RN-046 reservan la aprobación de KYB y tours exclusivamente a ADMIN, por ser decisión regulatoria.
- ✅ **Genera borrador de manifiesto DIMAR** — humano revisa y sube, igual que hoy (RN-054 es 100% manual).
- ⚠️ **Marca anomalías de payout** — nunca las corrige. Un falso positivo automatizado sobre el dinero de un operador es un riesgo que no vale la pena tomar.

**Costo estimado**: al ritmo de la meta de 70 operadores en 12 meses (partiendo de 3 hoy) y 150 tours ≈ **$1–2/mes**.

---

## Tabla resumen

| Agente | Modelo | Autónomo? | Canal | Depende de | Costo/mes estimado (meta 12m) |
|---|---|---|---|---|---|
| Travel Concierge | Haiku 4.5 + Sonnet 5 | Sí (búsqueda/carrito) | Web/App | Nada nuevo | ~$20–25 |
| Support 24/7 | Haiku 4.5 + Sonnet 5 | Sí (cancelación/reschedule dentro de política) | WhatsApp (Twilio) | ⚠️ Twilio Programmable Messaging | ~$5–7 |
| Desert Shopping Cart | Haiku 4.5 | Sí (mensaje) | WhatsApp/Email | ⚠️ Twilio (fallback: email ya existe) | ~$1–2 |
| Operator Support | Sonnet 5 | Borrador, operador aprueba | Web/App | Nada nuevo | ~$2–4 |
| Backoffice Support | Sonnet 5 | Borrador, ADMIN aprueba | Web/App | Nada nuevo | ~$1–2 |
| **Total estimado (inferencia LLM)** | | | | | **~$30–40/mes** |

> Margen sobre estimación: **3x** para presupuesto inicial ≈ **$100–120/mes**. El costo real de operar esta capa es marginal frente al costo de canal (Twilio + Meta) y de infraestructura (Cloud Run, Cloud SQL) — reevaluar al mes 3 con datos reales, igual que cualquier otro presupuesto de `app_config`.

> ✅ **Presupuesto aprobado Luis (2026-07-07)**: **$100–200 USD/mes** para inferencia LLM. Este rango cubre el estimado con margen 3× y permite absorber picos sin bloqueo. El costo de canal (Twilio + Meta) se contabiliza aparte.

---

## Agentes post-MVP (roadmap — no implementados)

Ya hay señales explícitas de esto en la documentación existente de Tourya:

### Agente 6 — Moderación de reseñas
📌 Ya está en el roadmap de RN-050: reintroducir `status = MODERATION` en `review`, un agente analiza spam/lenguaje ofensivo/enlaces sospechosos/patrones de fraude antes de `PUBLISHED`. Modelo sugerido: Haiku 4.5 (tarea de clasificación, alto volumen, bajo costo).

### Agente 7 — Payout automatizado con reglas
📌 Ya está sugerido en [03 — Roles y actores](03-roles-y-actores.md): *"evaluar si es viable que el pago lo ejecute un agente con reglas (ej. auto-aprobar hasta cierto monto, montos mayores requieren revisión humana)"*, cuando se integren las APIs de Wompi/Mercado Pago para payouts salientes (hoy 100% manual, RN-043).

### Agente 8 — Reconciliación de pagos huérfanos
Cuando se implemente el webhook server-side de Wompi (decisión ya tomada, pendiente de ejecución — ver RN-025 y Decisión 8 de [07 — Arquitectura técnica](07-arquitectura-tecnica.md)), un agente puede reconciliar automáticamente transacciones Wompi exitosas que no llegaron a confirmarse como `Payment` (el escenario de "pago cobrado, reserva en limbo" documentado como riesgo HIGH).

### Agente 9 — B2B / Convenios
Roadmap de 01-vision-y-negocio: QR de hotel → tour en Tourya → hotel recibe comisión. Un agente puede rastrear atribución y calcular la comisión del aliado automáticamente cuando este módulo exista.

### Agente 10 — Coordinador Autónomo (visión 2 años)
Meta-agente que orquesta a los demás, decide cuándo escalar, ajusta umbrales de autonomía según el override rate real. Requiere datos acumulados de los agentes 1-5 para entrenar políticas — no tiene sentido antes de tener volumen real.

---

## Arquitectura técnica de agentes

### Estructura de código

Se integra al monolito existente (Decisión 1 de 07-arquitectura-tecnica: Tourya es monolito Spring Boot, no microservicios), como un nuevo paquete junto a `controller/`, `services/`, `jobs/`:

```
com.tourya.api/
├── agents/
│   ├── shared/
│   │   ├── ILlmClient.java                    # Abstracción del proveedor (hoy solo Anthropic)
│   │   ├── AnthropicClient.java
│   │   ├── PromptTemplate.java                 # Plantillas versionadas
│   │   ├── AgentRunResult.java                 # sugerencia | acción | error
│   │   ├── BudgetGuard.java                    # Lee tope desde app_config
│   │   └── AgentAuditWriter.java               # Auditoría obligatoria (tabla nueva: agent_audit_log)
│   ├── travelconcierge/
│   │   ├── TravelConciergeAgent.java
│   │   └── prompts/                            # concierge.v1.txt
│   ├── support24x7/
│   ├── desertcart/
│   ├── operatorsupport/
│   ├── backofficesupport/
│   └── orchestrator/
│       └── AgentOrchestrator.java              # Decide qué agente actúa según evento
```

> ⚠️ Esta tabla `agent_audit_log` **no existe hoy** en el schema de 68 tablas ([08 — Modelo de datos](08-modelo-de-datos.md)) — es una migración nueva, prerequisito antes de dar autonomía real a cualquier agente (Principio rector #4).
>
> ✅ **Validación Franklin (2026-07-07)**: **es buena práctica y es necesaria**. Sin audit log estructurado no se puede:
> - **Depurar** (qué prompt produjo qué respuesta al operador X en la fecha Y).
> - **Trackear costo** por agente / usuario / entidad afectada.
> - **Iterar prompts** con datos (comparar override rate de v1 vs v2).
> - **Cumplir compliance** cuando el agente afecta reservas o pagos.
> - **Investigar anomalías** (¿por qué el agente 2 canceló 30 reservas ayer?).
>
> Es el pattern estándar en cualquier sistema de agentes que interactúa con transacciones reales. Ejecutar como migración temprana.

### Flujo de ejecución de un agente

```java
@Service
public class Support24x7Agent implements IAgent {

    private final ILlmClient llmClient;
    private final IAgentAuditWriter auditWriter;
    private final IBudgetGuard budgetGuard;
    private final ReservationService reservationService;

    public AgentRunResult<ReagendamientoResult> ejecutarReagendamiento(
            Long reservationId, String nuevoSlotId, String mensajeTurista) {

        // 1. Verificar presupuesto
        if (!budgetGuard.puedeEjecutar("support24x7")) {
            return AgentRunResult.rechazado("Presupuesto agotado mes actual");
        }

        // 2. Cargar contexto real (nunca inventado)
        Reservation reserva = reservationService.findById(reservationId);
        boolean disponible = reservationService.validarDisponibilidadSlot(nuevoSlotId);

        // 3. Construir prompt con datos reales
        var prompt = PromptTemplate.cargar("support24x7.v1");
        var input = prompt.construir(Map.of(
            "reserva", reserva,
            "nuevoSlotDisponible", disponible,
            "canReschedule", reserva.getCanReschedule(),
            "maxReschedulingDate", reserva.getMaxReschedulingDate(),
            "mensaje", mensajeTurista
        ));

        // 4. Llamar al LLM
        var start = Instant.now();
        var response = llmClient.complete(input, "claude-sonnet-5");
        var elapsedMs = Duration.between(start, Instant.now()).toMillis();

        // 5. Parsear output estructurado
        var resultado = JsonMapper.leer(response.content(), ReagendamientoResult.class);

        // 6. Auditar SIEMPRE, sin excepción
        auditWriter.registrar(AgentAuditEntry.builder()
            .agente("Support24x7")
            .modelo(response.model())
            .promptVersion("v1")
            .inputTokens(response.inputTokens())
            .outputTokens(response.outputTokens())
            .costo(response.costo())
            .duracionMs(elapsedMs)
            .idEntidadAfectada(reservationId)
            .resultadoTipo(resultado.requiereCobroAdicional() ? "sugerencia" : "accion_autonoma")
            .build());

        // 7. Ejecutar solo si la regla de negocio lo permite sin ambigüedad
        if (!resultado.requiereCobroAdicional()) {
            reservationService.confirmarReagendamiento(reservationId, nuevoSlotId);
            return AgentRunResult.accionAutonoma(resultado);
        }
        return AgentRunResult.sugerencia(resultado); // caso 3 de RN-033: requiere humano/turista
    }
}
```

### Versionado de prompts

- Cada prompt es un archivo versionado (`support24x7.v1.txt`, `.v2.txt`...). Al cambiar uno, se crea `vN+1` sin borrar `vN`, se rolea con feature flag (10%/90%), y se compara la tasa de override humano antes de un rollout completo.

### Tests de agentes

- **Unit tests**: construcción de prompt, parsing de output, budget guard — deterministas, sin LLM real.
- **Tests con LLM mockeado**: `ILlmClient.complete` retorna respuestas pre-grabadas (golden files) que cubren los 3 casos de RN-033 y los estados de RN-031.
- **Tests con LLM real** (semanal en staging): conjunto fijo de reservas de prueba → comparar output vs esperado, alertar si diverge.

---

## Observabilidad de agentes

Tourya ya usa **Cloud Logging + Cloud Monitoring** (GCP, ver [13 — Despliegue](13-despliegue-cicd.md)) — no hace falta una herramienta nueva, solo un dashboard adicional sobre la misma infraestructura:

| Métrica | Alerta si... |
|---|---|
| Tasa de uso por agente | Cae a 0 (algo se rompió) |
| Latencia P95 | > 5 s |
| Tasa de error | > 5% |
| Costo diario por agente | > 10x la media histórica |
| Tasa de override humano | > 30% (revisar el prompt) |
| % del presupuesto mensual consumido | > 80% |

✅ **Validación Franklin (2026-07-07)**: **es necesario mantener este punto** — no se elimina. Estamos por salir a producción con dinero real (pagos Wompi, payouts a operadores). Sin alertas básicas de Cloud Monitoring (errores 5xx, latencia P95, agotamiento de DB connections, cost anomaly) el equipo se entera de los problemas por reclamos del turista, no antes.

Configurar alertas generales del backend es prerequisito de higiene operativa **antes** de sumarle alertas específicas de agentes IA. Costo de Cloud Monitoring básico: prácticamente cero para el volumen inicial.

---

## Privacidad y seguridad

Estas reglas se apoyan directamente en vulnerabilidades **ya documentadas** en [12 — Seguridad y autenticación](12-seguridad-y-auth.md) — los agentes no deben agravarlas:

- **PII nunca sale sin enmascarar**: `payment.payer_name/email/phone/document` está guardado en texto plano hoy (tabla `payment`). Ningún agente debe recibir estos campos completos en su prompt — solo lo estrictamente necesario, y el documento de identidad enmascarado.
- **Nunca se envían secretos al LLM**: `WOMPI_INTEGRITY_SECRET` y `JWT_SECRET` (vulnerabilidades C-1/C-2) no deben aparecer en ningún prompt ni log de agente, bajo ninguna circunstancia.
- **Datos de tarjeta**: nunca los ve ningún agente — Wompi los tokeniza, Tourya nunca los almacena (ya es así hoy).
- **Documentos KYB** (cédula, RUT): cuando el Agente 5 los procese vía Sonnet 5, deben viajar por el mismo pipeline cifrado que hoy usa GCS — nunca guardar el contenido crudo del documento en el log de auditoría del agente, solo el resultado del checklist.
- **Opt-out de retención de logs** con Anthropic, para que las conversaciones de soporte y los documentos KYB no queden retenidos del lado del proveedor.
- **Cifrado en tránsito**: TLS con el proveedor, igual que cualquier otra integración externa de Tourya.

---

## Roadmap de agentes

Priorizado según dependencias reales (no sprints ficticios) y el estado actual del proyecto (3 operadores, 20 tours, 0 turistas registrados, aún pre-producción):

| Fase | Agentes a habilitar | Por qué en ese orden |
|---|---|---|
| **Fase 0** | Travel Concierge + Operator Support + Support 24/7 + Desert Shopping Cart + Backoffice Support| Travel Concierge + Operator Support sin dependencias externas — ayudan a los 3 operadores iniciales a cargar tours completos y ayudan a convertir a los primeros turistas, Support 24/7 + Desert Shopping Cart bloqueados hoy por integración no implementada (11-integraciones.md), Backoffice Support se puede iniciar ya para ayudar a los operadores |
| **Fase 1** (post-MVP, cuando el GMV meta de 12 meses esté cerca) | Moderación de reseñas IA, Payout automatizado, Reconciliación de pagos, B2B | Cada uno ya está señalado como roadmap explícito en la documentación existente — no son ideas nuevas, son ejecución de lo ya decidido |
| **Fase 2** (visión 2 años) | Coordinador Autónomo | Requiere datos acumulados reales de los agentes 1-5 |

📌 **priorizar** — priorizar la implementación de Twilio Programmable Messaging (WhatsApp) para los agentes que utilizan este canal para interactuar con los turistas. 

---

## Por qué Gemini (Vertex AI) en vez de Anthropic — decisión 2026-08-14

Cuando se cerró el framework `agents/shared/` (IA-01, 2026-07-16) el default fue Anthropic — era la implementación de referencia del `ILlmClient` y Franklin todavía no había habilitado Vertex AI en `tourya-project-dev`. Al llegar IA-02 (implementación del primer agente real, Travel Concierge) Franklin reevaluó el proveedor y cambió a **Vertex AI Gemini**. Tres razones concretas:

1. **Reusa infraestructura GCP existente**. `tourya-project-dev` ya tiene la service account `tourya-dev-cloud-run` con `roles/aiplatform.user`. La autenticación es Application Default Credentials — en Cloud Run automática por la SA, en local dev con `gcloud auth application-default login` que Franklin ya corría para otras cosas. Cero cuenta nueva de proveedor externo, cero API key nueva que rotar en Secret Manager, cero superficie de compliance adicional. Anthropic exigía crear la cuenta, generar la key, subirla a Secret Manager (`tourya-anthropic-api-key`), inyectarla como env var en Cloud Run — todo pasos adicionales sin valor incremental.
2. **~40% más barato para el mismo volumen**. Gemini 2.5 Pro cuesta $1.25 input / $10 output por 1M tokens; Claude Sonnet 5 cuesta $2 / $10 (precio introductorio, sube a $3 / $15 el 1-sep-2026). Con 2.5 Flash disponible a $0.075 / $0.30 (contra Haiku 4.5 a $1 / $5) el ahorro es aún mayor cuando entren clasificaciones de intención de alto volumen. Para el presupuesto aprobado por Luis ($100-200/mes) esto significa más margen para experimentar con prompts sin agotar cuota.

El framework `agents/shared/` estaba diseñado desde IA-01 para ser provider-agnostic vía `ILlmClient`. Cambiar de proveedor fue estrictamente aditivo: se creó `GeminiClient implements ILlmClient` como nueva clase junto a `AnthropicClient`, y un `LlmClientConfig` con `@Primary` selector por `agents.provider`. `AnthropicClient` queda intacto — si mañana Franklin quiere hacer A/B testing o volver a Anthropic basta con `AGENTS_PROVIDER=anthropic` + subir la key al Secret Manager. La decisión es completamente reversible sin tocar lógica de agente.

---

## Referencias

- [01 — Visión y negocio](01-vision-y-negocio.md)
- [05 — Reglas de negocio](05-reglas-de-negocio.md)
- [14 — Web vs Mobile](14-gap-web-mobile.md)
