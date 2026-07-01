# 01 — Visión y negocio

> ⚠️ **La mayor parte de este documento está marcado como 📌 PENDIENTE LUIS**. Lo que aparece aquí son deducciones del código y del dominio. Luis (PO) debe validar / corregir / completar la visión y los números.

---

## Qué es Tourya (deducido del código)

✅ Tourya es una **plataforma marketplace de experiencias turísticas**. Conecta:
- **Operadores turísticos** (proveedores con RNT — Registro Nacional de Turismo) que publican tours y experiencias.
- **Turistas** (usuarios finales) que buscan, reservan y pagan tours.

El sistema cobra una **comisión por transacción** sobre cada venta (`slotPercentageTourya`), que va para Tourya, y el resto se gira al proveedor en un payout periódico.

### Componentes implementados

✅ Confirmado en código:

1. **Web app de turista** (Angular 19): buscar tours, ver detalles, valida disponibilidad del tour y si hay disponibilidad permitir agregar el tour al carrito, pagar con Wompi, utilizar creditos como parte del pago, ver reservas, dejar reseñas, Reagendar o cancerlar de acuerdo a politicas del tour, transferir créditos.
2. **Panel del proveedor** (Angular 19): crear tours, definir horarios y precios (adulto, niño, bebe, cualquiera) por cada tour, gestionar reservas, ver pagos, responder reseñas, gestionar sub-usuarios (operadores).
3. **App móvil Android** (.NET MAUI 10): explorar tours, carrito, checkout, reservas, perfil, panel proveedor (incluyendo escáner QR para fulfillment).
4. **Backoffice** (Angular 19): aprobar proveedores (KYB), aprobar tour de proveedores, ajustar % comisión Tourya, ver reportes, gestionar reportes marítimos DIMAR, gestionar pagos a proveedores.

---

## Mercado objetivo

- **Geografía**: Colombia inicialmente (mucha evidencia: campo RNT, soporte DIMAR, departamentos colombianos en la BD, Wompi como pasarela, tourya.co como dominio). luego validaremos que otros paises podemos llevar el modelo de tourya
- **Idiomas soportados**: español (default), inglés, portugués de Brasil. esto debido a que en la isla vienen principalmente turistas colombianos pero tambien bienen de latinoamerica (brasil y argentina principalmente) y otros de EEUU y europa.
- **Tipo de operadores**: nicho marítimo / acuático visible (módulo "Maritime Activity Reports" para DIMAR), pero también categorías generales (Aventura, terrestre, Cultural, alquiler de transporte, Nocturno, Deportes, etc.).
- **Ciudad donde opera Tourya**: Tourya inicialmente va a operar en la isla de san andres (Colombia). Luego de que el modelo ya este probado y listo para escalar se evaluaran otros lugares de latinoamerica.
- **Foco inicial**: ciudades turisticas con mucha afluencia de turistas y que en esa ciudad no tengan apps locales para venta de tours y otros servicios requeridos por los turistas.
- **Planes de expansión**: Se podría evaluar llevar a Tourya otros paises como panama, republica dominicana y mexico. esto siempre y cuando las ciudades de esos paises cumplan con las caracteristicas de nuestro foco inicial.
- **Turista ideal**: Turista que le guste hacer compras por internet y/o teléfono móvil, y que no tenga problemas con pagar una comisión por la comodidad de adquirir productos y/o servicios desde su teléfono móvil.
- **Convenios B2B**: Actualmente no hay convenios pero si necesitamos que la plataforma permita hacer convenios B2B que nos ayuden a aumentar nuestras ventas. ej que un hotel o crucero pueda ganar comisiones por cada transaccion realizada con el QR del establecimiento.

---

## Visión a futuro

- **Horizonte a 6 Meses**: Establecer el modelo de negocio de Tourya en la isla de san andres. esto incluye optimizar la operación de manera que podemos operar con solo una persona pero apoyado por Agentes y Asistentes IA. se revisará habilitar publicidad para aquellas empresas que deseen publicitar sus productos o servicios en nuestra apps.
- **Horizonte a 1 año**: habilitar nuevos servicios como transporte,Hospedaje(hoteles Boutique), domicilios (Licor, Comida a domicilios y farmacia) y venta de Souvenir en la isla de san andres.
- **Horizonte a 2 años**: llevar nuestro modelo de negocio a otros lugares de colombia y de latinoamerica.
- **Métricas críticas**: Costo de adquisicion de usuarios (CAC), Tasa de conversion, Tasa de cancelacion, Tasa de Abandono de Carrito, tasa pedidos no atendidos por un proveedor.
- **Módulos planeados**: Marketplace B2B (para empresas que deseen dar beneficios a sus trabajadores), sistema de reserva apra hoteles, Fidelizacion avanzada.

