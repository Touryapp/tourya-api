# 01 — Visión y negocio

> ✅ Este documento fue consolidado con los aportes de Luis Mendoza (PO) el 2026-06-28. Los puntos que quedaron abiertos están marcados como 📌 PENDIENTE LUIS.

---

## Qué es Tourya

Tourya es una **plataforma marketplace de experiencias turísticas**. Conecta:

- **Operadores turísticos** (proveedores con RNT — Registro Nacional de Turismo) que publican tours y experiencias.
- **Turistas** (usuarios finales) que buscan, reservan y pagan tours.

El sistema cobra una **comisión por transacción** sobre cada venta (`slotPercentageTourya`), que va para Tourya; el resto se gira al proveedor en un payout periódico.

### Componentes implementados

1. **Web app de turista** (Angular 19): buscar tours, ver detalles, validar disponibilidad, agregar al carrito, pagar con Wompi, usar créditos como parte de pago, cancelar o reagendar según políticas del tour, ver reservas, dejar reseñas, transferir créditos.
2. **Panel del proveedor** (Angular 19): crear tours, definir horarios y precios (adulto, niño, bebé y "cualquiera") por tour, gestionar reservas, ver pagos, responder reseñas, gestionar sub-usuarios (operarios).
3. **App móvil Android** (.NET MAUI 10): explorar tours, carrito, checkout (pago con Wompi), reservas, perfil, panel proveedor (incluyendo escáner QR para fulfillment).
4. **Backoffice** (Angular 19): aprobar proveedores (KYB), aprobar tours, ajustar % comisión Tourya, ver reportes, gestionar reportes marítimos DIMAR, subir evidencia de pago a proveedores.

---

## Mercado objetivo

- **Geografía**: **Colombia — foco inicial en San Andrés Islas**. Después se explorarán otros sitios turísticos de Colombia y de Latinoamérica.
- **Estrategia**: entrar a destinos turísticos donde no estén operando localmente otras plataformas. Tourya busca ser la solución donde los turistas adquieren tours y otros productos y servicios.
- **Idiomas soportados**: español (default), inglés, portugués de Brasil.
- **Perfil del turista**: en San Andrés los turistas son normalmente de **Colombia, Argentina, Brasil, Perú, Chile y Ecuador**. En menor proporción vienen de Estados Unidos, Canadá y Europa.
- **Tipo de operadores**: nicho marítimo / acuático (justifica el módulo DIMAR / Maritime Activity Reports), y categorías generales: Aventura, Cultural, Terrestre, alquiler de transporte, Nocturno, deportes, etc.
- **Turista ideal**: personas que están cómodas adquiriendo productos y servicios desde su teléfono móvil y que no tienen problema en pagar una comisión por el servicio y la comodidad de la plataforma.
- **Convenios B2B**: hoy no hay convenios firmados. A corto plazo se planea habilitar esta funcionalidad para aumentar ventas: alianzas con hoteles y negocios de alto tráfico, ganando comisión por cada transacción generada por su recomendación (ej. QR del hotel → tour en Tourya → hotel recibe % por la venta).

---

## Visión a futuro

### Horizonte 6 meses
Validar el modelo y hacer los ajustes necesarios para que sea rentable y operable con **una sola persona** (eficiente), apoyada por IA.

### Horizonte 1 año
Habilitar servicios adicionales:
- Servicio de Transporte.
- Domicilios (Licor, Comida, Farmacia — productos básicos sin receta médica).
- Hospedaje.
- Souvenirs.

### Horizonte 2 años
Modelo probado en otros destinos turísticos de Colombia y Latinoamérica.

### Métricas críticas a monitorear
- CAC (costo de adquisición de usuario).
- Tasa de conversión.
- Tasa de abandono de carritos.
- Tasa de compras recurrentes.
- Métricas de eficiencia de los proveedores registrados.

### Módulos planeados (roadmap producto)

| Módulo | Descripción |
|--------|-------------|
| **Marketplace B2B** | Empresas ofrecen beneficios a sus colaboradores |
| **Reservas de hoteles** | Módulo dedicado |
| **Fidelización avanzada** | Programa de puntos y beneficios para turistas |
| **Módulo Hot Sale / Black Friday** | Estilo InDriver: turista publica lo que busca para hoy, operadores ofertan, turista da clic a la oferta que quiere |
| **Publicidad** | Espacios pagos para negocios locales |
| **Duty Free / Click & Collect** | Negocios publican promociones, turista compra, recibe QR, retira en sitio |
| **Herramienta de gestión de reservas** | Para operadores sin sistema: calendario, integración con Viator/Airbnb/Booking/GetYourGuide, reservas manuales, cierre de caja |

---

## Problema que Tourya resuelve

