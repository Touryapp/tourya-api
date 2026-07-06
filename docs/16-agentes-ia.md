# 16 — Agentes IA

> 📌 **PENDIENTE LUIS** — este documento fue iniciado por Luis Mendoza (commit `3e8c869`, 2026-07-06). El contenido detallado del alcance, arquitectura, prompts base, y casos de uso de cada agente lo aporta Luis.
>
> Este esqueleto queda como estructura sugerida para facilitar la carga del contenido. Sentirse libre de reorganizar según convenga.

---

## Contexto

Tourya planea incorporar **agentes de IA** que asistan al usuario final y a la operación interna. La visión a mediano plazo es que la plataforma pueda operar con **una sola persona** apoyada por IA, y a largo plazo evolucionar hacia un "autopiloto" que ejecute decisiones autónomamente en ciertos flujos.

Referencias en otros documentos:
- [01 — Visión y negocio](01-vision-y-negocio.md) — visión de eficiencia con IA.
- [14 — Web vs Mobile](14-gap-web-mobile.md) — menciona el **Travel Concierge** en el alcance del turista.
- [05 — Reglas de negocio](05-reglas-de-negocio.md), RN-050 — agente IA de **moderación de reseñas** en el roadmap.

---

## Agentes identificados hasta ahora

> Los que ya se han mencionado en otros documentos. Falta que Luis complete detalles y agregue los que faltan.

### 1. Travel Concierge (turista)

Asistente de búsqueda para el turista. Referenciado en el doc 14 como parte del alcance funcional de la app móvil.

**Alcance sugerido**:
- Búsqueda conversacional de tours ("quiero algo tranquilo para hacer con mi pareja mañana").
- Recomendación basada en preferencias, fecha, presupuesto.
- Consultas rápidas (qué llevar, cómo llegar).

📌 PENDIENTE LUIS — completar alcance detallado, prompts base, modelo elegido, guardrails.

---

### 2. Moderador de reseñas

Referenciado en el doc 05 (RN-050) como reemplazo del flujo actual (`PUBLISHED` sin moderación).

**Alcance sugerido**:
- Al crear una reseña → `status = MODERATION`.
- Agente analiza texto: spam, lenguaje ofensivo, enlaces sospechosos, patrones de fraude.
- Si pasa → `PUBLISHED`; si no → `CANCELED` (con razón registrada).

📌 PENDIENTE LUIS — completar reglas de decisión, modelo, umbrales de confianza, ejemplos.

---

### 3. Otros agentes propuestos por Luis

📌 PENDIENTE LUIS — completar la lista de agentes que Luis tenía definidos en su documento local.

Sugerencia de estructura por agente:

- **Nombre y rol**
- **Usuario que atiende** (turista, operador, backoffice, interno)
- **Alcance / responsabilidades**
- **Human-in-the-loop** (¿decide autónomamente o requiere confirmación humana?)
- **Modelo IA sugerido**
- **Prompt base**
- **Guardrails y límites**
- **Métricas de éxito**
- **Costo estimado por interacción**

---

## Consideraciones transversales

📌 PENDIENTE LUIS — Luis puede completar:

- **Presupuesto de IA**: cuánto se está dispuesto a invertir mensualmente.
- **Elección de proveedor**: OpenAI, Anthropic, Gemini, mix, local.
- **Manejo de PII y compliance**: qué datos se pueden enviar a modelos externos y cuáles no.
- **Fallback**: qué pasa si el agente falla o el proveedor está caído.
- **Feedback loop**: cómo se mejoran los agentes con datos reales.

---

## Roadmap

📌 PENDIENTE LUIS — priorización y horizonte de los agentes.

---

## Referencias

- [01 — Visión y negocio](01-vision-y-negocio.md)
- [05 — Reglas de negocio](05-reglas-de-negocio.md)
- [14 — Web vs Mobile](14-gap-web-mobile.md)
