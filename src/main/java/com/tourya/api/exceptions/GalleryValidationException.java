package com.tourya.api.exceptions;

import java.util.List;

/**
 * Se lanza cuando una o mas imagenes de la galeria de un tour fallan las validaciones
 * de RN-013 (tamano, formato, dimensiones, cuenta). Contiene la lista completa de issues
 * para que el frontend pueda mostrar el detalle por archivo.
 * Mapeada a HTTP 400 en GlobalExceptionHandler.
 */
public class GalleryValidationException extends RuntimeException {

    private final List<GalleryValidationIssue> issues;

    public GalleryValidationException(List<GalleryValidationIssue> issues) {
        super("Gallery validation failed: " + issues.size() + " issue(s)");
        this.issues = List.copyOf(issues);
    }

    public List<GalleryValidationIssue> getIssues() {
        return issues;
    }

    public record GalleryValidationIssue(String fileName, String code, String message) {}
}
