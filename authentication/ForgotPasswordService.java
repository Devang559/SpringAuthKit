package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.audit.AuditEvent;
import io.github.devang559.authkit.audit.AuditEventType;
import io.github.devang559.authkit.audit.AuditService;
import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.InvalidOtpException;
import io.github.devang559.authkit.exception.InvalidPasswordException;
import io.github.devang559.authkit.otp.OtpPurpose;
import io.github.devang559.authkit.otp.OtpService;
import io.github.devang559.authkit.password.PasswordService;
import io.github.devang559.authkit.token.TokenService;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import io.github.devang559.authkit.web.dto.ResetPasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final AuthKitConfig config;
    private final AuthUserService userService;
    private final OtpService otpService;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final AuditService auditService;

    public void sendResetOtp(String email) {
        try {
            otpService.sendOtp(email, OtpPurpose.PASSWORD_RESET);
        } catch (InvalidOtpException e) {
            // Do not reveal whether an account exists.
        }
    }

    @Transactional
    public AuthUser resetPassword(ResetPasswordRequest request) {
        if (!otpService.verifyOtp(request.getEmail(), request.getToken(), OtpPurpose.PASSWORD_RESET)) {
            throw new InvalidOtpException("Invalid or expired reset token");
        }
        AuthUser user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired reset token"));
        passwordService.validate(request.getPassword());
        user.setPassword(passwordService.hash(request.getPassword()));
        userService.save(user);
        tokenService.revokeAllUserRefreshTokens(user.getId());
        auditService.publish(new AuditEvent(
                AuditEventType.PASSWORD_RESET, user.getId(), user.getEmail(), true, null));
        return user;
    }
}
