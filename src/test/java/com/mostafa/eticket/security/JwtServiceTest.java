package com.mostafa.eticket.security;

import com.mostafa.eticket.domain.Organization;
import com.mostafa.eticket.domain.Role;
import com.mostafa.eticket.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-not-for-production";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600);
    }

    private User user(Long organizationId, Role role) {
        User user = new User();
        user.setUsername("alice");
        user.setRole(role);
        if (organizationId != null) {
            Organization org = new Organization();
            org.setId(organizationId);
            user.setOrganization(org);
        }
        return user;
    }

    @Test
    void roundTripReturnsSameIdentity() {
        String token = jwtService.generateToken(user(7L, Role.AGENT));

        TokenClaims parsed = jwtService.parseToken(token);

        assertThat(parsed.username()).isEqualTo("alice");
        assertThat(parsed.role()).isEqualTo(Role.AGENT);
        assertThat(parsed.organizationId()).isEqualTo(7L);
    }

    @Test
    void adminTokenCarriesNoOrganizationIdClaim() {
        String token = jwtService.generateToken(user(null, Role.ADMIN));

        assertThat(jwtService.parseToken(token).organizationId()).isNull();

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims raw = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertThat(raw.containsKey("organizationId")).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user(7L, Role.AGENT));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expired = new JwtService(SECRET, -1);
        String token = expired.generateToken(user(7L, Role.AGENT));

        assertThatThrownBy(() -> expired.parseToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shortSecretFailsAtConstruction() {
        assertThatThrownBy(() -> new JwtService("way-too-short", 3600))
                .isInstanceOf(WeakKeyException.class);
    }
}
