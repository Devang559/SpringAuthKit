package io.github.devang559.authkit.support;

import io.github.devang559.authkit.email.EmailService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CapturingEmailService implements EmailService {

    private final Map<String, String> lastOtpByDestination = new ConcurrentHashMap<>();

    @Override
    public void sendOtp(String destination, String otp) {
        lastOtpByDestination.put(destination, otp);
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
    }

    public String getLastOtp(String destination) {
        return lastOtpByDestination.get(destination);
    }
}
