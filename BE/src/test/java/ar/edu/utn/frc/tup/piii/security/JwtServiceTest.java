package ar.edu.utn.frc.tup.piii.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void generateToken_extractsClaimsAndValidates() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken(7L, "ash");

        assertEquals("ash", jwtService.extractUsername(token));
        assertEquals(7L, jwtService.extractPlayerId(token));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForInvalidToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        assertFalse(jwtService.isTokenValid("not-a-token"));
    }
}
