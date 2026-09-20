package io.github.devang559.authkit.email;

import io.github.devang559.authkit.config.AuthKitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
public class DefaultEmailService implements EmailService {

    private final AuthKitConfig config;

    public DefaultEmailService(AuthKitConfig config) {
        this.config = config;
    }

    @Override
    public void sendOtp(String destination, String otp) {
        Assert.hasText(destination, "Destination is required");
        Assert.hasText(otp, "OTP is required");
        log.info("SpringAuthKit: OTP issued for destination '{}' (length={}). No email provider configured; " +
                        "override the EmailService bean to deliver real emails. The OTP code is not logged.",
                destination, otp.length());
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        Assert.hasText(to, "Recipient is required");
        log.info("SpringAuthKit: email queued for destination '{}' (subject='{}'). " +
                        "No email provider configured; override the EmailService bean to deliver real emails.",
                to, subject);
    }
}
