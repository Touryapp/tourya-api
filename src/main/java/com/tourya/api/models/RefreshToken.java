package com.tourya.api.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Refresh token emitido por el sistema de auth.
 * Usado para rotar access tokens sin re-login. Cada rotacion revoca el token
 * anterior; el reuso de un token revocado revoca toda su familia (sesion).
 * No extiende BaseEntity: issued_at + revoked_at + user_id cubren la trazabilidad
 * y no hay lastModifiedBy relevante (el usuario es el mismo que crea).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "jti", nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "family_id", nullable = false, length = 64)
    private String familyId;

    @Column(name = "previous_jti", length = 64)
    private String previousJti;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_reason", length = 50)
    private String revokedReason;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;
}
