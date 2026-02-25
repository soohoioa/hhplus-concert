package kr.hhplus.be.server.reservation.infrastructure.persistence.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ReservationOutbox;
import kr.hhplus.be.server.reservation.infrastructure.persistence.jpa.ReservationOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReservationOutboxAppender {

    private final ReservationOutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void appendHoldCreated(HoldSeatCommand cmd, LocalDateTime expiresAt, LocalDateTime now) {
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> event = Map.of(
                "eventId", eventId,
                "eventType", "ReservationHoldCreatedV1",
                "occurredAt", now.toString(),
                "userId", cmd.getUserId(),
                "scheduleId", cmd.getScheduleId(),
                "seatNo", cmd.getSeatNo(),
                "holdExpiresAt", expiresAt.toString()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            ReservationOutbox outbox = ReservationOutbox.pending(
                    eventId,
                    String.valueOf(cmd.getScheduleId()), // messageKey (파티션 키)
                    payload,
                    now
            );
            outboxRepository.save(outbox);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize ReservationHoldCreated event", e);
        }
    }
}