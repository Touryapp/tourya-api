package com.tourya.api.services;

import com.tourya.api.models.DeviceToken;
import com.tourya.api.models.User;
import com.tourya.api.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * MO-40 Fase A: gestion de tokens FCM/APNs.
 *
 * <p>Comportamiento del upsert: el mismo token puede aparecer bajo distintos
 * usuarios si un dispositivo se comparte (poco frecuente). Cuando eso pasa,
 * reasignamos el token al usuario actual sin duplicar. Esto respeta la
 * uniqueness a nivel de token (un dispositivo = un canal).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void register(String token, String platform, Authentication auth) {
        User user = (User) auth.getPrincipal();

        deviceTokenRepository.findByToken(token).ifPresentOrElse(existing -> {
            // Reasignar si esta bajo otro user; siempre bumpear updated_at.
            existing.setUser(user);
            existing.setPlatform(platform);
            existing.setUpdatedAt(OffsetDateTime.now());
            deviceTokenRepository.save(existing);
            log.debug("Device token refreshed for userId={}", user.getId());
        }, () -> {
            DeviceToken dt = DeviceToken.builder()
                    .user(user)
                    .token(token)
                    .platform(platform)
                    .build();
            deviceTokenRepository.save(dt);
            log.info("Device token registered for userId={} platform={}", user.getId(), platform);
        });
    }

    @Transactional
    public void unregister(String token, Authentication auth) {
        User user = (User) auth.getPrincipal();
        // Solo el owner del token puede quitarlo. Silencioso si no existe o no
        // es del owner — no filtramos existencia al caller.
        deviceTokenRepository.deleteByUserIdAndToken(user.getId(), token);
        log.info("Device token unregistered for userId={}", user.getId());
    }

    @Transactional(readOnly = true)
    public List<DeviceToken> findByUserId(Integer userId) {
        return deviceTokenRepository.findByUserId(userId);
    }
}
