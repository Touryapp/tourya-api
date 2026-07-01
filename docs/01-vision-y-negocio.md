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

1. **Web app de turista** (Angular 19): buscar tours, ver detalles, valida disponibilidad del tour y si hay disponibilidad permite agregar el tour al carrito, pagar con Wompi, utilizar los creditos como parte de pago, cancelar o reagendar de acuerdo a políticas del tour, ver reservas, dejar reseñas, transferir créditos.
2. **Panel del proveedor** (Angular 19): crear tours, definir horarios y precios (adulto, niño, bebe y cualquiera) por tour, gestionar reservas, ver pagos, responder reseñas, gestionar sub-usuarios (operadorios).
3. **App móvil Android** (.NET MAUI 10): explorar tours, carrito, checkout (pagar por wompi), reservas, perfil, panel proveedor (incluyendo escáner QR para fulfillment).
4. **Backoffice** (Angular 19): aprobar proveedores (KYB),aprobar tour, ajustar % comisión Tourya, ver reportes, gestionar reportes marítimos DIMAR, enviar evidencia de pago a proveedores.

---

## Mercado objetivo

- **Geografía**: Colombia inicialmente. especificamente la isla de san andres. luego vamos a explorar otras sitios turisticos de colombia y de latinoamerica.
- **Idiomas soportados**: español (default), inglés, portugués de Brasil. En San andres Islas los Turistas son normalmente de Colombia, Argentina, Brasil, peru, chile y ecuador. los turistas de la isla tambien vienen de Estados unidos, canada y europa pero en menor proporcion.
- **Tipo de operadores**: nicho marítimo / acuático visible (módulo "Maritime Activity Reports" para DIMAR), pero también categorías generales (Aventura, Cultural, Terrestre, alquiler de transporte, Nocturno, deportes, etc.).
- **Foco inicial**: La plataforma de Tourya esta iniciando operaciones en la isla de san andres. Tourya esta enfocado en destinos turisticos donde no esten operando localmente otras plataformas. de esta manera, Tourya puede ser la solucion donde los turistas busquen adquirir tours y otros productos y servicios.
- **Planes de expansion**: luego de que el modelo de Tourya este probado y listo para escalar se evaluarán otros destinos turisticos donde Tourya agregue valor a los turistas.
- **Turistas ideal**: para Tourya el cliente ideal es todo aquel que le gusta adquirir productos y servicios utilizando su teléfono móvil y que no tiene problemas con pagar una comisión por el servicio y la comodidad que les brinda nuestra plataforma.
- **Convenios B2B**: Actualmente Tourya no tiene firmados convenios B2B, pero si esta planificado a corto plazo habilitar esta funcionalidad de manera que ayude a aumentar las ventas. la idea es firmar convenios con hoteles y negocios con alto trafico, que puedan generar ingresos de cada transacción generada por la recomendación de tourya realizada por dicho negocio. podria ser que el establecimiento genere un QR que lleve al turista a tourya y si realiza una compra el establecimiento se gana una comisión.

---

## Visión a futuro

