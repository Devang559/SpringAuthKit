package io.github.devang559.authkit.email;

public interface EmailService {

    void sendOtp(String destination, String otp);

    void sendEmail(String to, String subject, String body);
}
