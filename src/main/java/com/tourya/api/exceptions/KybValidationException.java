package com.tourya.api.exceptions;

import java.util.List;

/**
 * Se lanza al hacer submit de un RequestProvider si el feature flag
 * KYB_REQUIRE_MANDATORY_DOCS esta ON (=1) y faltan documentos obligatorios
 * (RequestProviderDocumentType con mandatory=true).
 * Mapeada a HTTP 400 en GlobalExceptionHandler con la lista de nombres
 * faltantes para que el frontend guie al usuario.
 */
public class KybValidationException extends RuntimeException {

    private final List<String> missingDocuments;

    public KybValidationException(List<String> missingDocuments) {
        super("KYB submit rechazado: faltan " + missingDocuments.size() + " documento(s) obligatorio(s)");
        this.missingDocuments = List.copyOf(missingDocuments);
    }

    public List<String> getMissingDocuments() {
        return missingDocuments;
    }
}
