package io.github.devang559.authkit.authentication;

import io.github.devang559.authkit.audit.AuditEvent;
import io.github.devang559.authkit.audit.AuditEventType;
import io.github.devang559.authkit.audit.AuditService;
import io.github.devang559.authkit.token.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final TokenService tokenService;
    private final AuditService auditService;

    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
        auditService.publish(new AuditEvent(AuditEventType.LOGOUT, null, null, true, null));
    }
}
