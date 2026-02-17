package kr.hhplus.be.server.payment.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payment_outbox", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_outbox_event_id", columnNames = "eventId")
})
public class PaymentOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 중복 방지를 위한 이벤트 식별자 */
    @Column(nullable = false, length = 36)
    private String eventId;

    /** 이벤트 타입 */
    @Column(nullable = false, length = 100)
    private String eventType;

    /** payload(JSON 문자열) */
    @Lob
    @Column(nullable = false)
    private String payload;

    /** 상태 (PENDING 고정으로만 시작) */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static PaymentOutbox pending(String eventType, String payload) {
        PaymentOutbox outbox = new PaymentOutbox();
        outbox.eventId = UUID.randomUUID().toString();
        outbox.eventType = eventType;
        outbox.payload = payload;
        outbox.status = "PENDING";
        outbox.createdAt = LocalDateTime.now();
        return outbox;
    }
}
