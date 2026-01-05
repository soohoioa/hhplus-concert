package kr.hhplus.be.server.concert.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "concert_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertSchedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    public static ConcertSchedule create(Concert concert, LocalDateTime startAt) {
        ConcertSchedule schedule = new ConcertSchedule();
        schedule.concert = concert;
        schedule.startAt = startAt;
        return schedule;
    }

}
