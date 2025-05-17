package com.tourya.api.exceptions;

public class InsufficientPrivilegesException extends RuntimeException {

    public InsufficientPrivilegesException(String message) {
        super(message);
    }
}
