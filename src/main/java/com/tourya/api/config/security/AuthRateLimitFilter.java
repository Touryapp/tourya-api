package com.tourya.api.config.security;

import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.services.AppConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de rate limiting para todas las rutas bajo /auth/**.
 *
 * <p>Cuando {@code app_config.AUTH_RATE_LIMIT_ENABLED = 1} y una IP excede
 * {@code AUTH_RATE_LIMIT_PER_MINUTE} requests por minuto, este filtro corta con HTTP
 * 429 (Too Many Requests) y header {@code Retry-After: 60}. En cualquier otro caso
 * (flag OFF, ruta fuera de /auth, dentro del limite) hace passthrough al siguiente
 * filtro (JwtFilter y luego el controller).
 *
 * <p><b>Identificacion del cliente</b>: se usa la primera IP del header
 * {@code X-Forwarded-For} (formato tipico detras de Cloud Run + Load Balancer).
 * Fallback a {@code request.getRemoteAddr()} si el header no esta presente.
 * Sabemos que un atacante puede falsificar {@code X-Forwarded-For}; para Cloud Run
 * ese header es reescrito por el load balancer y no puede spoofearse desde fuera.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/auth";
    private static final long RETRY_AFTER_SECONDS = 60L;

    private final AuthRateLimiter rateLimiter;
    private final AppConfigService appConfigService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!request.getServletPath().startsWith(AUTH_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (appConfigService.getInt(ConfigKeyEnum.AUTH_RATE_LIMIT_ENABLED, 0) != 1) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        if (!rateLimiter.tryAcquire(clientIp)) {
            response.setStatus(429); // HTTP 429 Too Many Requests (no hay constante en jakarta.servlet)
            response.setHeader("Retry-After", String.valueOf(RETRY_AFTER_SECONDS));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"errorCode\":null,\"message\":\"Too many requests\","
                    + "\"error\":\"Rate limit excedido. Reintente en " + RETRY_AFTER_SECONDS + "s.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int commaIdx = xff.indexOf(',');
            return commaIdx > 0 ? xff.substring(0, commaIdx).trim() : xff.trim();
        }
        return request.getRemoteAddr();
    }
}
