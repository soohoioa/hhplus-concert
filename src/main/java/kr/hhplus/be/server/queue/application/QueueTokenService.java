package kr.hhplus.be.server.queue.application;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class QueueTokenService {

    private final SecretKey secretKey;
    private final long ttlSeconds;

    public QueueTokenService(
            @Value("${queue.token.secret}") String secret,
            @Value("${queue.token.ttl-seconds:1800}") long ttlSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(String userUuid, String queueKey, long snapRank) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userUuid)
                .claim("qk", queueKey)
                .claim("snap_rank", snapRank)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public QueueTokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userUuid = claims.getSubject();
        String queueKey = claims.get("qk", String.class);

        return new QueueTokenClaims(userUuid, queueKey);
    }

    public record QueueTokenClaims(String userUuid, String queueKey) {}

}
