package io.github.devang559.authkit.otp;

import io.github.devang559.authkit.user.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {

    List<Otp> findTopByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, OtpPurpose purpose);

    List<Otp> findAllByUserIdAndPurpose(UUID userId, OtpPurpose purpose);

    List<Otp> findByDestinationAndPurposeOrderByCreatedAtDesc(String destination, OtpPurpose purpose);

    void deleteByUserIdAndPurpose(UUID userId, OtpPurpose purpose);
}