- **Horizonte 6 meses**: en los primeros 6 meses la idea validar nuestro modelo y hacer los ajustes de manera de que sea rentable y que pueda operar solo con una persona (eficiente), apoyado por la Inteligencia Artificial.
- **Horizonte a 1 año**: En este plazo, Tourya ya debe haber habilitado otros servicios como Servicio de Transporte, Domicilios (Licor, Comida, Farmacia(productos basicos sin recipe medico)), Hospedaje y Souvenirs.
- **Horizonte a 2 años**: en esta espacio ya debemos haber probado el modelo en otros destinos turisticos de colombia y latinoamerica.
- **Métricas críticas**: Costo de adquisicion de usuario (CAC), Tasa de conversión, Tasa de abandono de Carritos, Tasa de compras recurrentes. tambien algunas metricas que nos permitan conocer la eficiencia de los proveedores registrados en la plataforma.
- **Módulos planeados**: Marketplace B2B (para que las empresas ofrescan esos beneficios a sus colaboradores), modulo de reservas para hoteles, Fidelizacion Avanzada (para los turistas), Modulo de Black friday o hotsale donde el turista para el dia actual coloce lo que busca y los operadores puedan ofertar (tipo in driver) y el cliente la da clic a la oferta, modulo de publicidad (.

---

## Problema que Tourya resuelve hoy (deducido)

✅ Sin Tourya, un operador turístico tradicional en Colombia tiene que:
- Recibir reservas por WhatsApp / llamadas / correo.
- Recibe ventas por diferentes agencias turisticas o vendedores ambulantes.
- Gestionar disponibilidad en una agenda manual o Excel.
- Cobrar por transferencia bancaria / efectivo.
- Cumplir DIMAR sin sistema integrado (cuando aplica).
- Sin visibilidad real de su cartera de turistas ni de su flujo de caja.

✅ Y sin Tourya, un turista que busca experiencias en Colombia:
- Tiene que buscar por separado en Instagram / Booking / TripAdvisor / Viator / agencias locales.
- No tiene forma de comprar un Tour sin tantos intermediarios.
- No tiene forma estandarizada de comparar precios / reseñas / horários.
- No tiene un canal de pago seguro (Wompi o Mercadopago).
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
| Payouts semanales (Martes y Viernes) | Reschedule de reservas |
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
- El operador NUNCA ve el `slotPercentageTourya` ni el `price` final, solo ve su `providerPrice`.

### Cómo paga Tourya al operador

✅ `ProviderPayoutOrderJob` corre dos veces por semana:
- **Lunes 7:00 AM (Bogotá)**: crea las ordenes de pago para las reservas ejecutadas Jueves a Domingo de la semana anterior.
- **Jueves 7:00 AM (Bogotá)**: crea las ordenes de pago para las reservas ejecutadas Lunes a Miércoles.

Una reserva entra al payout cuando:
1. Ya pasó la fecha del tour (`reservation_date`).
2. Han pasado **2 días** desde la ejecución (`payout_available_date = reservation_date + 2 días`) — buffer para reclamos/disputas.
3. La reserva fue marcada como `DELIVERED` (escaneada por QR o confirmada por el operador).

El backoffice luego **sube el comprobante de pago** (`POST /provider/payout-orders/admin/{orderId}/proof`) y marca como `PAID`.

- **Cronograma de Payouts**: Los payouts se realizan los martes y los viernes. por tanto el Job (ProviderPayoutOrderJob) genera las ordenes de pago los lunes y jueves, para que el backoffice pueda el martes y viernes subir los comprobantes de pago.
- **Buffer**: El Buffer de 2 dias actualmente no es una regla de negocio configurable. seria lo ideal que fuese configurable de manera que podamos adaptarla de acuerdo a las exigencias del mercado.
- **Transferencias a proveedores**: Tourya tiene planeado hacer de forma manual las transferencias a los proveedores de acuerdo a las opciones (ej Nequi, Banco, Bre-B) que brinde Whompi o Mercadopago para el pago a sus proveedores. el comprobante de transferencia es lo que el backoffice sube a la orden de pago. Luego evaluaremos automatizar dichos pagos de acuerdo a las API que brinden las pasarelas de pago antes mencionadas.
     

### Otras formas de monetización (potenciales)

- **Tours destacados / promoción pagada**: esta funcionalidad esta planeada para una version 2 del marketplace.
- **Herramienta de Gestion de Reservas**: para los operadores turisticos que no tengan una herramienta de gestion de reservas (tours y hospedajes), el cual tenga un calendario de todas las reservas por tour o hospedaje, integracion a plataformas de reserva (viator, airbnb, booking, GetYourGuide, etc), modulos de reservas manuales, envio de reservas por correco electronico, gestion de asignacion de recursos, reportes y estadisticas financieros (Reporte de ingresos por canal,Ocupación promedio, cierre de caja).
- **Publicidad para negocios locales**: esta funcionalidad permitira que negocios locales puedan pautar publicidad con la plataforma. estos videos se mostraran en 3 lugares: al iniciar (maximo 5 segundos y con boton para skip ad), luego del checkout (en el correo que llega con el QR de la reserva), y pestaña guia de la isla donde se podran colocar videos tipo tiktok con boton directa a whatsapp.
- **Duty free**: esta funcionalidad permitirá que los negocios publiquen promociones y que el cliente al hacer la compra le llegue un QR. el cliente debe recoger en sitio su compra (Click & Collect) y presentar el QR el cual debe ser escaneado por el negocio local. en este caso tourya se quedara con una comision por la venta.



---

## Clientes / Operadores hoy

Necesario saber:
- Número de operadores onboarded: actualmente se tiene hablado 3 operadores turisticos entre ellos el mas grande.
- Número de tours publicados: falta publicar Tourya en productivo para que esos 3 operadores incluyan sus tours (que serian en total unos 20 tours).
- Número de turistas registrados: actualmente 0.
- Reservas mensuales actuales: actualmente 0.
- GMV mensual: actualmente 0.
- Comisión bruta mensual de Tourya: actualemente 0.

---

## Diferencia vs alternativas (deducido)

| Hoy turista usa... | Limitación | Tourya |
|--------------------|------------|--------|
| WhatsApp / llamada al operador | Sin reservas confirmadas, sin pago seguro | Reserva con pago integrado y QR |
| Booking / TripAdvisor / viator | Foco en hotelería; tours como "complemento", poca cobertura local | Foco 100% en experiencias colombianas |
| Instagram / búsqueda en redes | Sin estructura, sin comparación, sin reseñas reales | Estructura, reseñas verificadas, comparación |
| Agencia tradicional | Comisión alta, intermediación lenta | Marketplace directo operador↔turista |
| Excel / agenda en papel (operador) | Sin disponibilidad real, conflictos de booking | Capacity tracking automático, multi-operador |

---

## Métricas críticas (a definir)



| Métrica | Hoy | Meta 12 meses |
|---------|-----|----------------|
| # Operadores activos | 3 (cuando salgamos en productivo) | 70 |
| # Tours publicados | 20 (cuando salgamos en productivo) | 150 |
| # Turistas registrados | 0 | 500 |
| # Reservas/mes | 0 | 300 |
| GMV mensual | 0 | 400.000.000 COP |
| Comisión bruta Tourya/mes | 0 | 47.000.000 COP |
| Tasa de cancelación | 0 | 18% |
| NPS turistas | 0 | 55 |
| NPS operadores | 0 | 30 |
| % reservas que dejan reseña | 0 | 15% |
| % tours con galería completa | 0 | 85% |

---

## Roadmap de producto (a definir)

Sugerencias basadas en lo que está en código vs. lo que falta:

### Funcionalidades parcialmente implementadas (vale la pena terminar)
- **Traducción automática de tours** (es → en, pt) — propuesta en `traduccion-automatica-tours.md`.
- **Login social sin Firebase** (eliminar lock-in) — propuesta en `social-login-google-facebook.md`.
- **Mobile iOS** (hoy solo Android) — el código MAUI lo soportaría con poco trabajo. tanto la version android como la iOS deben brindar la misma funcionalidad que brinda la WEB.
- **Backoffice** (hoy solo Android) — se debe mejorar todo el backoffice. actualmente permite: gestionar aprobacion de proveedores (operadores turisticos), gestionar la aprobacion de tours, ver las reservas, ver las reseñas, ordenes de pago (subir comprobante de pago). actualmente faltan trabajar el dashboard (Financiero (GMV, Net revenue estimado, cuentas por pagar, costo de adquisicion de cliente) y operativo (Tasa de Conversión,Tasa de Cancelación, Tours de Alto Riesgo, Tiempo de Resolución de Soporte)), gestor de disputas,.


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
