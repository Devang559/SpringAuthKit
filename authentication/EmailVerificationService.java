package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.audit.AuditEvent;
import io.github.devang559.authkit.audit.AuditEventType;
import io.github.devang559.authkit.audit.AuditService;
import io.github.devang559.authkit.exception.InvalidOtpException;
import io.github.devang559.authkit.lifecycle.AccountStatus;
import io.github.devang559.authkit.otp.OtpPurpose;
import io.github.devang559.authkit.otp.OtpService;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import io.github.devang559.authkit.web.dto.VerifyOtpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final AuthUserService userService;
    private final OtpService otpService;
    private final AuditService auditService;

    @Transactional
    public boolean verifyEmail(VerifyOtpRequest request) {
        boolean verified = otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpPurpose.EMAIL_VERIFICATION);
        if (!verified) {
            throw new InvalidOtpException("Invalid or expired OTP");
        }
        AuthUser user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP"));
        userService.markEmailVerified(user.getId());
        user.setAccountStatus(AccountStatus.ACTIVE);
        userService.save(user);
        auditService.publish(new AuditEvent(
                AuditEventType.EMAIL_VERIFIED, user.getId(), user.getEmail(), true, null));
        return true;
    }
}
