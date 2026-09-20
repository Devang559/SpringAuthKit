package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.config.AuthKitConfig;
import io.github.devang559.authkit.exception.InvalidPasswordException;
import io.github.devang559.authkit.exception.UserAlreadyExistsException;
import io.github.devang559.authkit.otp.OtpPurpose;
import io.github.devang559.authkit.otp.OtpService;
import io.github.devang559.authkit.password.PasswordService;
import io.github.devang559.authkit.role.RoleService;
import io.github.devang559.authkit.user.AuthUser;
import io.github.devang559.authkit.user.AuthUserService;
import io.github.devang559.authkit.user.UserField;
import io.github.devang559.authkit.web.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final AuthUserService userService;
    private final PasswordService passwordService;
    private final RoleService roleService;
    private final OtpService otpService;
    private final AuthKitConfig config;

    @Transactional
    public AuthUser register(RegisterRequest request) {
        checkUnique(UserField.EMAIL, request.getEmail());
        checkUnique(UserField.USERNAME, request.getUsername());
        checkUnique(UserField.PHONE, request.getPhone());

        String password = request.getPassword();
        if (!StringUtils.hasText(password)) {
            throw new InvalidPasswordException("Password is required");
        }
        passwordService.validate(password);
        String hashed = passwordService.hash(password);

        AuthUser user = new AuthUser(hashed);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDisplayName(request.getDisplayName());
        user.setAvatar(request.getAvatar());
        if (config.isFieldEnabled(UserField.EMAIL)) {
            user.setEmail(request.getEmail());
        }
        if (config.isFieldEnabled(UserField.USERNAME)) {
            user.setUsername(request.getUsername());
        }
        if (config.isFieldEnabled(UserField.PHONE)) {
            user.setPhone(request.getPhone());
        }
        user.setAccountStatus(config.initialAccountStatus());

        roleService.assignDefaultRole(user);
        AuthUser saved = userService.save(user);

        if (config.isEmailVerificationRequired()
                && config.isFieldEnabled(UserField.EMAIL)
                && StringUtils.hasText(saved.getEmail())) {
            otpService.sendOtp(saved.getEmail(), OtpPurpose.EMAIL_VERIFICATION);
        }
        return saved;
    }

    private void checkUnique(UserField field, String value) {
        if (!config.isFieldEnabled(field) || !StringUtils.hasText(value)) {
            return;
        }
        String mapped = mapField(field);
        if (userService.isFieldTaken(mapped, value)) {
            throw new UserAlreadyExistsException(field + " is already in use");
        }
    }

    private static String mapField(UserField field) {
        return switch (field) {
            case EMAIL -> "email";
            case USERNAME -> "username";
            case PHONE -> "phone";
            default -> "unknown";
        };
    }
}
