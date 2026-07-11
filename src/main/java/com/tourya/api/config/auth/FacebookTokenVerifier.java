package com.tourya.api.config.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.exceptions.InvalidSocialTokenException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * SEC-06: valida access tokens de Facebook contra la Graph API de Facebook.
 *
 * <p>El flow es:
 * <ol>
 *     <li>Llamar a {@code debug_token} para verificar que el token es valido y
 *     que fue emitido para nuestra app ({@code app_id} y {@code is_valid=true}).</li>
 *     <li>Llamar a {@code /me?fields=id,name,email} para obtener el perfil del
 *     usuario.</li>
 * </ol>
 *
 * <p>Si el usuario oculto su email al autorizar, se rechaza (Tourya requiere
 * email — ver RN-006).</p>
 *
 * <p>Facebook no expone claves publicas para validacion offline, por lo que
 * cada login hace 2 llamadas HTTP a Facebook. Es aceptable — la frecuencia es
 * baja y el resultado es cacheable durante la sesion via el JWT propio de
 * Tourya.</p>
 */
@Slf4j
@Service
public class FacebookTokenVerifier {

    private static final String GRAPH_BASE = "https://graph.facebook.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Value("${social.facebook.app-id:}")
    private String appId;

    @Value("${social.facebook.app-secret:}")
    private String appSecret;

    private RestClient restClient;
    private ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        this.restClient = RestClient.builder()
                .baseUrl(GRAPH_BASE)
                .build();
        this.objectMapper = new ObjectMapper();
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            log.warn("SEC-06: Facebook app-id/app-secret is not configured. POST /auth/facebook will reject requests until env vars FACEBOOK_APP_ID/FACEBOOK_APP_SECRET are set.");
        }
    }

    public VerifiedSocialUser verify(String accessToken) {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new InvalidSocialTokenException("Facebook verifier is not configured");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new InvalidSocialTokenException("Missing Facebook accessToken");
        }
        try {
            debugToken(accessToken);
            return fetchProfile(accessToken);
        } catch (InvalidSocialTokenException e) {
            throw e;
        } catch (Exception e) {
            log.warn("SEC-06: Facebook accessToken verification failed: {}", e.getMessage());
            throw new InvalidSocialTokenException("Facebook accessToken verification error");
        }
    }

    private void debugToken(String accessToken) throws Exception {
        String appToken = appId + "|" + appSecret;
        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/debug_token")
                        .queryParam("input_token", accessToken)
                        .queryParam("access_token", appToken)
                        .build())
                .retrieve()
                .body(String.class);
        if (body == null) {
            throw new InvalidSocialTokenException("Facebook debug_token empty response");
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        if (!data.path("is_valid").asBoolean(false)) {
            throw new InvalidSocialTokenException("Facebook accessToken is not valid");
        }
        String tokenAppId = data.path("app_id").asText("");
        if (!appId.equals(tokenAppId)) {
            throw new InvalidSocialTokenException("Facebook accessToken belongs to a different app");
        }
    }

    private VerifiedSocialUser fetchProfile(String accessToken) throws Exception {
        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/me")
                        .queryParam("fields", "id,name,email,first_name,last_name")
                        .queryParam("access_token", accessToken)
                        .build())
                .retrieve()
                .body(String.class);
        if (body == null) {
            throw new InvalidSocialTokenException("Facebook /me empty response");
        }
        JsonNode profile = objectMapper.readTree(body);
        String id = profile.path("id").asText("");
        String email = profile.path("email").asText("");
        String firstname = profile.path("first_name").asText("");
        String lastname = profile.path("last_name").asText("");
        if (id.isBlank()) {
            throw new InvalidSocialTokenException("Facebook profile has no id");
        }
        if (email.isBlank()) {
            throw new InvalidSocialTokenException("Facebook account has no email available (usuario ocultó email al autorizar)");
        }
        if (firstname.isBlank()) {
            String name = profile.path("name").asText(email);
            firstname = name;
        }
        return new VerifiedSocialUser(id, email, firstname, lastname);
    }
}
