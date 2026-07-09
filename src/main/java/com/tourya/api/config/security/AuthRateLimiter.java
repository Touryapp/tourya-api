package com.tourya.api.config.security;

import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.services.AppConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter in-memory por IP para endpoints /auth/**.
 *
 * <p>Ventana deslizante de 1 minuto: cada IP tiene un contador que se resetea cuando
 * pasa 1 minuto desde el {@code windowStartMs}. Cuando el contador excede el limite
 * configurado en {@code app_config.AUTH_RATE_LIMIT_PER_MINUTE}, {@code tryAcquire}
 * retorna {@code false} y el filtro devuelve HTTP 429.
 *
 * <p><b>Coordinacion entre instancias</b>: NO. El contador es por instancia de Cloud
 * Run. Con max-instances=2 el limite efectivo puede ser hasta 2x el configurado.
 * Aceptable como defensa en profundidad con umbrales generosos. Si se necesita
 * coordinacion estricta, migrar a Redis.
 *
 * <p><b>Memoria</b>: entradas viejas (>10 min sin actividad) se limpian cada 5 min via
 * {@code @Scheduled}. Cota superior: cantidad de IPs distintas activas en los ultimos
 * 5 min. Para volumen normal, footprint despreciable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRateLimiter {

    private static final long WINDOW_MS = 60_000L;
    private static final long STALE_ENTRY_MS = 10 * 60_000L;

    private final AppConfigService appConfigService;
    private final ConcurrentHashMap<String, Window> counters = new ConcurrentHashMap<>();

    /**
     * @return {@code true} si el request puede proceder; {@code false} si excedio el limite.
     */
    public boolean tryAcquire(String clientId) {
        int limit = appConfigService.getInt(ConfigKeyEnum.AUTH_RATE_LIMIT_PER_MINUTE, 60);
        long now = System.currentTimeMillis();

        Window window = counters.compute(clientId, (k, existing) -> {
            if (existing == null || now - existing.windowStartMs >= WINDOW_MS) {
                return new Window(now);
            }
            return existing;
        });

        int newCount = window.count.incrementAndGet();
        if (newCount > limit) {
            log.warn("Rate limit exceeded para clientId={} (count={}, limit={}/min)", clientId, newCount, limit);
            return false;
        }
        return true;
    }

    /**
     * Limpieza periodica de entradas viejas (>10 min sin actividad).
     * Cada 5 min por default; configurable si es necesario en el futuro.
     */
    @Scheduled(fixedDelayString = "${tourya.authRateLimit.cleanupFixedDelayMs:300000}")
    public void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Window>> iterator = counters.entrySet().iterator();
        int removed = 0;
        while (iterator.hasNext()) {
            Map.Entry<String, Window> entry = iterator.next();
            if (now - entry.getValue().windowStartMs > STALE_ENTRY_MS) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("AuthRateLimiter cleanup: {} entradas viejas removidas (activas: {})",
                    removed, counters.size());
        }
    }

    private static final class Window {
        final long windowStartMs;
        final AtomicInteger count;

        Window(long windowStartMs) {
            this.windowStartMs = windowStartMs;
            this.count = new AtomicInteger(0);
        }
    }
}
