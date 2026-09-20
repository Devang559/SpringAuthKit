package io.github.devang559.authkit.otp;

import java.security.SecureRandom;
import java.util.stream.IntStream;

public class SecureRandomOtpGenerator implements OtpGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String DIGITS = "0123456789";

    @Override
    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }
}
