package io.github.devang559.authkit.otp;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.*;
import io.github.devang559.authkit.ratelimit.RateLimitPolicy;
import io.github.devang559.authkit.ratelimit.RateLimiter;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import io.github.devang559.authkit.util.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final AuthUserService userService;
    private final OtpRepository otpRepository;
    private final OtpGenerator otpGenerator;
    private final io.github.devang559.authkit.email.EmailService emailService;
    private final RateLimiter rateLimiter;
    private final AuthKitConfig config;

    private OtpPolicy policy() {
        var e = config.getProperties().getEmail();
        return new OtpPolicy(e.getOtpLength(), e.getOtpExpiration(), e.getMaxAttempts());
    }

    private RateLimitPolicy otpRateLimitPolicy() {
        var r = config.getProperties().getRateLimit().getOtp();
        return new RateLimitPolicy(r.getMaxAttempts(), r.getWindow());
    }

    private boolean rateLimit(String key) {
        if (!config.isRateLimitEnabled()) {
            return true;
        }
        return rateLimiter.tryAcquire(key, otpRateLimitPolicy());
    }

    @Transactional
    public void sendOtp(String destination, OtpPurpose purpose) {
        if (!rateLimit("otp:send:" + destination)) {
            throw new RateLimitExceededException("Too many OTP requests. Try again later.", null);
        }
        AuthUser user = userService.findByEmail(destination)
                .orElseThrow(() -> new InvalidOtpException("No account found for the given address"));
        issueAndSend(user, destination, purpose);
    }

    @Transactional
    public void resendOtp(String destination, OtpPurpose purpose) {
        if (!rateLimit("otp:resend:" + destination)) {
            throw new RateLimitExceededException("Too many OTP requests. Try again later.", null);
        }
        AuthUser user = userService.findByEmail(destination)
                .orElseThrow(() -> new InvalidOtpException("No account found for the given address"));
        otpRepository.deleteByUserIdAndPurpose(user.getId(), purpose);
        issueAndSend(user, destination, purpose);
    }

    @Transactional
    public boolean verifyOtp(String destination, String otp, OtpPurpose purpose) {
        if (!rateLimit("otp:verify:" + destination)) {
            throw new RateLimitExceededException("Too many OTP attempts. Try again later.", null);
        }
        OtpPolicy policy = policy();
        List<Otp> list = otpRepository.findByDestinationAndPurposeOrderByCreatedAtDesc(destination, purpose);
        Optional<Otp> maybe = list.stream().filter(o -> !o.isUsed()).findFirst();
        Otp current = maybe.orElseThrow(() -> new InvalidOtpException("No valid OTP found"));

        if (current.isExpired()) {
            throw new OtpExpiredException("OTP has expired");
        }
        if (current.getAttempts() >= policy.getMaxAttempts()) {
            throw new OtpAttemptsExceededException("Maximum OTP attempts exceeded");
        }
        if (!HashUtils.sha256Hex(otp).equals(current.getCodeHash())) {
            current.setAttempts(current.getAttempts() + 1);
            otpRepository.save(current);
            if (current.getAttempts() >= policy.getMaxAttempts()) {
                throw new OtpAttemptsExceededException("Maximum OTP attempts exceeded");
            }
            throw new InvalidOtpException("Invalid OTP");
        }
        current.setUsed(true);
        current.setVerified(true);
        otpRepository.save(current);
        return true;
    }

    @Transactional
    public void issueAndSend(AuthUser user, String destination, OtpPurpose purpose) {
        OtpPolicy policy = policy();
        String code = otpGenerator.generate(policy.getLength());
        Instant expiresAt = Instant.now().plus(policy.getExpiration());
        Otp otp = new Otp(user, purpose, destination, HashUtils.sha256Hex(code), expiresAt);
        otpRepository.save(otp);
        emailService.sendOtp(destination, code);
    }
}
