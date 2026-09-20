package io.github.devang559.authkit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.devang559.authkit.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.time.Instant;

public class AuthKitAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public AuthKitAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         org.springframework.security.core.AuthenticationException authException) {
        try {
            ErrorResponse error = new ErrorResponse();
            error.setTimestamp(Instant.now().toString());
            error.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            error.setCode("AUTHENTICATION_REQUIRED");
            error.setMessage("Authentication is required to access this resource");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), error);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write authentication error response", e);
        }
    }
}
