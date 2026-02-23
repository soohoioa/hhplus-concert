package kr.hhplus.be.server.reservation.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_outbox", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reservation_outbox_event_id", columnNames = "event_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationOutbox {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false, length = 20)
    private String status; // PENDING/SENDING/SENT/FAILED (Payment과 동일하게 String로)

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey; // scheduleId

    @Lob
    @Column(nullable = false)
    private String payload; // json

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ReservationOutbox pending(String eventId, String messageKey, String payload, LocalDateTime now) {
        ReservationOutbox o = new ReservationOutbox();
        o.eventId = eventId;
        o.status = "PENDING";
        o.retryCount = 0;
        o.messageKey = messageKey;
        o.payload = payload;
        o.createdAt = now;
        o.updatedAt = now;
        return o;
    }
}
