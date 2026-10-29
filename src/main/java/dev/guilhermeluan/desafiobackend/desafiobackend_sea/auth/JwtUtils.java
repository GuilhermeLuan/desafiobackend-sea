package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    @Value("${jwt.issuer}")
    private String jwtIssuer;

    private SecretKey key;
    private JwtParser parser;

    @PostConstruct
    public void init(){
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(key).requireIssuer(jwtIssuer).build();
    }

    public String generateToken(String username, Role role){
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be null or blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return Jwts.builder()
                .issuer(jwtIssuer)
                .subject(username)
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(jwtExpiration)))
                .signWith(key)
                .compact();
    }

    public String getUserFromToken(String token){
        return getUserFromClaims(getClaims(token));
    }

    public Role getRoleFromToken(String token){
        return getRoleFromClaims(getClaims(token));
    }

    public String getUserFromClaims(Claims claims){
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("JWT token has no subject");
        }
        return subject;
    }

    public Role getRoleFromClaims(Claims claims){
        String role = claims.get("role", String.class);
        if (role == null) {
            throw new JwtException("JWT token has no role claim");
        }
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new JwtException("JWT token has an unknown role: " + role, e);
        }
    }

    public boolean validateJwtToken(String token){
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT token validation error: {}", e.getMessage());
        }

        return false;
    }

    public Claims getClaims(String token){
        try {
            return parser.parseSignedClaims(token).getPayload();
        } catch (IllegalArgumentException e) {
            throw new JwtException("JWT token is null or empty", e);
        }
    }
}
