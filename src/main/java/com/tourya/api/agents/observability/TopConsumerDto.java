package com.tourya.api.agents.observability;

import java.math.BigDecimal;

/**
 * IA-11: top consumer (usuario) por calls y costo en el rango.
 *
 * <p>Se obtiene con {@code GROUP BY user_id} sobre {@code agent_audit_log} +
 * LEFT JOIN a {@code _user} para el email (el {@code user_id} puede ser
 * {@code NULL} — llamadas anónimas del Concierge pre-login, por ejemplo — y
 * en ese caso el email queda {@code null} y el "userId" se representa como
 * {@code null} en la respuesta).</p>
 */
public record TopConsumerDto(
        Integer userId,
        String userEmail,
        long calls,
        BigDecimal costUsd
) {}
