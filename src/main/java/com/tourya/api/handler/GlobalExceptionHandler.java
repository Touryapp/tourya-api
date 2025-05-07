package com.tourya.api.handler;


import com.tourya.api.exceptions.EmailAlreadyExistsException;
import com.tourya.api.exceptions.EmailInvalidFormatException;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tourya.api.handler.BusinessErrorCodes.ACCOUNT_DISABLED;
import static com.tourya.api.handler.BusinessErrorCodes.ACCOUNT_LOCKED;
import static com.tourya.api.handler.BusinessErrorCodes.BAD_CREDENTIALS;
import static com.tourya.api.handler.BusinessErrorCodes.EMAIL_ALREADY_EXISTS;
import static com.tourya.api.handler.BusinessErrorCodes.EMAIL_INVALID_FORMAT;
import static com.tourya.api.handler.BusinessErrorCodes.VALIDATION_FAILURE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
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
        response.put("businessErrorCode", exceptionResponse.getBusinessErrorCode());
        response.put("businessErrorDescription", exceptionResponse.getBusinessErrorDescription());
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
                .businessErrorCode(ACCOUNT_LOCKED.getCode())
                .businessErrorDescription(ACCOUNT_LOCKED.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Object> handleException(DisabledException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .businessErrorCode(ACCOUNT_DISABLED.getCode())
                .businessErrorDescription(ACCOUNT_DISABLED.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleException() {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .businessErrorCode(BAD_CREDENTIALS.getCode())
                .businessErrorDescription(BAD_CREDENTIALS.getDescription())
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
                .businessErrorCode(VALIDATION_FAILURE.getCode())
                .businessErrorDescription(VALIDATION_FAILURE.getDescription())
                .validationErrors(errors)
                .error(VALIDATION_FAILURE.getDescription())
                .build();
        return buildErrorResponse(exceptionResponse, BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception exp) {
        exp.printStackTrace();
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .businessErrorDescription("Internal error, please contact the admin")
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Object> handleException(EmailAlreadyExistsException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .businessErrorCode(EMAIL_ALREADY_EXISTS.getCode())
                .businessErrorDescription(EMAIL_ALREADY_EXISTS.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, BAD_REQUEST);
    }

    @ExceptionHandler(EmailInvalidFormatException.class)
    public ResponseEntity<Object> handleException(EmailInvalidFormatException exp) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .businessErrorCode(EMAIL_INVALID_FORMAT.getCode())
                .businessErrorDescription(EMAIL_INVALID_FORMAT.getDescription())
                .error(exp.getMessage())
                .build();
        return buildErrorResponse(exceptionResponse, BAD_REQUEST);
    }
    /*@ExceptionHandler(LockedException.class)
    public ResponseEntity<ExceptionResponse> handleException(LockedException exp) {
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(ACCOUNT_LOCKED.getCode())
                                .businessErrorDescription(ACCOUNT_LOCKED.getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ExceptionResponse> handleException(DisabledException exp) {
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(ACCOUNT_DISABLED.getCode())
                                .businessErrorDescription(ACCOUNT_DISABLED.getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponse> handleException() {
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(BAD_CREDENTIALS.getCode())
                                .businessErrorDescription(BAD_CREDENTIALS.getDescription())
                                .error("Login and / or Password is incorrect")
                                .build()
                );
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ExceptionResponse> handleException(MessagingException exp) {
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(
                        ExceptionResponse.builder()
                                .error(exp.getMessage())
                                .build()
                );
    }



    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exp) {
        Set<String> errors = new HashSet<>();
        exp.getBindingResult().getAllErrors()
                .forEach(error -> {
                    //var fieldName = ((FieldError) error).getField();
                    var errorMessage = error.getDefaultMessage();
                    errors.add(errorMessage);
                });

        return ResponseEntity
                .status(BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .validationErrors(errors)
                                .build()
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleException(Exception exp) {
        exp.printStackTrace();
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorDescription("Internal error, please contact the admin")
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ExceptionResponse> handleException(EmailAlreadyExistsException exp) {
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(EMAIL_ALREADY_EXISTS.getCode())
                                .businessErrorDescription(EMAIL_ALREADY_EXISTS.getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }
    @ExceptionHandler(EmailInvalidFormatException.class)
    public ResponseEntity<ExceptionResponse> handleException(EmailInvalidFormatException exp) {
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(EMAIL_INVALID_FORMAT.getCode())
                                .businessErrorDescription(EMAIL_INVALID_FORMAT.getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }*/
}
