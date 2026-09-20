package io.github.devang559.authkit.web;

import io.github.devang559.authkit.authentication.AuthenticationService;
import io.github.devang559.authkit.authentication.EmailVerificationService;
import io.github.devang559.authkit.authentication.ForgotPasswordService;
import io.github.devang559.authkit.authentication.LogoutService;
import io.github.devang559.authkit.authentication.RegistrationService;
import io.github.devang559.authkit.otp.OtpPurpose;
import io.github.devang559.authkit.otp.OtpService;
import io.github.devang559.authkit.token.TokenPair;
import io.github.devang559.authkit.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final LogoutService logoutService;
    private final OtpService otpService;
    private final EmailVerificationService emailVerificationService;
    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair pair = authenticationService.login(request.getIdentifier(), request.getPassword());
        return ResponseEntity.ok(toLoginResponse(pair));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenPair pair = authenticationService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(toLoginResponse(pair));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshRequest request) {
        logoutService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<MessageResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        OtpPurpose purpose = resolvePurpose(request.getPurpose(), OtpPurpose.EMAIL_VERIFICATION);
        otpService.sendOtp(request.getEmail(), purpose);
        return ResponseEntity.ok(new MessageResponse("OTP sent"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        OtpPurpose purpose = resolvePurpose(request.getPurpose(), OtpPurpose.EMAIL_VERIFICATION);
        if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
            emailVerificationService.verifyEmail(request);
        } else {
            otpService.verifyOtp(request.getEmail(), request.getOtp(), purpose);
        }
        return ResponseEntity.ok(new MessageResponse("OTP verified"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        OtpPurpose purpose = resolvePurpose(request.getPurpose(), OtpPurpose.EMAIL_VERIFICATION);
        otpService.resendOtp(request.getEmail(), purpose);
        return ResponseEntity.ok(new MessageResponse("OTP resent"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordService.sendResetOtp(request.getEmail());
        return ResponseEntity.ok(new MessageResponse("If the account exists, a reset OTP has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        forgotPasswordService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password has been reset"));
    }

    private LoginResponse toLoginResponse(TokenPair pair) {
        return LoginResponse.builder()
                .accessToken(pair.getAccessToken())
                .refreshToken(pair.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(pair.getExpiresIn())
                .build();
    }

    private OtpPurpose resolvePurpose(SendOtpRequest.OtpPurposeType purpose, OtpPurpose defaultValue) {
        if (purpose == null) {
            return defaultValue;
        }
        return switch (purpose) {
            case EMAIL_VERIFICATION -> OtpPurpose.EMAIL_VERIFICATION;
            case PASSWORD_RESET -> OtpPurpose.PASSWORD_RESET;
        };
    }
}
