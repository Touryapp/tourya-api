package com.tourya.api.config.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tourya.api.exceptions.InvalidSocialTokenException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * SEC-06: valida id_tokens de Google contra las claves publicas de Google
 * (JWKS descargadas desde https://www.googleapis.com/oauth2/v3/certs). La
 * verificacion es local (sin llamada a Google en cada login), solo verifica
 * la firma criptografica del JWT y que fue emitido para nuestro
 * {@code GOOGLE_CLIENT_ID}.
 *
 * <p>Se valida ademas que el claim {@code email_verified = true} — no
 * aceptamos usuarios cuya cuenta Google no confirmo el correo.</p>
 */
@Slf4j
@Service
public class GoogleTokenVerifier {

    @Value("${social.google.client-id:}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    void init() {
        if (clientId == null || clientId.isBlank()) {
            log.warn("SEC-06: social.google.client-id is not configured. POST /auth/google will reject requests until env var GOOGLE_CLIENT_ID is set.");
            return;
        }
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Verifica el id_token y devuelve los datos ya validados.
     *
     * @throws InvalidSocialTokenException si el token es invalido, expirado,
     *                                     firmado por otro emisor, con audience
     *                                     distinto o con email no verificado.
     */
    public VerifiedSocialUser verify(String idTokenString) {
        if (verifier == null) {
            throw new InvalidSocialTokenException("Google verifier is not configured (missing social.google.client-id)");
        }
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new InvalidSocialTokenException("Missing Google idToken");
        }
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidSocialTokenException("Google idToken failed verification");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new InvalidSocialTokenException("Google account email is not verified");
            }
            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new InvalidSocialTokenException("Google idToken has no email claim");
            }
            String subject = payload.getSubject();
            String givenName = (String) payload.get("given_name");
            String familyName = (String) payload.get("family_name");
            if (givenName == null || givenName.isBlank()) {
                String name = (String) payload.get("name");
                givenName = name != null ? name : email;
            }
            return new VerifiedSocialUser(subject, email, givenName, familyName != null ? familyName : "");
        } catch (GeneralSecurityException | java.io.IOException e) {
            log.warn("SEC-06: Google idToken verification failed: {}", e.getMessage());
            throw new InvalidSocialTokenException("Google idToken verification error");
        }
    }
}
