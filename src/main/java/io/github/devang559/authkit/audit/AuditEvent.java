package io.github.devang559.authkit.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuditEvent {
    private final AuditEventType eventType;
    private final UUID userId;
    private final String identifier;
    private final boolean success;
    private final String details;
}
