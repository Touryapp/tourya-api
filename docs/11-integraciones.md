# 11 — Integraciones externas

Servicios de terceros con los que Tourya se integra. Para cada uno: propósito, cómo se integra, credenciales y consideraciones.

---

## 1. Wompi (pasarela de pagos)

### Propósito
Procesar pagos con tarjeta de crédito/débito, PSE, Bancolombia button y otros métodos de Colombia.

### Cómo se integra

✅ Tourya usa el flujo de **Wompi Widget** (no API directa):

1. Frontend / Mobile genera una **referencia firmada** llamando a:
   ```
   GET /api/v1/reference/generate?amount=115000&currency=COP
   ```
   El backend (`ReferenceController`) devuelve `{ reference, amountInCents, currency, sha256_hash }` usando el `integrity_secret` de Wompi.

2. Frontend abre el widget de Wompi (WebView en mobile, JS embebido en web).

3. Usuario completa el pago.

4. Wompi retorna el `transactionId` al frontend.

5. Frontend llama `POST /api/v1/payment` con `transactionId`, `reservationIds`, datos del pagador.

6. Backend persiste `Payment` y confirma las reservas (TEMPORAL → CONFIRMED).

### Credenciales

- **Public Key** (frontend/mobile, segura de exponer):
  - `pub_test_bIOZLLlzg8Oel52ljFIp7Sd4FDEOo1da` (test)

- **Integrity Secret** (backend, NO exponer):
  - ⚠️ Hardcoded en `application.properties`. **Vulnerabilidad** documentada en `security-remediation-plan.md`.

- **Private Key** (server-side, para webhooks): ❓ — Tourya no usa webhook hoy.

### Limitaciones / problemas

⚠️ **NO hay webhook server-side de Wompi**. La confirmación es client-side. Si el frontend falla entre el pago Wompi y el `POST /payment`, el dinero del usuario está cobrado pero la reserva queda en limbo (TEMPORAL → expira).

📌 PENDIENTE LUIS — confirmar si se implementa webhook.

---

## 2. SMTP (Gmail Workspace Relay)

### Propósito
Envío de emails transaccionales:
- Activación de cuenta (token 6 dígitos).
- Confirmación de reserva.
- Notificación de cancelación.
- Reset de contraseña (sub-usuarios).
- Reseñas pendientes.
- Bienvenida al provider aprobado.

### Cómo se integra

✅ Spring Mail apuntando a Gmail Workspace SMTP Relay:

```properties
spring.mail.host=smtp-relay.gmail.com
spring.mail.port=587
spring.mail.username=noreply@wass.com.co
spring.mail.password=${MAIL_PASSWORD}     ← env var
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.ssl.trust=*
spring.mail.properties.mail.smtp.localhost=wass.com.co
spring.mail.properties.mail.smtp.ssl.protocols=TLSv1.2
```

### Templates

Usa Thymeleaf:
- `templates/activate_account.html`
- `templates/purchase_confirmation.html`
- `templates/request_provider_status.html`
- etc.

### Async

`EmailService` usa `@Async` para no bloquear la respuesta HTTP del controller.

### Históricos relevantes

⚠️ **Problema resuelto**: Cloud Run con CPU throttling cortaba el TLS handshake del SMTP durante el `@Async`. Solución: activar `--no-cpu-throttling` en el servicio.

⚠️ **SPF + DKIM pendientes** para el dominio `wass.com.co` — los emails llegan a spam mientras tanto. Coordinación con Luis (admin de Workspace).

### Sender

- `From: noreply@wass.com.co` (configurado por Luis en su Workspace).
- Razón: la versión anterior usaba `eowkin@gmail.com` (cuenta personal), se migró a la cuenta de empresa.

---

## 3. Google Cloud Storage (GCS) / AWS S3

### Propósito
Almacenamiento de archivos:
- Imágenes de tour (galería).
- Fotos de perfil de turista.
- Fotos en reseñas.
- Documentos KYB.
- Comprobantes de payout.
- QR codes generados.

### Cómo se integra

✅ Tourya tiene una **abstracción `IStorageService`** con dos implementaciones:

| Implementación | Usado en | Selección |
|----------------|----------|-----------|
| `S3Service` | AWS legacy | `storage.provider=AWS` |
| `GcsStorageService` | GCP producción | `storage.provider=GCP` |

La elección es a nivel property → puede cambiar sin recompilar.

### Credenciales

