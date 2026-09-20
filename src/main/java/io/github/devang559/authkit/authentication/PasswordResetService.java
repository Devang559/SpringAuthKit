package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.exception.InvalidOtpException;
import io.github.devang559.authkit.otp.OtpPurpose;
import io.github.devang559.authkit.otp.OtpService;
import io.github.devang559.authkit.password.PasswordService;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import io.github.devang559.authkit.web.dto.ResetPasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final AuthUserService userService;
    private final OtpService otpService;
    private final PasswordService passwordService;

    @Transactional
    public void forgotPassword(String email) {
        userService.findByEmail(email).ifPresent(u ->
                otpService.sendOtp(email, OtpPurpose.PASSWORD_RESET));
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        String email = request.getEmail();
        String code = request.getToken();
        String newPassword = request.getPassword();

        passwordService.validate(newPassword);

        AuthUser user = userService.findByEmail(email)
                .orElseThrow(() -> new InvalidOtpException("No account found for the given email"));

        otpService.verifyOtp(email, code, OtpPurpose.PASSWORD_RESET);

        user.setPassword(passwordService.hash(newPassword));
        userService.save(user);
    }
}
