package kr.hhplus.be.server.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long scheduleId;

    @Column(nullable = false)
    private Integer seatNo;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private LocalDateTime paidAt;

    public static Payment of(Long userId, Long scheduleId, Integer seatNo, Long amount, LocalDateTime paidAt) {
        Payment p = new Payment();
        p.userId = userId;
        p.scheduleId = scheduleId;
        p.seatNo = seatNo;
        p.amount = amount;
        p.paidAt = paidAt;
        return p;
    }

}
