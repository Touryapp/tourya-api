package com.tourya.api.agents.operator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * IA-07: resultado de la validacion pre-upload de galeria (RN-013).
 *
 * <p>Cero costo LLM — es reglas puras del {@link com.tourya.api.services.GalleryValidator}
 * reformuladas como issues estructurados por imagen.</p>
 *
 * @param isValid     {@code true} si TODAS las imagenes pasan (0 issues).
 * @param issues      Problemas encontrados — vacio si {@code isValid=true}.
 * @param suggestions Consejos generales al operador (ej. "usa imagenes horizontales").
 */
@Builder
@Schema(description = "Resultado pre-upload de la galeria (sin LLM, cero costo)")
public record ValidateGalleryResponse(
        boolean isValid,
        List<GalleryIssue> issues,
        List<String> suggestions
) {

    /**
     * @param severity  ERROR bloquea el upload, WARNING solo advierte.
     * @param code      Codigo tecnico (ej. {@code TOO_LARGE}, {@code NOT_LANDSCAPE}).
     * @param message   Mensaje amigable en espanol para mostrar al operador.
     * @param fileIndex Indice 0-based de la imagen en el array del request.
     */
    public record GalleryIssue(
            Severity severity,
            String code,
            String message,
            int fileIndex
    ) {}

    public enum Severity { ERROR, WARNING }
}
