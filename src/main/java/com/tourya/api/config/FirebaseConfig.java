package com.tourya.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * MO-40 Fase A: bootstrap del Firebase Admin SDK.
 *
 * <p>Estrategia de config, en orden de prioridad:</p>
 * <ol>
 *   <li>Env var <b>FIREBASE_ADMIN_SDK_JSON</b> con el contenido COMPLETO del
 *       service account JSON (patron para Cloud Run + Secret Manager). Recomendado
 *       para prod/dev.</li>
 *   <li>Env var <b>FIREBASE_ADMIN_SDK_JSON_PATH</b> apuntando a un archivo local
 *       con el JSON (dev local sin secret manager).</li>
 *   <li>Si ninguna esta seteada, se loguea warning y el bean {@link FirebaseMessaging}
 *       NO se registra — {@link com.tourya.api.services.PushNotificationService}
 *       detecta la ausencia y hace no-op en vez de fallar. El backend arranca
 *       igual (evita bloquear dev/CI que no necesitan push).</li>
 * </ol>
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.admin.sdk.json:}")
    private String adminSdkJson;

    @Value("${firebase.admin.sdk.json.path:}")
    private String adminSdkJsonPath;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            InputStream credentialsStream = resolveCredentialsStream();
            if (credentialsStream == null) {
                log.warn("MO-40: Firebase Admin SDK credentials NOT configured. " +
                        "PushNotificationService will run in no-op mode. " +
                        "Set FIREBASE_ADMIN_SDK_JSON or FIREBASE_ADMIN_SDK_JSON_PATH to enable push.");
                return null; // Marca "push desactivado"; PushNotificationService lo tolera.
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();

            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();

            log.info("MO-40: Firebase Admin SDK initialized (projectId={})",
                    app.getOptions().getProjectId());
            return FirebaseMessaging.getInstance(app);
        } catch (Exception ex) {
            log.error("MO-40: Failed to initialize Firebase Admin SDK — push disabled", ex);
            return null;
        }
    }

    private InputStream resolveCredentialsStream() throws Exception {
        if (adminSdkJson != null && !adminSdkJson.isBlank()) {
            return new ByteArrayInputStream(adminSdkJson.getBytes(StandardCharsets.UTF_8));
        }
        if (adminSdkJsonPath != null && !adminSdkJsonPath.isBlank()) {
            return new FileInputStream(adminSdkJsonPath);
        }
        return null;
    }
}
