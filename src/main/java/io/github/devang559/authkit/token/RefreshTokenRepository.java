package io.github.devang559.authkit.token;

import io.github.devang559.authkit.user.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);

    long countByUserIdAndRevokedFalseAndReplacedByIdIsNull(UUID userId);
}
