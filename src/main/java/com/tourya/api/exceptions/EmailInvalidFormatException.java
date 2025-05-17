package com.tourya.api.exceptions;

public class EmailInvalidFormatException extends RuntimeException {

    public EmailInvalidFormatException(String message) {
        super(message);
    }
}
