package com.example.identitymanagementservice.exceptions;

import com.example.identitymanagementservice.common.constants.ErrorCode;
import com.example.identitymanagementservice.common.constants.ErrorMessage;
import com.example.identitymanagementservice.dto.request.pagenationdto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(TimesheetException.class)
    public ResponseEntity<ErrorResponse> handleKeycloakException(TimesheetException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .error_code(ex.getErrorCode())
                .message(ex.getMessage())
                .property("")
                .build();

        log.warn("KeycloakException caught: {}", response);
        return ResponseEntity.status(resolveHttpStatus(ex.getErrorCode())).body(response);
    }

    private HttpStatus resolveHttpStatus(String errorCode) {
        return switch (errorCode) {
            case ErrorCode.NOT_FOUND_ERROR -> HttpStatus.NOT_FOUND;
            case ErrorCode.CONFLICT_ERROR -> HttpStatus.CONFLICT;
            case ErrorCode.FORBIDDEN_ERROR -> HttpStatus.FORBIDDEN;
            case ErrorCode.UNAUTHORIZED_ERROR -> HttpStatus.UNAUTHORIZED;
            case ErrorCode.VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case ErrorCode.INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }





    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ErrorResponse>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ErrorResponse> errorResponses = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.builder()
                        .error_code(ErrorCode.VALIDATION_ERROR)
                        .message(error.getDefaultMessage())
                        .property(error.getField())
                        .build())
                .toList();

        log.warn("Validation errors: {}", errorResponses);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponses);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .error_code(ErrorCode.VALIDATION_ERROR)
                .message(ex.getMessage())
                .property("")
                .build();

        log.warn("Constraint violation: {}", response);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }




    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(SecurityException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .error_code(ErrorCode.FORBIDDEN_ERROR)
                .message(ErrorMessage.UNAUTHORIZED_ACCESS)
                .property("")
                .build();

        log.warn("Forbidden access: {}", response);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }





    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse response = ErrorResponse.builder()
                .error_code(ErrorCode.INTERNAL_SERVER_ERROR)
                .message(ErrorCode.INTERNAL_SERVER_ERROR)
                .property("")
                .build();

        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}