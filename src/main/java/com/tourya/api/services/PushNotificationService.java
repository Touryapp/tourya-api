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
