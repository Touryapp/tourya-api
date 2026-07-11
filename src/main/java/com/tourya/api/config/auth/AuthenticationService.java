package com.tourya.api.config.auth;

import com.tourya.api._utils.Utils;
import com.tourya.api.config.auth.request.AuthenticationRequest;
import com.tourya.api.config.auth.request.FacebookAuthRequest;
import com.tourya.api.config.auth.request.GoogleAuthRequest;
import com.tourya.api.config.auth.request.SocialAuthRequest;
import com.tourya.api.config.auth.request.RegistrationRequest;
import com.tourya.api.config.auth.response.AuthenticationResponse;
import com.tourya.api.config.security.JwtService;
import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.constans.enums.EmailTemplateNameEnum;
import com.tourya.api.exceptions.EmailAlreadyExistsException;
import com.tourya.api.exceptions.EmailInvalidFormatException;
import com.tourya.api.models.Token;
import com.tourya.api.models.User;
import com.tourya.api.models.responses.MetaResponse;
import com.tourya.api.repository.RoleRepository;
import com.tourya.api.repository.TokenRepository;
import com.tourya.api.repository.UserRepository;
import com.tourya.api.services.AppConfigService;
import com.tourya.api.services.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final TokenRepository tokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final AppConfigService appConfigService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final FacebookTokenVerifier facebookTokenVerifier;

    private static final long MAX_LOCKOUT_SECONDS = 24 * 60 * 60L;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    public void register(RegistrationRequest request) throws MessagingException {
        if (!Utils.isValidEmail(request.getEmail())) {
            throw new EmailInvalidFormatException("Invalid email format: " + request.getEmail());
        }
        if (userRepository.findByEmail(request.getEmail().toLowerCase()).isPresent()) {
            throw new EmailAlreadyExistsException("Email address already exists: " + request.getEmail());
        }
        var userRole = roleRepository.findByName("USER")
                // todo - better exception handling
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initiated"));
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();
        userRepository.save(user);
        sendValidationEmail(user);
    }

    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        String activationUrlFinal = activationUrl+newToken;
        emailService.sendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplateNameEnum.ACTIVATE_ACCOUNT,
                activationUrlFinal,
                newToken,
                "Account activation"
        );
    }
    private String generateAndSaveActivationToken(User user) {
        // Generate a token
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepository.save(token);

        return generatedToken;
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();

        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            User user = (User) auth.getPrincipal();
            // SEC-10: login exitoso -> reset del contador y del lock temporal si aplica.
            // Se persiste solo si habia algo que limpiar (evita write innecesario).
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
            RefreshTokenService.IssuedTokens tokens = refreshTokenService.issueForUser(user);
            return buildAuthResponse(user, tokens, user.isMustChangePassword());
        } catch (BadCredentialsException e) {
            // SEC-10: credenciales invalidas. Si el feature flag esta ON y el email
            // existe, incrementar el contador y aplicar lockout con backoff exponencial
            // cuando pasa el umbral. Si el email no existe, NO se registra nada (evita
            // filtrar la existencia de cuentas via timing/side effects).
            if (appConfigService.getInt(ConfigKeyEnum.AUTH_LOCKOUT_ENABLED, 0) == 1) {
                registerFailedLoginAttempt(request.getEmail());
            }
            throw e;
        }
    }

    /**
     * Incrementa el contador de intentos fallidos del usuario y aplica lockout
     * temporal con backoff exponencial cuando se supera el maximo configurado.
     * <p>Se llama solo con feature flag ON (SEC-10). Silencioso si el email no existe:
     * asi evitamos filtrar cuentas al atacante via side effects observables.
     */
    private void registerFailedLoginAttempt(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return;
        }
        Optional<User> found = userRepository.findByEmail(rawEmail.toLowerCase().trim());
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();
        int maxAttempts = appConfigService.getInt(ConfigKeyEnum.AUTH_LOCKOUT_MAX_ATTEMPTS, 5);
        long baseBackoffSeconds = appConfigService.getInt(ConfigKeyEnum.AUTH_LOCKOUT_BASE_BACKOFF_SECONDS, 60);

        int newAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttempts);

        if (newAttempts >= maxAttempts) {
            long overflow = newAttempts - maxAttempts;
            long lockoutSeconds = baseBackoffSeconds * (1L << Math.min(overflow, 20)); // 2^overflow con cap logico en 20
            lockoutSeconds = Math.min(lockoutSeconds, MAX_LOCKOUT_SECONDS);
            OffsetDateTime lockedUntil = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(lockoutSeconds);
            user.setLockedUntil(lockedUntil);
            log.warn("SEC-10 lockout: user_id={} bloqueada hasta {} tras {} intentos fallidos (lockout={}s)",
                    user.getId(), lockedUntil, newAttempts, lockoutSeconds);
        } else {
            log.info("SEC-10: user_id={} intento fallido #{} / {}", user.getId(), newAttempts, maxAttempts);
        }
        userRepository.save(user);
    }

    /**
     * Rota el par (access + refresh) usando un refresh token existente.
     * Requiere que el refresh no este revocado ni expirado. Si detecta reuso,
     * revoca la familia completa antes de lanzar la excepcion.
     */
    public AuthenticationResponse refreshTokens(String rawRefreshToken) {
        RefreshTokenService.IssuedTokens tokens = refreshTokenService.rotate(rawRefreshToken);
        return buildAuthResponse(tokens.user(), tokens, tokens.user().isMustChangePassword());
    }

    /**
     * Cierra sesion: revoca la familia entera del refresh token. Idempotente.
     */
    public void logout(String rawRefreshToken) {
        refreshTokenService.logout(rawRefreshToken);
    }

    /**
     * Construye la respuesta de auth manteniendo compatibilidad hacia atras:
     * `token` sigue siendo el access token (clientes viejos lo leen), y se agregan
     * `accessToken` y `refreshToken` para los clientes nuevos.
     */
    private AuthenticationResponse buildAuthResponse(User user, RefreshTokenService.IssuedTokens tokens, Boolean mustChangePassword) {
        return AuthenticationResponse.builder()
                .meta(new MetaResponse())
                .fullName(user.fullName())
                .email(user.getEmail())
                .roleList(user.getRoles())
                .token(tokens.accessToken())
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .mustChangePassword(mustChangePassword)
                .build();
    }

    @Transactional
    public void activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token)
                // todo exception has to be defined
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        // Prevenir replay: un token ya validado no puede reutilizarse.
        // Mismo mensaje que "token no encontrado" para no filtrar informacion al atacante.
        if (savedToken.getValidatedAt() != null) {
            throw new RuntimeException("Invalid token");
        }

        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. A new token has been send to the same email address");
        }

        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);

        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }

    /**
     * SEC-06 legacy: recibe datos del proveedor SIN validarlos.
     * Vulnerabilidad conocida (RN-006) — mantenido temporalmente durante la transición.
     * Marcar @Deprecated y eliminar en la Fase C de SEC-06 (después de validar
     * los endpoints /auth/google y /auth/facebook por N dias en dev).
     */
    @Deprecated
    @Transactional
    public AuthenticationResponse authenticateWithSocial(SocialAuthRequest request){
        if (!Utils.isValidEmail(request.getEmail())) {
            throw new EmailInvalidFormatException("Invalid email format: " + request.getEmail());
        }
        VerifiedSocialUser verified = new VerifiedSocialUser(
                request.getUuidSocial(),
                request.getEmail(),
                request.getFirstname(),
                request.getLastname());
        return authenticateOrCreateSocialUser(verified);
    }

    /**
     * SEC-06: login con Google verificado server-side.
     * <p>Valida el id_token contra las claves publicas de Google (audience =
     * GOOGLE_CLIENT_ID) y verifica {@code email_verified = true} antes de
     * crear/buscar el usuario.</p>
     */
    @Transactional
    public AuthenticationResponse authenticateWithGoogle(GoogleAuthRequest request) {
        VerifiedSocialUser verified = googleTokenVerifier.verify(request.getIdToken());
        return authenticateOrCreateSocialUser(verified);
    }

    /**
     * SEC-06: login con Facebook verificado server-side.
     * <p>Valida el accessToken via Graph API (debug_token) y obtiene el perfil
     * (/me).</p>
     */
    @Transactional
    public AuthenticationResponse authenticateWithFacebook(FacebookAuthRequest request) {
        VerifiedSocialUser verified = facebookTokenVerifier.verify(request.getAccessToken());
        return authenticateOrCreateSocialUser(verified);
    }

    /**
     * Ruta comun para login social ya verificado: crea el usuario si no existe,
     * lo habilita si esta deshabilitado, emite JWT propio de Tourya. La
     * vinculacion se hace por email (RN-006, seccion 9 del doc de propuesta —
     * opcion A: vincular por email verificado).
     */
    private AuthenticationResponse authenticateOrCreateSocialUser(VerifiedSocialUser verified) {
        if (!Utils.isValidEmail(verified.email())) {
            throw new EmailInvalidFormatException("Invalid email format: " + verified.email());
        }
        String normalizedEmail = verified.email().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            var userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE USER was not initiated"));
            String tempPassword = generateTemporaryPassword();
            User newUser = User.builder()
                    .firstname(verified.firstname())
                    .lastname(verified.lastname())
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(tempPassword))
                    .accountLocked(false)
                    .enabled(true) // el proveedor ya verificó el correo (email_verified para Google, presencia para Facebook)
                    .roles(List.of(userRole))
                    .uuidSocial(verified.providerSubject())
                    .build();
            userRepository.save(newUser);
            RefreshTokenService.IssuedTokens tokens = refreshTokenService.issueForUser(newUser);
            return buildAuthResponse(newUser, tokens, newUser.isMustChangePassword());
        }
        boolean modified = false;
        if (!user.isEnabled()) {
            user.setEnabled(true);
            modified = true;
        }
        // Refresh del providerSubject: si el usuario venia de Firebase con un UID
        // viejo, se sobreescribe al primer login por Token Exchange.
        if (verified.providerSubject() != null && !verified.providerSubject().isBlank()
                && !verified.providerSubject().equals(user.getUuidSocial())) {
            user.setUuidSocial(verified.providerSubject());
            modified = true;
        }
        if (modified) {
            userRepository.save(user);
        }
        RefreshTokenService.IssuedTokens tokens = refreshTokenService.issueForUser(user);
        return buildAuthResponse(user, tokens, user.isMustChangePassword());
    }
    private String generateTemporaryPassword() {
        // Generar una contraseña temporal segura
        return UUID.randomUUID().toString().substring(0, 16); // Ejemplo
    }
    @Value("${spring.mail.username}")
    private String mailUsername;

    public void sendEmailTest(){
        emailService.sendSimpleMessage(mailUsername,  "test-email", "Prueba de email");
    }
}