#### GCS
- **Service Account Key JSON**: archivo con credenciales.
- ✅ **Resuelto 2026-07-08**: el archivo `tourya-api/docs/tourya-dev-sa-key.json` NO estaba commiteado al repo (el `.gitignore` con patrón `docs/*-sa-key.json` lo protegió desde siempre). El key `df67...` sí vivía en el filesystem local del dev y en GitHub Secrets como `GCP_SA_KEY_DEV`. Ambos exposiciones fueron cerradas: key deshabilitado en IAM y migración de autenticación GitHub Actions → GCP a Workload Identity Federation (PR #153). El GitHub Secret fue eliminado.

#### AWS S3
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `AWS_BUCKET` (env vars).

### URLs

✅ Las URLs generadas se persisten en las entidades (`tour_gallery.image_url`, `review.attachment.fileUrl`, etc.). Pueden ser:
- Públicas (lecturas directas).
- Signed URLs (para uploads o privadas).

📌 PENDIENTE — confirmar policy del bucket (¿es público o requiere autenticación?).

---

## 4. Firebase Authentication

### Propósito
Login social con Google y Facebook.

### Cómo se integra (hoy)

✅ Flujo actual:
1. Frontend usa Firebase JS SDK (`firebase/auth`).
2. Usuario hace click en "Continuar con Google" o "Continuar con Facebook".
3. Firebase abre popup, usuario se autentica, devuelve `UserCredential` con `user.uid` (Firebase UID).
4. Frontend envía a `POST /auth/social-auth`:
   ```json
   {
     "firstname": "Juan",
     "lastname": "Pérez",
     "email": "juan@gmail.com",
     "uuidSocial": "abc123..."
   }
   ```
5. Backend busca/crea User por email, guarda `uuidSocial`, emite JWT.

### Proyecto Firebase
- **Project ID**: `tourya-169d6`
- **API Key**: `AIzaSyCJyNkzo4e80-G0eBCUrIBDt6bbJ8Osp_g` (frontend, no sensible)
- **Configuración**: en `environment.ts` / `environment.prod.ts`

### Limitación grave

⚠️ El backend **NO valida el token** Firebase. Solo confía en el `uuidSocial` enviado. Cualquiera puede suplantar a un usuario conociendo su email.

### Propuesta de migración

✅ Documentada en `social-login-google-facebook.md`: salir de Firebase y validar tokens directamente contra Google/Facebook (patrón Token Exchange). Beneficios:
- Elimina lock-in con Firebase.
- Cierra la vulnerabilidad.
- Reduce tamaño de bundle (Firebase SDK ~300KB).

📌 PENDIENTE LUIS — aprobar la migración (3-4 días de desarrollo).

---

## 5. Google Cloud Translation (futuro)

### Propósito (propuesto)
Traducir automáticamente los textos del tour (es → en, pt-BR) cuando el operador crea/edita.

### Estado
⚠️ **NO implementado aún**. Documentación de la propuesta en `traduccion-automatica-tours.md`.

### Estimación
- Setup: 0.5 día.
- Backend (service + hook async en `TourService`): 1.5 días.
- Pruebas: 0.5 día.

### Costo
- Cuota gratuita: 500.000 caracteres/mes.
- ~50-80 tours/mes sin costo.
- Pasada la cuota: $20/millón → ~$0.0002 por tour.

---

## 6. DIMAR (Dirección General Marítima)

### Propósito
Cumplimiento normativo para tours marítimos en Colombia (banderas verde/amarilla/roja).

### Cómo se integra
✅ Tourya **NO tiene integración directa con DIMAR**. El backoffice ingresa manualmente reportes (`POST /maritime-activity-reports`) que sirven de soporte legal para cancelaciones por mal tiempo.

📌 PENDIENTE LUIS — ¿hay roadmap para integrar API DIMAR (si existe pública)?

---

## 7. Servicios bancarios (payouts a proveedores)

### Propósito
Transferir el dinero recaudado al operador.

### Cómo se integra
⚠️ **NO hay integración bancaria.** El proceso es 100% manual:
1. Sistema genera `ProviderPayoutOrder` con monto.
2. Backoffice consulta los datos bancarios del proveedor.
3. Hace la transferencia manualmente (banco, Bre-B, Nequi, etc.).
4. Sube comprobante (`POST /provider/payout-orders/admin/{orderId}/proof`).
5. Marca como `PAID`.

📌 PENDIENTE LUIS — ¿hay planes de integrar Bre-B o transferencia automática? (Wompi ofrece transferencias salientes).

---

## 8. Twilio Programmable Messaging (WhatsApp) — aprobado, a implementar

### Propósito
Canal WhatsApp para los agentes IA (ver [16 — Agentes IA](16-agentes-ia.md)):
- **Agente 2 — Support 24/7**: envío de QR post-pago, respuestas de cancelación y reagendamiento.
- **Agente 3 — Desert Shopping Cart**: recuperación de carritos abandonados.

### Cómo se integrará
✅ **Decisión Luis + Franklin (2026-07-07)**: usar **Twilio** como **BSP** (Business Solution Provider) en vez de integrar directamente con Meta/Facebook.

**Ventajas**:
- Onboarding más simple — Twilio ya tiene WABA (WhatsApp Business Account) aprobado.
- SDK estable y documentado.
- No hay que pelear el proceso de verificación de Meta directamente.

**Trade-off**: costo mayor (fee de Twilio sobre el costo Meta) a cambio de velocidad de implementación.

### Alcance de la integración
- Envío outbound de mensajes (plantillas aprobadas + free-form dentro de la ventana 24h).
- Recepción inbound vía webhook (para que el agente responda al turista).
- Plantillas por caso de uso: confirmación de reserva, recordatorio, cancelación, reagendamiento, recuperación de carrito.

📌 Pendiente: crear cuenta Twilio, aprobar plantillas, definir sender ID.

---

## 9. Otras integraciones potenciales (no implementadas)

| Integración | Propósito | Estado |
|-------------|-----------|--------|
| **Webhook Wompi** | Confirmación de pago server-side | ✅ Aprobado — a implementar (RN-025) |
| **Push Notifications (FCM)** | Notificar reservas, cancelaciones | No implementado |
| **Google Maps Geocoding** | Resolver lat/long desde direcciones | No implementado (se ingresan manualmente) |
| **Google Maps Render** | Mostrar mapa en detalle de tour | Componente Syncfusion.Maps importado en mobile, no usado aún |
| **DIAN (facturación electrónica)** | Cumplimiento factura para compras | No implementado |
| **CRM (Hubspot, etc.)** | Marketing/leads | No implementado |
| **Analytics (GA4, Mixpanel)** | Producto / conversión | No implementado |
| **Sentry / Datadog** | Monitoring de errores | No implementado — ver [13 — Despliegue](13-despliegue-cicd.md) para alertas GCP básicas |

📌 PENDIENTE LUIS — priorizar cuáles del listado de arriba van en el roadmap.

---

## Resumen de credenciales y dónde viven

| Servicio | Credencial | Ubicación |
|----------|------------|-----------|
| Wompi public key | `pub_test_*` | mobile `Constants.cs`, frontend env |
| Wompi integrity secret | ✅ En GCP Secret Manager (`tourya-wompi-integrity-secret`) en dev. Fallback en `application.properties` como puente hasta quitar |
| Gmail SMTP password | env var `MAIL_PASSWORD` (Cloud Run) |
| GCS Service Account key | ✅ Ya no se usan keys JSON. GitHub Actions autentica vía Workload Identity Federation (WIF). Cloud Run usa identidad del SA implícita |
| AWS keys | env vars (Cloud Run / AWS legacy) |
| Firebase API key | frontend env (no sensible) |
| JWT secret | ✅ En GCP Secret Manager (`tourya-jwt-key`, valor rotado) en dev. Fallback en `application.properties` como puente hasta quitar |

✅ **Completado en dev (2026-07-08)**: `JWT_SECRET`, `WOMPI_INTEGRITY_SECRET`, `DB_PASSWORD`, `MAIL_PASSWORD` en GCP Secret Manager. Cloud Run inyecta como env vars vía `--set-secrets`. 📌 Falta replicar en proyecto de producción (`tourya-project-493820`).

---

## Recomendaciones

1. **Implementar webhook server-side de Wompi** para no perder pagos.
2. **Migrar de Firebase a Token Exchange** (propuesta en `social-login-google-facebook.md`).
3. **Implementar Google Cloud Translation** (propuesta en `traduccion-automatica-tours.md`).
4. ~~Migrar secretos a Secret Manager~~ — ✅ Completado en dev (2026-07-08).
5. **Push Notifications**: evaluar FCM (es gratis y se integra bien con MAUI).
6. **Analytics**: si Luis quiere data del producto, GA4 web + Firebase Analytics mobile.
