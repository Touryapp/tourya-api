package com.tourya.api.handler;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.networknt.schema.ValidationMessage;
import com.tourya.api.exceptions.EmailAlreadyExistsException;
import com.tourya.api.exceptions.EmailInvalidFormatException;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.JsonSchemaValidationException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.exceptions.UnknownEnumValueException;
import jakarta.mail.MessagingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tourya.api.handler.BusinessErrorCodes.ACCOUNT_DISABLED;
import static com.tourya.api.handler.BusinessErrorCodes.ACCOUNT_LOCKED;
import static com.tourya.api.handler.BusinessErrorCodes.BAD_CREDENTIALS;
import static com.tourya.api.handler.BusinessErrorCodes.EMAIL_ALREADY_EXISTS;
import static com.tourya.api.handler.BusinessErrorCodes.EMAIL_INVALID_FORMAT;
import static com.tourya.api.handler.BusinessErrorCodes.JSON_PARSE_ERROR;
import static com.tourya.api.handler.BusinessErrorCodes.OPERATION_NOT_PERMITTED;
import static com.tourya.api.handler.BusinessErrorCodes.RESOURCE_NOT_FOUND;
import static com.tourya.api.handler.BusinessErrorCodes.UNKNOWN_ENUM;
import static com.tourya.api.handler.BusinessErrorCodes.VALIDATION_FAILURE;
import static com.tourya.api.handler.BusinessErrorCodes.NOT_PRIVILEGES_TO_ACTION;
import static com.tourya.api.handler.BusinessErrorCodes.JSON_SCHEMA_VALIDATION_FAILURE; // <-- Si lo añades
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private Map<String, Object> buildMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("messageUid", UUID.randomUUID().toString());
        meta.put("requestDt", OffsetDateTime.now().toString());
        return meta;
    }

    private ResponseEntity<Object> buildErrorResponse(ExceptionResponse exceptionResponse, org.springframework.http.HttpStatus httpStatus) {
        Map<String, Object> response = new HashMap<>();
        response.put("meta", buildMeta());
        response.put("errorCode", exceptionResponse.getErrorCode());
        response.put("message", exceptionResponse.getMessage());
        response.put("error", exceptionResponse.getError());
        if (exceptionResponse.getValidationErrors() != null) {
            response.put("validationErrors", exceptionResponse.getValidationErrors());
        }
        if (exceptionResponse.getErrors() != null) {
            response.put("errors", exceptionResponse.getErrors());
        }
        return new ResponseEntity<>(response, httpStatus);
    }
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Object> handleException(LockedException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .errorCode(ACCOUNT_LOCKED.getCode())
                .message(ACCOUNT_LOCKED.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Object> handleException(DisabledException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .errorCode(ACCOUNT_DISABLED.getCode())
                .message(ACCOUNT_DISABLED.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleException() {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .errorCode(BAD_CREDENTIALS.getCode())
                .message(BAD_CREDENTIALS.getDescription())
                .error("Login and / or Password is incorrect")
                .build();
        return buildErrorResponse(exceptionResponse, UNAUTHORIZED);
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<Object> handleException(MessagingException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException exp) {
        Set<String> errors = new HashSet<>();
        exp.getBindingResult().getAllErrors()
                .forEach(error -> {
                    var errorMessage = error.getDefaultMessage();
                    errors.add(errorMessage);
                });

        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .errorCode(VALIDATION_FAILURE.getCode())
                .message(VALIDATION_FAILURE.getDescription())
                .validationErrors(errors)
                .error(VALIDATION_FAILURE.getDescription())
                .build();
        return buildErrorResponse(exceptionResponse, BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception exp) {
        exp.printStackTrace();
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message("Internal error, please contact the admin")
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Object> handleException(EmailAlreadyExistsException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .errorCode(EMAIL_ALREADY_EXISTS.getCode())
                .message(EMAIL_ALREADY_EXISTS.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, BAD_REQUEST);
    }

    @ExceptionHandler(EmailInvalidFormatException.class)
    public ResponseEntity<Object> handleException(EmailInvalidFormatException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .errorCode(EMAIL_INVALID_FORMAT.getCode())
                .message(EMAIL_INVALID_FORMAT.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, BAD_REQUEST);
    }
    @ExceptionHandler(InsufficientPrivilegesException.class)
    public ResponseEntity<Object> handleException(InsufficientPrivilegesException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .errorCode(NOT_PRIVILEGES_TO_ACTION.getCode())
                .message(NOT_PRIVILEGES_TO_ACTION.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, UNAUTHORIZED);
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleException(ResourceNotFoundException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .errorCode(RESOURCE_NOT_FOUND.getCode())
                .message(RESOURCE_NOT_FOUND.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, NOT_FOUND);
    }
    @ExceptionHandler(UnknownEnumValueException.class)
    public ResponseEntity<Object> handleException(UnknownEnumValueException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .errorCode(UNKNOWN_ENUM.getCode())
                .message(UNKNOWN_ENUM.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, BAD_REQUEST);
    }
    @ExceptionHandler(OperationNotPermittedException.class)
    public ResponseEntity<Object> handleException(OperationNotPermittedException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .errorCode(OPERATION_NOT_PERMITTED.getCode())
                .message(OPERATION_NOT_PERMITTED.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, BAD_REQUEST);
    }
    // --- Manejador para JsonSchemaValidationException ---
    @ExceptionHandler(JsonSchemaValidationException.class)
    public ResponseEntity<Object> handleJsonSchemaValidationException(JsonSchemaValidationException exp) {
        // Convertir el Set<ValidationMessage> a un Set<String> para 'validationErrors'
        Set<String> validationErrors = exp.getValidationMessages().stream()
                .map(ValidationMessage::getMessage)
                .collect(Collectors.toSet());

        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                // Usar tu nuevo BusinessErrorCodes si lo creaste, o el existente VALIDATION_FAILURE
                .errorCode(JSON_SCHEMA_VALIDATION_FAILURE.getCode()) // O VALIDATION_FAILURE.getCode()
                .message(JSON_SCHEMA_VALIDATION_FAILURE.getDescription()) // O VALIDATION_FAILURE.getDescription()
                .error(exp.getMessage()) // Mensaje general de la excepción
                .validationErrors(validationErrors) // Añade los errores de validación específicos
                .build();
        return buildErrorResponse(exceptionResponse, BAD_REQUEST);
    }
    // --- NUEVO MANEJADOR PARA JSON MAL FORMADO ---
}
