package io.github.devang559.authkit.audit;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService implements AuditPublisher {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void publish(AuditEvent event) {
        HttpServletRequest request = currentRequest();
        String clientIp = clientIp(request);
        String userAgent = userAgent(request);
        AuditLog record = new AuditLog(
                event.getEventType(),
                event.getUserId(),
                event.getIdentifier(),
                clientIp,
                userAgent,
                event.isSuccess(),
                event.getDetails()
        );
        try {
            auditLogRepository.save(record);
        } catch (Exception e) {
            log.warn("Failed to persist audit event: {}", event.getEventType(), e);
        }
        log.info("Audit: {} user={} identifier={} success={} ip={} detail={}",
                event.getEventType(), event.getUserId(), event.getIdentifier(),
                event.isSuccess(), clientIp, event.getDetails());
    }

    @Nullable
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(header)) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }
}
