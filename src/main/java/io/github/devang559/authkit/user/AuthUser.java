package io.github.devang559.authkit.user;

import io.github.devang559.authkit.lifecycle.AccountStatus;
import io.github.devang559.authkit.role.AuthRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "auth_users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthUser {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "username", length = 100, unique = true, nullable = true)
    private String username;

    @Column(name = "email", length = 254, unique = true, nullable = true)
    private String email;

    @Column(name = "phone", length = 30, unique = true, nullable = true)
    private String phone;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "account_status", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus = AccountStatus.PENDING_VERIFICATION;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "auth_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<AuthRole> roles = new HashSet<>();

    public AuthUser(String password) {
        this.password = password;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public void addRole(AuthRole role) {
        roles.add(role);
    }

    public void removeRole(AuthRole role) {
        roles.remove(role);
    }

    public boolean isLockedNow() {
        if (accountStatus == AccountStatus.LOCKED) {
            return true;
        }
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }
}
