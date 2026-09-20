package io.github.devang559.authkit.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthPermissionRepository extends JpaRepository<AuthPermission, UUID> {

    Optional<AuthPermission> findByName(String name);
}
