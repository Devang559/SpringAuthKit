package io.github.devang559.authkit.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_audit_events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "event_type", length = 50, nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "identifier", length = 254)
    private String identifier;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "success", nullable = false, updatable = false)
    private boolean success;

    @Column(name = "details")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AuditLog(AuditEventType eventType, UUID userId, String identifier,
                    String clientIp, String userAgent, boolean success, String details) {
        this.eventType = eventType;
        this.userId = userId;
        this.identifier = identifier;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.success = success;
        this.details = details;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
