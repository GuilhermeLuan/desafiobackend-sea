package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-of-length!!";
    private static final String ISSUER = "test-issuer";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = newJwtUtils(SECRET, ISSUER, 3600);
    }

    private static JwtUtils newJwtUtils(String secret, String issuer, long expiration) {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", secret);
        ReflectionTestUtils.setField(utils, "jwtIssuer", issuer);
        ReflectionTestUtils.setField(utils, "jwtExpiration", expiration);
        utils.init();
        return utils;
    }

    @Test
    void generatedTokenCarriesUsernameAndRole() {
        String token = jwtUtils.generateToken("user@example.com", Role.ANALYST);

        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        assertThat(jwtUtils.getUserFromToken(token)).isEqualTo("user@example.com");
        assertThat(jwtUtils.getRoleFromToken(token)).isEqualTo(Role.ANALYST);
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtUtils expiring = newJwtUtils(SECRET, ISSUER, -10);
        String token = expiring.generateToken("user@example.com", Role.CLIENT);

        assertThat(expiring.validateJwtToken(token)).isFalse();
        assertThatThrownBy(() -> expiring.getClaims(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedTokenIsInvalid() {
        String token = jwtUtils.generateToken("user@example.com", Role.CLIENT);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtUtils.validateJwtToken(tampered)).isFalse();
    }

    @Test
    void tokenFromOtherIssuerIsInvalid() {
        String token = newJwtUtils(SECRET, "other-issuer", 3600)
                .generateToken("user@example.com", Role.CLIENT);

        assertThat(jwtUtils.validateJwtToken(token)).isFalse();
    }

    @Test
    void nullOrBlankTokenThrowsJwtException() {
        assertThat(jwtUtils.validateJwtToken(null)).isFalse();
        assertThatThrownBy(() -> jwtUtils.getUserFromToken(null)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwtUtils.getRoleFromToken("")).isInstanceOf(JwtException.class);
    }

    @Test
    void generateTokenRejectsInvalidArguments() {
        assertThatThrownBy(() -> jwtUtils.generateToken(" ", Role.CLIENT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwtUtils.generateToken("user@example.com", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