---

## Problema que Tourya resuelve hoy (deducido)

✅ Sin Tourya, un operador turístico tradicional en Colombia tiene que:
- Recibir reservas por WhatsApp / llamadas / correo.
- Gestionar disponibilidad en una agenda manual o Excel.
- Cobrar por transferencia bancaria / efectivo.
- Cumplir DIMAR sin sistema integrado (cuando aplica).
- Sin visibilidad real de su cartera de turistas ni de su flujo de caja.

✅ Y sin Tourya, un turista que busca experiencias en Colombia:
- Tiene que buscar por separado en Instagram / Booking / TripAdvisor / agencias locales.
- No tiene forma estandarizada de comparar precios / reseñas / horarios.
- No tiene un canal de pago seguro (Wompi).
- No tiene una forma simple de cambiar / cancelar la reserva si llueve.

### Lo que Tourya resuelve

✅ Visible en el código:

| Para el operador | Para el turista |
|------------------|-----------------|
| Publicación de tours con multimedia (galería) | Búsqueda con filtros (categoría, fecha, precio, duración, tags) |
| Plantillas de horarios reutilizables | Reservas online con confirmación inmediata |
| Disponibilidad automática (capacity tracking) | Pago con tarjeta vía Wompi |
| Cobro estructurado (con comisión transparente) | Códigos QR para asistencia |
| Operadores sub-usuarios (PROVIDER_OPERATOR) | Cancelación con políticas claras → crédito automático |
| Reportes DIMAR para actividades marítimas | Reseñas con fotos |
| Payouts semanales (lunes y jueves) | Reschedule de reservas |
| Panel de reservas + estadísticas | Wishlist de tours favoritos |
| Reseñas y responses | Transferencia de créditos a otros turistas |

---

## Modelo de negocio (deducido)

### Cómo cobra Tourya

✅ El modelo actual implementado es **comisión por transacción**:

```
providerPrice  = lo que el operador define que quiere recibir
slotPercentageTourya  = % comisión asignada por Backoffice (puntos, ej. 15 = 15%)
price (precio de venta)  = providerPrice × (1 + slotPercentageTourya / 100)

Ejemplo:
  providerPrice = $100.000
  slotPercentageTourya = 15
  price = $115.000

El turista paga $115.000 → Tourya retiene $15.000 → Operador recibe $100.000 (en payout)
```

✅ Características del modelo:
- La comisión es **configurable por slot/día** (no es fija a nivel plataforma). Esto permite negociar tarifas individuales con cada operador o por tour específico.
- Existen **overrides puntuales** (`TourSchedulePriceOverride`, `TourScheduleSlotOverride`) para ajustar el precio o el porcentaje de un slot específico (ej. promociones, surge pricing).
- El operador NUNCA ve el `slotPercentageTourya`, solo ve su `providerPrice` y el `price` final.

### Cómo paga Tourya al operador

✅ `ProviderPayoutOrderJob` corre dos veces por semana:
- **Lunes 7:00 AM (Bogotá)**: paga reservas ejecutadas Jueves-Sábado-Domingo de la semana anterior.
- **Jueves 7:00 AM (Bogotá)**: paga reservas ejecutadas Lunes-Martes-Miércoles.

Una reserva entra al payout cuando:
1. Ya pasó la fecha del tour (`reservation_date`).
2. Han pasado **2 días** desde la ejecución (`payout_available_date = reservation_date + 2 días`) — buffer para reclamos/disputas.
3. La reserva fue marcada como `DELIVERED` (escaneada por QR o confirmada por el operador).

El backoffice luego **sube el comprobante de pago** (`POST /provider/payout-orders/admin/{orderId}/proof`) y marca como `PAID`.

📌 PENDIENTE LUIS — confirmar:
- ¿El cronograma de payout es correcto (Mon/Thu)?
- ¿El buffer de 2 días es regla de negocio definida o configurable?
- ¿Tourya transfiere por banco / Bre-B / nequi? ¿Hay alguna integración bancaria, o es manual?

