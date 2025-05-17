package com.tourya.api.exceptions;

public class UnknownEnumValueException extends RuntimeException {

    public UnknownEnumValueException(String message) {
        super(message);
    }
}
