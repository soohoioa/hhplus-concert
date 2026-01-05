package kr.hhplus.be.server.reservation.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "schedule_seat",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_schedule_seat",
                columnNames = {"schedule_id", "seat_no"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleSeat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ConcertSchedule schedule;

    @Column(name = "seat_no", nullable = false)
    private Integer seatNo; // 1~50

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "hold_user_id")
    private Long holdUserId;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "reserved_user_id")
    private Long reservedUserId;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    public boolean isHoldExpired(LocalDateTime now) {
        return status == SeatStatus.HELD && holdExpiresAt != null && holdExpiresAt.isBefore(now);
    }

    public void hold(Long userId, LocalDateTime expiresAt) {
        this.status = SeatStatus.HELD;
        this.holdUserId = userId;
        this.holdExpiresAt = expiresAt;
        this.reservedUserId = null;
        this.reservedAt = null;
    }

    public void releaseHold() {
        this.status = SeatStatus.AVAILABLE;
        this.holdUserId = null;
        this.holdExpiresAt = null;
    }

    public void reserve(Long userId, LocalDateTime now) {
        this.status = SeatStatus.RESERVED;
        this.reservedUserId = userId;
        this.reservedAt = now;
        this.holdUserId = null;
        this.holdExpiresAt = null;
    }

    public static ScheduleSeat create(ConcertSchedule schedule, int seatNo) {
        ScheduleSeat seat = new ScheduleSeat();
        seat.schedule = schedule;
        seat.seatNo = seatNo;
        seat.status = SeatStatus.AVAILABLE;
        return seat;
    }

}
