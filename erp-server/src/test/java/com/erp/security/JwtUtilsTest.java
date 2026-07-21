package com.erp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret",
                "test-jwt-secret-key-that-is-long-enough-for-hs256-signing");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 60_000L);
    }

    @Test
    void tokenRoundTripKeepsUserIdentity() {
        String token = jwtUtils.generateToken(7L, "admin", 3L);

        assertTrue(jwtUtils.validateToken(token));
        assertEquals(7L, jwtUtils.getUserId(token));
        assertEquals("admin", jwtUtils.getUsername(token));
        assertEquals(3L, jwtUtils.getEnterpriseId(token));
    }
}
