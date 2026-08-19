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

✅ **Webhook server-side de Wompi implementado 2026-07-08** (PRs #156 + #157 + #158, BE-16/17):

- Endpoint público `POST /public/wompi/webhook` recibe eventos server-to-server.
- Verifica firma SHA-256 con `WOMPI_EVENTS_SECRET` (secret en Secret Manager, distinto del integrity secret).
- Persiste todos los eventos en tabla `wompi_webhook_event` (con `signature_valid` como flag, para forensia).
- Job `WompiReconciliationJob` corre cada 5 min: matchea contra `Payment` existentes por `transactionId`. Si existe → link + tracking. Si no existe → **loguea WARN `orphan payment detected`** con tx_id, reference, amount → para investigación operativa manual.
- **Limitación conocida**: el matcheo automático de pagos huérfanos a las reservas TEMPORAL correctas requiere agregar `wompi_reference` a `shopping_cart` (backlog futuro BE-20 extendido). Por ahora se detectan pero se resuelven manualmente.

**Configurado en dev**: URL registrada en dashboard Wompi Sandbox por Luis, verificado end-to-end 2026-07-08.

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

✅ Spring Mail apuntando a Google Workspace SMTP Relay del dominio `tourya.co` (migrado desde el Workspace de WASS el 2026-07-10):

```properties
spring.mail.host=smtp-relay.gmail.com
spring.mail.port=587
spring.mail.username=luis.mendoza@tourya.co     ← dev provisional, pendiente noreply@tourya.co
spring.mail.password=${MAIL_PASSWORD}           ← env var (secret tourya-smtp-password)
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

⚠️ **SPF + DKIM + DMARC pendientes** para el dominio `tourya.co` en el DNS de GoDaddy — al 2026-07-10 los emails salen desde Google Workspace de Tourya pero **caen en spam** en clientes que verifican estos records. Bloqueante para prod. Luis debe agregar los TXT records que Google Workspace Admin le muestra.

### Sender

- `From: luis.mendoza@tourya.co` (provisional en dev, mientras Luis crea `noreply@tourya.co`).
- Migración desde `noreply@wass.com.co` completada el 2026-07-10 en `tourya-project-dev`: rotación de secret `tourya-smtp-password` (v1=WASS, v2=Tourya) + `gcloud run services update ... --update-env-vars=MAIL_USERNAME=luis.mendoza@tourya.co`. Revisión desplegada: `tourya-dev-api-00107-8j6`.
- **Incidente 2026-07-11/12 (BadCredentials)** — Google rechazó SMTP con `535-5.7.8`. Rollback a WASS imposible (Luis eliminó la cuenta para reducir costos). Fix: Luis regeneró app-password → v3 del secret aplicado 2026-07-13. Revisión post-fix: `tourya-dev-api-00125-jcg`. Verificado end-to-end con `POST /auth/register` a Hotmail (correo llegó a spam, esperable hasta SPF/DKIM/DMARC alineados).
- Prod (`tourya-project-493820`) sigue en Workspace de WASS al momento de esta doc. Pendiente aplicar el mismo switch cuando dev esté estable durante N días y Luis termine SPF/DKIM/DMARC.

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

## 5. Google Cloud Translation (IA-09) — ✅ productivo desde 2026-08-15

### Propósito
Traducir automáticamente los textos JSONB del tour (`es → en, pt-BR`) cuando el operador crea o edita un tour. El operador escribe una sola vez en español; el turista en/pt-BR ve el contenido en su idioma sin trabajo adicional del provider.

### Estado
✅ **Implementado y desplegado en dev** (rama `feature/ia-09-google-cloud-translation`, 2026-08-15). Propuesta original en `traduccion-automatica-tours.md` — aprobada por Luis y ejecutada según lo planeado.

### Cómo funciona
1. `TourService.saveCreateOrUpdateFullData` termina la tx del save.
2. Publica `TourTranslationEvent(tourId)`.
3. `TourTranslationEventListener` (@TransactionalEventListener AFTER_COMMIT) delega en `TourTranslationApplier.translateTourAsync` (@Async).
4. El applier recorre todos los campos JSONB del tour (`name`, `description`, addresses, main attractions, includes/excludes, faq, itinerary, cancellation policies, gallery). Para cada campo con `es` no vacío y `en`/`pt` vacío, llama `GoogleCloudTranslationService.translateBatch(es, ["en","pt"])` y persiste el resultado.
5. Si el operador ya escribió `en`/`pt` manualmente (o aceptó una sugerencia de IA-07), el applier **no sobrescribe** — respeta el input del provider.
6. Si Google Translate falla, el campo queda vacío en ese idioma y `TranslatedField#get("en")` hace auto-fallback a `es`. El tour nunca se rompe.

### Configuración
- API habilitada: `translate.googleapis.com` en `tourya-project-dev`.
- IAM: SA `tourya-dev-cloud-run@tourya-project-dev.iam.gserviceaccount.com` con `roles/cloudtranslate.user`.
- Auth: ADC (mismo patrón que Vertex AI). En Cloud Run automático; en local `gcloud auth application-default login`.
- Flags: `agents.translation.enabled=${AGENTS_TRANSLATION_ENABLED:true}`, `agents.translation.target-langs=en,pt` (`pt` se mapea internamente a `pt-BR` para el call).
- Endpoint admin para backfill/retry: `POST /admin/tours/{tourId}/retranslate` (solo ADMIN).

### Costo
- Cuota gratuita: 500.000 caracteres/mes.
- ~50-80 tours/mes sin costo.
- Pasada la cuota: $20/millón → ~$0.0002 por tour.
- Volumen esperado inicial: **$0/mes**.

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
| **Push Notifications (FCM)** | Notificar reservas, cancelaciones, recordatorios | ✅ MO-40 completo (A→D) + MO-40b (hooks a `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`). Los 5 flows publican `PushDomainEvent`s; el listener corre post-commit para que ningún fallo del stack push tumbe la tx de negocio |
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
| Firebase Admin SDK JSON (FCM) | env var `FIREBASE_ADMIN_SDK_JSON` (Cloud Run, source: GCP Secret Manager `tourya-firebase-admin-sdk` — pendiente subir). Sin ella, `PushNotificationService` corre en modo no-op sin fallar (backend arranca igual en dev/CI) |
| GCS Service Account key | ✅ Ya no se usan keys JSON. GitHub Actions autentica vía Workload Identity Federation (WIF). Cloud Run usa identidad del SA implícita |
| AWS keys | env vars (Cloud Run / AWS legacy) |
| Firebase API key | frontend env (no sensible) |
| JWT secret | ✅ En GCP Secret Manager (`tourya-jwt-key`, valor rotado) en dev. Fallback en `application.properties` como puente hasta quitar |

✅ **Completado en dev (2026-07-08)**: `JWT_SECRET`, `WOMPI_INTEGRITY_SECRET`, `DB_PASSWORD`, `MAIL_PASSWORD` en GCP Secret Manager. Cloud Run inyecta como env vars vía `--set-secrets`. 📌 Falta replicar en proyecto de producción (`tourya-project-493820`).

---

## Recomendaciones

1. ~~Implementar webhook server-side de Wompi~~ — ✅ Completado en dev 2026-07-08. Pendiente: matcheo automático de huérfanos → reservas TEMPORAL (requiere `wompi_reference` en `shopping_cart`).
2. **Migrar de Firebase a Token Exchange** (propuesta en `social-login-google-facebook.md`).
3. ~~Implementar Google Cloud Translation~~ — ✅ Completado en dev (2026-08-15, IA-09).
4. ~~Migrar secretos a Secret Manager~~ — ✅ Completado en dev (2026-07-08).
5. **Push Notifications**: evaluar FCM (es gratis y se integra bien con MAUI).
6. **Analytics**: si Luis quiere data del producto, GA4 web + Firebase Analytics mobile.