Sin Tourya, un **operador turístico tradicional** en Colombia tiene que:
- Recibir reservas por WhatsApp / llamadas / correo.
- Recibir ventas vía agencias turísticas o vendedores ambulantes.
- Gestionar disponibilidad en agenda manual o Excel.
- Cobrar por transferencia bancaria / efectivo.
- Cumplir DIMAR sin sistema integrado (cuando aplica).
- Sin visibilidad real de su cartera de turistas ni de su flujo de caja.

Sin Tourya, un **turista** que busca experiencias en Colombia:
- Tiene que buscar por separado en Instagram / Booking / TripAdvisor / Viator / agencias locales.
- No tiene forma de comprar un tour sin muchos intermediarios.
- No tiene forma estandarizada de comparar precios, reseñas u horarios.
- No tiene un canal de pago seguro (Wompi o Mercado Pago).
- No tiene forma simple de cambiar / cancelar la reserva si llueve.

### Lo que Tourya resuelve

| Para el operador | Para el turista |
|------------------|-----------------|
| Publicación de tours con multimedia (galería) | Búsqueda con filtros (categoría, fecha, precio, duración, tags) |
| Plantillas de horarios reutilizables | Reservas online con confirmación inmediata |
| Disponibilidad automática (capacity tracking) | Pago con tarjeta vía Wompi |
| Cobro estructurado (con comisión transparente) | Códigos QR para asistencia |
| Sub-usuarios / operarios (PROVIDER_OPERATOR) | Cancelación con políticas claras → crédito automático |
| Reportes DIMAR para actividades marítimas | Reseñas con fotos |
| Payouts semanales (**martes y viernes**) | Reschedule de reservas |
| Panel de reservas + estadísticas | Wishlist de tours favoritos |
| Reseñas con respuestas del operador | Transferencia de créditos a otros turistas |

---

## Modelo de negocio

### Cómo cobra Tourya

Modelo actual implementado: **comisión por transacción**.

```
providerPrice       = lo que el operador define que quiere recibir
slotPercentageTourya = % comisión asignada por Backoffice (puntos, ej. 15 = 15%)
price (venta)        = providerPrice × (1 + slotPercentageTourya / 100)

Ejemplo:
  providerPrice = $100.000
  slotPercentageTourya = 15
  price = $115.000

El turista paga $115.000 → Tourya retiene $15.000 → Operador recibe $100.000 (en payout)
```

Características del modelo:
- La comisión es **configurable por slot/día** (no es fija a nivel plataforma). Esto permite negociar tarifas por operador o por tour específico.
- Existen **overrides puntuales** (`TourSchedulePriceOverride`, `TourScheduleSlotOverride`) para ajustar precio o porcentaje de un slot en una fecha (ej. promociones, surge pricing).
- El operador **NUNCA** ve el `slotPercentageTourya` ni el `price` final: solo ve su `providerPrice`.

### Cómo paga Tourya al operador

**Cronograma de payouts**: los pagos se hacen los **martes y viernes**. El Job (`ProviderPayoutOrderJob`) corre los lunes y jueves a las 7:00 AM (Bogotá) para generar las órdenes de pago:
- **Lunes 7:00 AM**: agrupa reservas ejecutadas jueves a domingo de la semana anterior → paga el martes.
- **Jueves 7:00 AM**: agrupa reservas ejecutadas lunes a miércoles → paga el viernes.

Una reserva entra al payout cuando:
1. Ya pasó la fecha del tour (`reservation_date`).
2. Han pasado **2 días** desde la ejecución (`payout_available_date = reservation_date + 2 días`) — buffer para reclamos/disputas.
3. La reserva fue marcada como `DELIVERED` (escaneada por QR o confirmada por el operador).

**Buffer configurable (pendiente)**: hoy los 2 días están hardcoded. Debe volverse **configurable** para poder adaptarlo según exigencias del mercado.

**Transferencia manual**: hoy el backoffice hace la transferencia manualmente usando las opciones que ofrezcan las pasarelas (Wompi / Mercado Pago) — bancos, Nequi, Bre-B. Luego sube el comprobante y marca como `PAID`. **Roadmap**: automatizar dichos pagos usando las APIs de las pasarelas.

### Otras formas de monetización (roadmap)

| Iniciativa | Descripción |
|------------|-------------|
| **Tours destacados / promoción pagada** | Featured tours en home/search. Planeado para v2 del marketplace |
| **Herramienta de Gestión de Reservas** | Para operadores sin sistema: calendario multi-tour/multi-hospedaje, integraciones (Viator, Airbnb, Booking, GetYourGuide), reservas manuales, envío por correo, asignación de recursos, reportes financieros (ingresos por canal, ocupación promedio, cierre de caja) |
| **Publicidad para negocios locales** | Videos en 3 lugares: (a) al iniciar app (máx 5s, con skip); (b) email post-checkout con QR; (c) pestaña "Guía de la isla" tipo TikTok con botón directo a WhatsApp |
| **Duty Free / Click & Collect** | Negocios publican promociones → el turista compra → recibe QR → recoge en sitio → el negocio escanea el QR → Tourya cobra comisión |

