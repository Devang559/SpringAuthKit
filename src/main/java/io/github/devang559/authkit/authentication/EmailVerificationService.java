package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.EmailNotVerifiedException;
import io.github.devang559.authkit.lifecycle.AccountStatus;
import io.github.devang559.authkit.otp.OtpPurpose;
import io.github.devang559.authkit.otp.OtpService;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final AuthUserService userService;
    private final OtpService otpService;
    private final AuthKitConfig config;

    @Transactional
    public void verify(String email, String code) {
        AuthUser user = userService.findByEmail(email)
                .orElseThrow(() -> new EmailNotVerifiedException("No account found for the given email"));

        otpService.verifyOtp(email, code, OtpPurpose.EMAIL_VERIFICATION);

        userService.markEmailVerified(user.getId());
        if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            user.setAccountStatus(AccountStatus.ACTIVE);
            userService.save(user);
        }
    }
}
