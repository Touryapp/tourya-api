package com.tourya.api.agents.backoffice;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * IA-08: borrador de manifiesto DIMAR (RN-054) para un provider en una fecha.
 *
 * <p><b>Sin LLM</b> — pura agregacion estructurada. Se persiste igual en
 * {@code agent_audit_log} para trackear volumen (capability = {@code dimar_draft},
 * {@code tokens_in=0}, {@code tokens_out=0}).</p>
 *
 * @param date              Fecha del zarpe.
 * @param providerId        Id del provider destinatario.
 * @param totalPassengers   Suma de {@link DimarPassengerRow#quantity} de todas las filas.
 * @param passengers        Detalle por pasajero — el humano revisa antes de subir.
 * @param notes             Nota informativa sobre volumen y proximo paso.
 */
@Builder
public record DimarDraftResponse(
        LocalDate date,
        Integer providerId,
        Integer totalPassengers,
        List<DimarPassengerRow> passengers,
        String notes
) {}
