package com.tourya.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * MO-40: token FCM/APNs registrado por un cliente mobile/web para recibir
 * notificaciones push. Un usuario puede tener varios (un token por dispositivo
 * activo). El token es unico globalmente — si el mismo device se registra bajo
 * otro user, se reasigna (upsert por token).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "device_token", uniqueConstraints = {
        @UniqueConstraint(columnNames = "token", name = "uk_device_token")
})
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String token;

    /** ANDROID | IOS | WEB — determina que canal FCM/APNs/WebPush usar. */
    @Column(nullable = false, length = 20)
    private String platform;

    // MO-40b (2026-09-15): fechas seteadas explicit por el DeviceTokenService,
    // NO por Spring Data Auditing. Motivo: Spring Data @CreatedDate / @LastModifiedDate
    // instancia LocalDateTime por default (via SpringDataJpaAuditingDateTimeProvider),
    // pero la columna Postgres es TIMESTAMPTZ que mapea a OffsetDateTime.
    // Al hacer .save() Hibernate lanzaba InvalidDataAccessApiUsageException:
    // "Cannot convert unsupported date type java.time.LocalDateTime to
    // java.time.OffsetDateTime" (Sentry TOURYA-MOBILE-4, 16 events escalating
    // en 7 dias). Sin @EntityListeners ni auditing annotations, el service
    // setea createdAt/updatedAt explicit con OffsetDateTime.now().
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
