package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.audit.AuditEvent;
import io.github.devang559.authkit.audit.AuditEventType;
import io.github.devang559.authkit.audit.AuditService;
import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.UserAlreadyExistsException;
import io.github.devang559.authkit.otp.OtpPurpose;
import io.github.devang559.authkit.otp.OtpService;
import io.github.devang559.authkit.password.PasswordService;
import io.github.devang559.authkit.role.RoleService;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import io.github.devang559.authkit.user.UserField;
import io.github.devang559.authkit.web.dto.RegisterRequest;
import io.github.devang559.authkit.web.dto.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final AuthKitConfig config;
    private final PasswordService passwordService;
    private final AuthUserService userService;
    private final RoleService roleService;
    private final OtpService otpService;
    private final AuditService auditService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        validateFields(request);
        passwordService.validate(request.getPassword());
        checkUniqueness(request);

        AuthUser user = new AuthUser(passwordService.hash(request.getPassword()));
        applyFields(user, request);
        user.setAccountStatus(config.initialAccountStatus());
        roleService.assignDefaultRole(user);
        AuthUser saved = userService.save(user);

        boolean verificationRequired = config.isEmailVerificationRequired();
        if (verificationRequired) {
            otpService.sendOtp(saved.getEmail(), OtpPurpose.EMAIL_VERIFICATION);
            auditService.publish(new AuditEvent(
                    AuditEventType.REGISTRATION_OTP_SENT, saved.getId(), saved.getEmail(), true, null));
        }

        auditService.publish(new AuditEvent(
                AuditEventType.USER_REGISTERED, saved.getId(), saved.getEmail(), true, null));

        return RegisterResponse.builder()
                .userId(saved.getId())
                .emailVerificationRequired(verificationRequired)
                .emailVerified(false)
                .message("Registration successful")
                .build();
    }

    private void validateFields(RegisterRequest request) {
        if (config.isFieldEnabled(UserField.USERNAME) && request.getUsername() != null && !request.getUsername().isBlank()) {
            if (config.isFieldRequired(UserField.USERNAME) && request.getUsername().isBlank()) {
                throw new IllegalArgumentException("username is required");
            }
        }
        if (config.isFieldEnabled(UserField.EMAIL) && !isBlank(request.getEmail())) {
            if (config.isFieldRequired(UserField.EMAIL) && request.getEmail() == null) {
                throw new IllegalArgumentException("email is required");
            }
        }
        if (config.isFieldEnabled(UserField.FIRST_NAME) && request.getFirstName() != null && !request.getFirstName().isBlank()) {
            if (config.isFieldRequired(UserField.FIRST_NAME) && request.getFirstName().isBlank()) {
                throw new IllegalArgumentException("firstName is required");
            }
        }
        if (config.isFieldEnabled(UserField.LAST_NAME) && request.getLastName() != null && !request.getLastName().isBlank()) {
            if (config.isFieldRequired(UserField.LAST_NAME) && request.getLastName().isBlank()) {
                throw new IllegalArgumentException("lastName is required");
            }
        }
    }

    private void checkUniqueness(RegisterRequest request) {
        if (config.isFieldEnabled(UserField.EMAIL) && isNotBlank(request.getEmail())
                && userService.isFieldTaken("email", request.getEmail())) {
            throw new UserAlreadyExistsException("An account with this email already exists");
        }
        if (config.isFieldEnabled(UserField.USERNAME) && isNotBlank(request.getUsername())
                && userService.isFieldTaken("username", request.getUsername())) {
            throw new UserAlreadyExistsException("An account with this username already exists");
        }
        if (config.isFieldEnabled(UserField.PHONE) && isNotBlank(request.getPhone())
                && userService.isFieldTaken("phone", request.getPhone())) {
            throw new UserAlreadyExistsException("An account with this phone already exists");
        }
    }

    private void applyFields(AuthUser user, RegisterRequest request) {
        if (config.isFieldEnabled(UserField.USERNAME) && isNotBlank(request.getUsername())) {
            user.setUsername(request.getUsername());
        }
        if (config.isFieldEnabled(UserField.EMAIL) && isNotBlank(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (config.isFieldEnabled(UserField.PHONE) && isNotBlank(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (config.isFieldEnabled(UserField.FIRST_NAME) && isNotBlank(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (config.isFieldEnabled(UserField.LAST_NAME) && isNotBlank(request.getLastName())) {
            user.setLastName(request.getLastName());
        }
        if (config.isFieldEnabled(UserField.DISPLAY_NAME) && isNotBlank(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName());
        }
        if (config.isFieldEnabled(UserField.AVATAR) && isNotBlank(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
