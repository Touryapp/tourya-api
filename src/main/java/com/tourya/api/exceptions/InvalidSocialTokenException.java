package com.tourya.api.exceptions;

/**
 * SEC-06: se lanza cuando un token de un proveedor social (Google id_token o
 * Facebook accessToken) no supera la verificacion server-side. El
 * {@link com.tourya.api.handler.GlobalExceptionHandler} lo mapea a HTTP 401.
 */
public class InvalidSocialTokenException extends RuntimeException {
    public InvalidSocialTokenException(String message) {
        super(message);
    }
}
