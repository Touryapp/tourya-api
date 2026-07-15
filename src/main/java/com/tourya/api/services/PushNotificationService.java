package com.tourya.api.services;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.tourya.api.models.DeviceToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MO-40 Fase A: envio de push notifications via FCM.
 *
 * <p>El {@link FirebaseMessaging} se inyecta como {@code @Autowired(required=false)}
 * — cuando no hay credentials configuradas, el bean vale {@code null} y este
 * servicio degrada a no-op (loguea WARN). Esto permite que el backend arranque
 * en dev/CI sin FCM configurado.</p>
 *
 * <p>Todos los envios son {@code @Async} — no bloquean el hilo del caller. Un
 * fallo de FCM solo se loguea; el flow de negocio no se afecta.</p>
 *
 * <p>Fase D integrara este servicio en flows existentes (reserva creada,
 * cancelada, review recibida, etc.). Por ahora solo expone las primitivas.</p>
 */
@Slf4j
@Service
public class PushNotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final DeviceTokenService deviceTokenService;

    public PushNotificationService(
            @Autowired(required = false) FirebaseMessaging firebaseMessaging,
            DeviceTokenService deviceTokenService) {
        this.firebaseMessaging = firebaseMessaging;
        this.deviceTokenService = deviceTokenService;
    }

    /**
     * Envia push a todos los dispositivos registrados de un usuario.
     *
     * @param userId destino
     * @param title  titulo del notification (visible)
     * @param body   cuerpo del notification (visible)
     * @param data   payload opcional key/value (invisible; para deep-links)
     */
    @Async
    public void sendToUser(Integer userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.warn("MO-40 push no-op: FirebaseMessaging not configured (userId={} title={})", userId, title);
            return;
        }

        List<DeviceToken> tokens = deviceTokenService.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("MO-40 push skip: userId={} has no device tokens", userId);
            return;
        }

        List<String> tokenValues = tokens.stream().map(DeviceToken::getToken).toList();

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        MulticastMessage.Builder builder = MulticastMessage.builder()
                .setNotification(notification)
                .addAllTokens(tokenValues);

        if (data != null && !data.isEmpty()) {
            builder.putAllData(data);
        }

        try {
            var response = firebaseMessaging.sendEachForMulticast(builder.build());
            log.info("MO-40 push sent userId={} success={} failure={}",
                    userId, response.getSuccessCount(), response.getFailureCount());
            // Nota: la limpieza de tokens invalidos (response.getResponses() con error
            // UNREGISTERED) queda como TODO para un job de mantenimiento futuro.
        } catch (Exception ex) {
            log.error("MO-40 push failed userId={} title={}", userId, title, ex);
        }
    }

    // ===================================================================
    // MO-40 Fase D: helpers de dominio. Envuelven sendToUser con templates
    // de titulo/body + payload data que el handler mobile sabe interpretar
    // (ver PushNotificationHandler.BuildRoute en tourya-mobile).
    // ===================================================================

    /**
     * Turista: su pago fue aprobado y la reserva quedo confirmada.
     * Deep-link → reservation-detail.
     */
    public void notifyReservationConfirmedForTourist(Integer userId, String tourName, long reservationId) {
        if (userId == null || userId <= 0) return;
        sendToUser(userId,
                "¡Reserva confirmada!",
                "Tu compra de " + safe(tourName) + " ya está lista. Toca para ver el QR.",
                Map.of("type", "reservation", "targetId", String.valueOf(reservationId)));
    }

    /**
     * Provider: llego una nueva reserva a uno de sus tours.
     * Deep-link → reservation-detail (misma pantalla, provider la ve como PROVIDER).
     */
    public void notifyNewReservationForProvider(Integer providerUserId, String tourName, long reservationId) {
        if (providerUserId == null || providerUserId <= 0) return;
        sendToUser(providerUserId,
                "Nueva reserva",
                "Un turista reservó " + safe(tourName) + ".",
                Map.of("type", "reservation", "targetId", String.valueOf(reservationId)));
    }

    /**
     * Turista: recordatorio 24h antes del tour.
     * Deep-link → reservation-detail.
     */
    public void notifyTourReminderForTourist(Integer userId, String tourName, long reservationId) {
        if (userId == null || userId <= 0) return;
        sendToUser(userId,
                "Tu tour es mañana",
                "Recuerda tu reserva de " + safe(tourName) + ". Ten a mano el QR.",
                Map.of("type", "reservation", "targetId", String.valueOf(reservationId)));
    }

    /**
     * Turista: uno de sus creditos vence en N dias.
     * Deep-link → credits.
     */
    public void notifyCreditExpiringSoonForTourist(Integer userId, int daysLeft) {
        if (userId == null || userId <= 0) return;
        sendToUser(userId,
                "Tu crédito vence pronto",
                "Te quedan " + daysLeft + " días para usar tu crédito. No lo dejes vencer.",
                Map.of("type", "credit"));
    }

    /**
     * Turista: uno de sus creditos vencio hoy.
     * Deep-link → credits (para ver el historial).
     */
    public void notifyCreditExpiredForTourist(Integer userId) {
        if (userId == null || userId <= 0) return;
        sendToUser(userId,
                "Tu crédito venció",
                "Uno de tus créditos venció hoy. Podés ver el historial en Mis Créditos.",
                Map.of("type", "credit"));
    }

    /**
     * Turista: el provider respondio su review.
     * Deep-link → reservation-detail (la review vive en el detalle de la reserva).
     */
    public void notifyReviewRepliedForTourist(Integer userId, String tourName, long reservationId) {
        if (userId == null || userId <= 0) return;
        sendToUser(userId,
                "Respondieron tu reseña",
                "El proveedor de " + safe(tourName) + " respondió tu reseña.",
                Map.of("type", "review", "targetId", String.valueOf(reservationId)));
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "tu tour" : s;
    }

    /**
     * Envia push a un token puntual (util para pruebas). Los flows de negocio
     * deberian usar {@link #sendToUser(Integer, String, String, Map)}.
     */
    @Async
    public void sendToToken(String token, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.warn("MO-40 push no-op: FirebaseMessaging not configured");
            return;
        }

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(notification);

        if (data != null && !data.isEmpty()) {
            builder.putAllData(data);
        }

        try {
            String messageId = firebaseMessaging.send(builder.build());
            log.info("MO-40 push single sent messageId={}", messageId);
        } catch (Exception ex) {
            log.error("MO-40 push single failed", ex);
        }
    }
}
