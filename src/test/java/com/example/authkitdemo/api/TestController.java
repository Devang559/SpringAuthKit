package com.example.authkitdemo.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public java.util.Map<String, Object> secured(Authentication authentication) {
        return java.util.Map.of("authenticated", authentication != null,
                "principal", authentication.getName());
    }
}
