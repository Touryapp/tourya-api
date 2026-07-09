package com.tourya.api.config.auth;

import com.tourya.api.config.security.JwtService;
import com.tourya.api.models.RefreshToken;
import com.tourya.api.models.User;
import com.tourya.api.repository.RefreshTokenRepository;
import com.tourya.api.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestiona el ciclo de vida de los refresh tokens.
 * <ul>
 *   <li><b>issueForUser</b>: emite el primer refresh token de una sesion nueva (family_id nuevo).</li>
 *   <li><b>rotate</b>: valida un refresh token existente, detecta reuso, revoca el actual
 *       y emite un nuevo par access + refresh (misma familia).</li>
 *   <li><b>logout</b>: revoca la familia entera de un refresh token dado.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REASON_ROTATED = "rotated";
    private static final String REASON_LOGOUT = "logout";
    private static final String REASON_REUSE = "reuse_detected";

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * Emite el primer refresh token de una nueva sesion.
     * Se llama al login exitoso (password o social).
     */
    @Transactional
    public IssuedTokens issueForUser(User user) {
        String familyId = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();
        return persistAndBuild(user, jti, familyId, null);
    }

    /**
     * Rota un refresh token existente.
     * Al validar detecta 3 casos de fraude:
     * <ol>
     *   <li>Firma invalida o expirado (JWT).</li>
     *   <li>Token no existe en BD (nunca emitido).</li>
     *   <li>Token ya revocado (reuso -> revoca toda la familia).</li>
     * </ol>
     * Si todo OK: revoca el actual con reason=rotated, emite uno nuevo con el mismo family_id
     * y previous_jti = jti anterior. Retorna el nuevo par access + refresh.
     */
    @Transactional
    public IssuedTokens rotate(String rawRefreshToken) {
        Claims claims;
        try {
            claims = jwtService.extractAllClaimsPublic(rawRefreshToken);
        } catch (Exception e) {
            log.warn("Refresh token rechazado: JWT invalido o expirado. reason={}", e.getClass().getSimpleName());
            throw new RefreshTokenException("Invalid refresh token");
        }

        if (jwtService.isExpired(rawRefreshToken)) {
            throw new RefreshTokenException("Refresh token expired");
        }
        if (!"refresh".equals(claims.get("type"))) {
            throw new RefreshTokenException("Not a refresh token");
        }

        String jti = claims.get("jti", String.class);
        if (jti == null) {
            throw new RefreshTokenException("Refresh token missing jti");
        }

        Optional<RefreshToken> found = refreshTokenRepository.findByJti(jti);
        if (found.isEmpty()) {
            log.warn("Refresh token rechazado: jti={} no existe en BD (posible falsificacion)", jti);
            throw new RefreshTokenException("Unknown refresh token");
        }
        RefreshToken current = found.get();

        if (current.getRevokedAt() != null) {
            log.warn("Reuso de refresh token detectado. jti={} family_id={} revoked_at={}. Revocando familia completa.",
                    current.getJti(), current.getFamilyId(), current.getRevokedAt());
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now, REASON_REUSE);
            throw new RefreshTokenException("Refresh token reuse detected");
        }

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found for refresh token"));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        current.setRevokedAt(now);
        current.setRevokedReason(REASON_ROTATED);
        refreshTokenRepository.save(current);

        String newJti = UUID.randomUUID().toString();
        return persistAndBuild(user, newJti, current.getFamilyId(), current.getJti());
    }

    /**
     * Cierra sesion: revoca la familia entera del refresh token.
     * Si el token no existe o ya esta revocado, no hace nada (idempotente).
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        Claims claims;
        try {
            claims = jwtService.extractAllClaimsPublic(rawRefreshToken);
        } catch (Exception e) {
            log.debug("Logout: refresh token invalido o expirado. Se ignora silenciosamente.");
            return;
        }
        String jti = claims.get("jti", String.class);
        if (jti == null) return;

        Optional<RefreshToken> found = refreshTokenRepository.findByJti(jti);
        if (found.isEmpty()) return;

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int revoked = refreshTokenRepository.revokeFamily(found.get().getFamilyId(), now, REASON_LOGOUT);
        log.info("Logout: revocada familia {} (tokens revocados: {})", found.get().getFamilyId(), revoked);
    }

    private IssuedTokens persistAndBuild(User user, String jti, String familyId, String previousJti) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = Instant.ofEpochMilli(System.currentTimeMillis() + jwtService.getRefreshExpirationMs())
                .atOffset(ZoneOffset.UTC);

        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .jti(jti)
                .familyId(familyId)
                .previousJti(previousJti)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(entity);

        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put("fullName", user.fullName());
        String accessToken = jwtService.generateToken(accessClaims, user);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), jti, familyId, previousJti);

        return new IssuedTokens(user, accessToken, refreshToken);
    }

    public record IssuedTokens(User user, String accessToken, String refreshToken) {}

    /**
     * Excepcion domain-level para errores de refresh token. Se mapea a 401 en el handler
     * de auth (o Spring devuelve 500 por default hasta que se agregue en GlobalExceptionHandler).
     */
    public static class RefreshTokenException extends RuntimeException {
        public RefreshTokenException(String message) {
            super(message);
        }
    }
}
