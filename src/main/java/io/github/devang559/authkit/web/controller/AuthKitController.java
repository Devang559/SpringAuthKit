package io.github.devang559.authkit.web.controller;

import io.github.devang559.authkit.authentication.AuthenticationService;
import io.github.devang559.authkit.authentication.EmailVerificationService;
import io.github.devang559.authkit.authentication.PasswordResetService;
import io.github.devang559.authkit.authentication.RegistrationService;
import io.github.devang559.authkit.otp.OtpPurpose;
import io.github.devang559.authkit.otp.OtpService;
import io.github.devang559.authkit.web.dto.ForgotPasswordRequest;
import io.github.devang559.authkit.web.dto.LoginRequest;
import io.github.devang559.authkit.web.dto.LoginResponse;
import io.github.devang559.authkit.web.dto.MessageResponse;
import io.github.devang559.authkit.web.dto.RefreshRequest;
import io.github.devang559.authkit.web.dto.RegisterRequest;
import io.github.devang559.authkit.web.dto.ResendOtpRequest;
import io.github.devang559.authkit.web.dto.ResetPasswordRequest;
import io.github.devang559.authkit.web.dto.SendOtpRequest;
import io.github.devang559.authkit.web.dto.VerifyOtpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthKitController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final OtpService otpService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return MessageResponse.builder()
                .message("Registration successful. Verify your email to activate your account.")
                .build();
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.authenticate(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authenticationService.refreshToken(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshRequest request) {
        authenticationService.logout(request);
        return MessageResponse.builder().message("Logged out successfully").build();
    }

    @PostMapping("/send-otp")
    public MessageResponse sendOtp(@Valid @RequestBody SendOtpRequest request) {
        otpService.sendOtp(request.getEmail(), toPurpose(request.getPurpose()));
        return MessageResponse.builder().message("OTP sent").build();
    }

    @PostMapping("/verify-otp")
    public MessageResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        OtpPurpose purpose = toPurpose(request.getPurpose());
        if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
            emailVerificationService.verify(request.getEmail(), request.getOtp());
        } else {
            otpService.verifyOtp(request.getEmail(), request.getOtp(), purpose);
        }
        return MessageResponse.builder().message("OTP verified").build();
    }

    @PostMapping("/resend-otp")
    public MessageResponse resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        otpService.resendOtp(request.getEmail(), toPurpose(request.getPurpose()));
        return MessageResponse.builder().message("OTP sent").build();
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.forgotPassword(request.getEmail());
        return MessageResponse.builder()
                .message("If the email exists, a password reset OTP has been sent")
                .build();
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request);
        return MessageResponse.builder().message("Password reset successful").build();
    }

    private OtpPurpose toPurpose(SendOtpRequest.OtpPurposeType type) {
        if (type == null) {
            return OtpPurpose.EMAIL_VERIFICATION;
        }
        return switch (type) {
            case EMAIL_VERIFICATION -> OtpPurpose.EMAIL_VERIFICATION;
            case PASSWORD_RESET -> OtpPurpose.PASSWORD_RESET;
        };
    }
}
