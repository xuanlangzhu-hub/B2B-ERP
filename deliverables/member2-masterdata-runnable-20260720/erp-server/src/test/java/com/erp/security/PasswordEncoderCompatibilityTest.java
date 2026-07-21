package com.erp.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderCompatibilityTest {

    private final PasswordEncoder passwordEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void acceptsNoopPasswordFromDemoSeed() {
        assertTrue(passwordEncoder.matches("Admin@123456", "{noop}Admin@123456"));
    }

    @Test
    void encodesNewPasswordsWithBcrypt() {
        String encoded = passwordEncoder.encode("123456");
        assertTrue(encoded.startsWith("{bcrypt}"));
        assertTrue(passwordEncoder.matches("123456", encoded));
    }
}
