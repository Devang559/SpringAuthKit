package io.github.devang559.authkit.otp;

import io.github.devang559.authkit.user.AuthUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_otps")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Otp {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AuthUser user;

    @Column(name = "purpose", length = 30, nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    @Column(name = "destination", length = 254, nullable = false, updatable = false)
    private String destination;

    @Column(name = "code_hash", length = 255, nullable = false, updatable = false)
    private String codeHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    public Otp(AuthUser user, OtpPurpose purpose, String destination, String codeHash, Instant expiresAt) {
        this.user = user;
        this.purpose = purpose;
        this.destination = destination;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
