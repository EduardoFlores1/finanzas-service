package pe.org.floreseh.finanzas.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import pe.org.floreseh.finanzas.core.config.JwtProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey signInKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signInKey = Keys.hmacShaKeyFor(properties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(SecurityUser securityUser) {
        Instant now = Instant.now();
        Instant expiration = now.plus(properties.expiration());

        var authorities = securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .subject(securityUser.getUsername())
                .claims(Map.of(
                        "uid", securityUser.getId().toString(),
                        "authorities", authorities
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signInKey)
                .compact();
    }

    public Optional<String> extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token)
                .filter(username -> username.equals(userDetails.getUsername()))
                .filter(_ -> !isTokenExpired(token))
                .isPresent();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration)
                .map(expiration -> expiration.before(Date.from(Instant.now())))
                .orElse(true);
    }

    public <T> Optional<T> extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signInKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claimsResolver.apply(claims));
        } catch (JwtException | IllegalArgumentException _) {
            return Optional.empty();
        }
    }
}
