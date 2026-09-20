package io.github.devang559.authkit.audit;

public interface AuditPublisher {
    void publish(AuditEvent event);
}