### Otras formas de monetización (potenciales)

❓ ASUNCIÓN — no implementadas hoy, pero el modelo de datos lo permite:

- **Tours destacados / promoción pagada**: featured tours en home/search.
- **Suscripción del operador**: free vs pro con más beneficios (mayor visibilidad, comisión menor, multi-usuario más operadores, etc.).
- **Servicios adicionales** (transfer, guía, traducción): hay una entidad `TouryaService` y `ServiceType` no completamente explotada.
- **Comisión por reschedule**: actualmente las reservas se pueden reagendar sin costo aparente.

📌 PENDIENTE LUIS — ¿alguno de estos está en roadmap?

---

## Clientes / Operadores hoy

📌 PENDIENTE LUIS.

Necesario saber:
- Número de operadores onboarded.
- Número de tours publicados.
- Número de turistas registrados.
- Reservas mensuales actuales.
- GMV mensual.
- Comisión bruta mensual de Tourya.

---

## Diferencia vs alternativas (deducido)

| Hoy turista usa... | Limitación | Tourya |
|--------------------|------------|--------|
| WhatsApp / llamada al operador | Sin reservas confirmadas, sin pago seguro | Reserva con pago integrado y QR |
| Booking / TripAdvisor | Foco en hotelería; tours como "complemento", poca cobertura local | Foco 100% en experiencias colombianas |
| Instagram / búsqueda en redes | Sin estructura, sin comparación, sin reseñas reales | Estructura, reseñas verificadas, comparación |
| Agencia tradicional | Comisión alta, intermediación lenta | Marketplace directo operador↔turista |
| Excel / agenda en papel (operador) | Sin disponibilidad real, conflictos de booking | Capacity tracking automático, multi-operador |

---

## Métricas críticas (a definir)

📌 PENDIENTE LUIS — definir KPIs:

| Métrica | Hoy | Meta 12 meses |
|---------|-----|----------------|
| # Operadores activos | ? | ? |
| # Tours publicados | ? | ? |
| # Turistas registrados | ? | ? |
| # Reservas/mes | ? | ? |
| GMV mensual | ? | ? |
| Comisión bruta Tourya/mes | ? | ? |
| Tasa de cancelación | ? | ? |
| NPS turistas | ? | ? |
| NPS operadores | ? | ? |
| % reservas que dejan reseña | ? | ? |
| % tours con galería completa | ? | ? |

---

## Roadmap de producto (a definir)

📌 PENDIENTE LUIS — definir.

Sugerencias basadas en lo que está en código vs. lo que falta:

### Funcionalidades parcialmente implementadas (vale la pena terminar)
- **Traducción automática de tours** (es → en, pt) — propuesta en `traduccion-automatica-tours.md`.
- **Login social sin Firebase** (eliminar lock-in) — propuesta en `social-login-google-facebook.md`.
- **Mobile iOS** (hoy solo Android) — el código MAUI lo soportaría con poco trabajo.

### Riesgos / tech debt a atender
- ⚠️ Vulnerabilidades de seguridad documentadas en `security-remediation-plan.md` (5 CRITICAL, 7 HIGH).
- ⚠️ CI/CD sin coverage gate ni análisis de seguridad — plan en `cicd-improvement-plan.md`.
- ⚠️ `tourya-mobile` no está en GitHub (solo local) — pérdida de respaldo.

---

## Preguntas para alinear con Luis

1. **Visión a 12-24 meses**: ¿hacia dónde va Tourya?
2. **Mercado**: ¿alcance geográfico hoy y meta?
3. **Modelo de comisión**: ¿% promedio? ¿es negociable por operador?
4. **Pricing strategy**: ¿hay tours destacados pagados? ¿planes para suscripción?
5. **B2B**: ¿hay convenios con hoteles / agencias? ¿API pública para terceros?
6. **Cumplimiento legal**: ¿más allá de DIMAR / RNT? (privacidad, facturación electrónica DIAN, etc.)
7. **Equipo**: ¿cuántas personas en producto, ingeniería, operaciones?
8. **Métricas actuales**: la lista de KPIs de arriba.
9. **Prioridades de producto**: ¿qué es lo siguiente más importante?

---

> Cuando Luis aporte la información, actualizar este documento y mover los `📌 PENDIENTE LUIS` a `✅`.
