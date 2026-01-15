package kr.hhplus.be.server.reservation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservation_token",
        indexes = {
                @Index(name = "idx_reservation_token_value", columnList = "token", unique = true),
                @Index(name = "idx_reservation_token_status", columnList = "status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 대기 순번으로 사용(대략)

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime activatedAt;
    private LocalDateTime consumedAt;

    public static ReservationToken issue(Long userId, LocalDateTime now, LocalDateTime expiresAt) {
        ReservationToken t = new ReservationToken();
        t.userId = userId;
        t.token = UUID.randomUUID().toString();
        t.status = TokenStatus.WAITING;
        t.issuedAt = now;
        t.expiresAt = expiresAt;
        return t;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void activate(LocalDateTime now) {
        this.status = TokenStatus.ACTIVE;
        this.activatedAt = now;
    }

    public void consume(LocalDateTime now) {
        this.status = TokenStatus.CONSUMED;
        this.consumedAt = now;
    }

    public void expire(LocalDateTime now) {
        this.status = TokenStatus.EXPIRED;
        // 필요하면 expiredAt 필드 추가 가능
    }
}
