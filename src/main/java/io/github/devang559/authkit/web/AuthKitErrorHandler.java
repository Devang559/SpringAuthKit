package io.github.devang559.authkit.web;

import io.github.devang559.authkit.exception.AuthKitException;
import io.github.devang559.authkit.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
@Order(0)
public class AuthKitErrorHandler {

    @ExceptionHandler(AuthKitException.class)
    public ResponseEntity<ErrorResponse> handleAuthKit(AuthKitException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(403, "ACCESS_DENIED", ex.getMessage());
        return ResponseEntity.status(403).body(body);
    }

    @ExceptionHandler({InsufficientAuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthentication(
            org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(401, "UNAUTHENTICATED", "Authentication is required");
        return ResponseEntity.status(401).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        ErrorResponse body = new ErrorResponse(400, "VALIDATION_ERROR", "Request validation failed");
        body.setDetails(details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        log.warn("SpringAuthKit: unhandled error on {}", request.getRequestURI(), ex);
        ErrorResponse body = new ErrorResponse(500, "INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(500).body(body);
    }
}