---

## Estado actual (junio 2026, previo a producción)

- **Operadores**: 3 operadores turísticos negociados, incluyendo el más grande de la isla.
- **Tours publicados**: ~20 (una vez Tourya salga en producción).
- **Turistas registrados**: 0.
- **Reservas / GMV / comisión**: 0 (aún no productivo).

---

## Diferencia vs alternativas

| Hoy el turista usa... | Limitación | Tourya |
|-----------------------|------------|--------|
| WhatsApp / llamada al operador | Sin reservas confirmadas, sin pago seguro | Reserva con pago integrado y QR |
| Booking / TripAdvisor / Viator | Foco en hotelería; tours como "complemento", poca cobertura local | Foco 100% en experiencias colombianas |
| Instagram / búsqueda en redes | Sin estructura, sin comparación, sin reseñas reales | Estructura, reseñas verificadas, comparación |
| Agencia tradicional | Comisión alta, intermediación lenta | Marketplace directo operador ↔ turista |
| Excel / agenda en papel (operador) | Sin disponibilidad real, conflictos de booking | Capacity tracking automático, multi-operador |

---

## Métricas y metas 12 meses

| Métrica | Hoy | Meta 12 meses |
|---------|-----|---------------|
| Operadores activos | 3 (al salir productivo) | **70** |
| Tours publicados | 20 (al salir productivo) | **150** |
| Turistas registrados | 0 | **500** |
| Reservas/mes | 0 | **300** |
| GMV mensual | 0 | **$400.000.000 COP** |
| Comisión bruta Tourya/mes | 0 | **$47.000.000 COP** |
| Tasa de cancelación | 0 | 18% |
| NPS turistas | 0 | 55 |
| NPS operadores | 0 | 30 |
| % reservas que dejan reseña | 0 | 15% |
| % tours con galería completa | 0 | 85% |

---

## Roadmap de producto

### Prioridades inmediatas (para salir a producción)
- Terminar el **mínimo producto viable** y **salir en productivo**.
- Publicar Tourya para que los 3 operadores puedan cargar sus tours.

### Funcionalidades parcialmente implementadas (vale la pena terminar)
- **Traducción automática de tours** (es → en, pt) — propuesta en `traduccion-automatica-tours.md`. El operador ingresa en español y la plataforma debe traducir.
- **Login social sin Firebase** (eliminar lock-in + corregir vulnerabilidad) — propuesta en `social-login-google-facebook.md`.
- **Mobile iOS** — hoy solo Android; el código MAUI lo soportaría con poco trabajo. Tanto Android como iOS deben brindar la misma funcionalidad que la web.
- **Mejora del backoffice**:
  - Actualmente permite: aprobar proveedores, aprobar tours, ver reservas, ver reseñas, subir comprobantes de pago.
  - Falta: **dashboard financiero** (GMV, net revenue estimado, cuentas por pagar, CAC), **dashboard operativo** (tasa de conversión, tasa de cancelación, tours de alto riesgo, tiempo de resolución de soporte) y **gestor de disputas**.

### Riesgos / tech debt a atender
- ⚠️ Vulnerabilidades de seguridad documentadas en `security-remediation-plan.md` (5 CRITICAL, 7 HIGH).
- ⚠️ CI/CD sin coverage gate ni análisis de seguridad — plan en `cicd-improvement-plan.md`.
- ⚠️ `tourya-mobile` no está en GitHub (solo local) — pérdida de respaldo.

---

## Reglas de comisión y modelo comercial

- **Comisión por tour**: es un porcentaje por tour, definido a discreción del backoffice de Tourya.
- **Modelo Freemium / Pro / Enterprise**: por ahora **no**, se está revisando la posibilidad.
- **Tours destacados / suscripción del operador**: en revisión.
- **API pública para terceros / convenios B2B**: no hay hoy, en revisión.

---

## Cumplimiento legal

- Los operadores deben tener **RNT** vigente y **pólizas vigentes** para poder publicar sus tours en Tourya.
- **DIMAR**: los operadores que operan en el mar deben cumplir DIMAR. Tourya asiste con reportes internos (`MaritimActivityReport`) para justificar cancelaciones por mal tiempo.
- 📌 PENDIENTE LUIS — validar cumplimiento adicional: privacidad (habeas data / política de tratamiento de datos), facturación electrónica DIAN.

---

## Equipo

- Producto: 1 persona.
- Desarrollo: 1 persona.
- Operaciones: 1 persona.

📌 PENDIENTE LUIS — validar plan de crecimiento del equipo cuando la operación escale.
