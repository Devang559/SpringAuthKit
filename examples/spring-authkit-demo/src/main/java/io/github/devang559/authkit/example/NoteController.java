package io.github.devang559.authkit.example;

import io.github.devang559.authkit.user.AuthKitUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class NoteController {

    @GetMapping("/notes")
    public Map<String, Object> notes(@AuthenticationPrincipal AuthKitUser principal) {
        return Map.of(
                "who", principal.getId(),
                "identifier", principal.getUsername(),
                "roles", principal.getRoleNames(),
                "authorities", principal.getAuthorities().stream()
                        .map(Object::toString).toList());
    }
}
