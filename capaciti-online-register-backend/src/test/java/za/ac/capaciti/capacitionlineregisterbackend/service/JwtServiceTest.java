package za.ac.capaciti.capacitionlineregisterbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = encodeSecret("jwt-test-secret-with-at-least-32-bytes");
    private static final String OTHER_SECRET = encodeSecret("different-jwt-test-secret-with-32-bytes");

    @Test
    void generatedTokenContainsTheSameEmail() {
        JwtService jwtService = createService(SECRET, 15);
        UserDetails user = enabledUser("candidate@example.com");

        String token = jwtService.generateToken(user);

        assertEquals(user.getUsername(), jwtService.extractEmail(token));
    }

    @Test
    void tokenIsInvalidForDifferentUsername() {
        JwtService jwtService = createService(SECRET, 15);
        String token = jwtService.generateToken(enabledUser("candidate@example.com"));

        assertFalse(jwtService.isTokenValid(token, enabledUser("other@example.com")));
    }

    @Test
    void tokenIsInvalidForDisabledUser() {
        JwtService jwtService = createService(SECRET, 15);
        String token = jwtService.generateToken(enabledUser("candidate@example.com"));
        UserDetails disabledUser = User.withUsername("candidate@example.com")
                .password("password")
                .authorities("ROLE_CANDIDATE")
                .disabled(true)
                .build();

        assertFalse(jwtService.isTokenValid(token, disabledUser));
    }

    @Test
    void tokenSignedWithDifferentSecretThrowsOnExtraction() {
        JwtService signingService = createService(SECRET, 15);
        JwtService validatingService = createService(OTHER_SECRET, 15);
        String token = signingService.generateToken(enabledUser("candidate@example.com"));

        assertThrows(JwtException.class, () -> validatingService.extractEmail(token));
    }

    @Test
    void expiredTokenThrowsOnExtraction() {
        JwtService jwtService = createService(SECRET, -1);
        String token = jwtService.generateToken(enabledUser("candidate@example.com"));

        assertThrows(JwtException.class, () -> jwtService.extractEmail(token));
    }

    @Test
    void blankSecretThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> createService(" ", 15));
    }

    @Test
    void shortSecretThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> createService(encodeSecret("short"), 15));
    }

    private static JwtService createService(String secret, long expirationMinutes) {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expirationMinutes", expirationMinutes);
        jwtService.initializeKey();
        return jwtService;
    }

    private static UserDetails enabledUser(String email) {
        return User.withUsername(email)
                .password("password")
                .authorities("ROLE_CANDIDATE")
                .build();
    }

    private static String encodeSecret(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
