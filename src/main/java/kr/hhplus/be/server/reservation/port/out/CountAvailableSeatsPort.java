package kr.hhplus.be.server.reservation.port.out;

public interface CountAvailableSeatsPort {
    long countAvailableSeats(Long scheduleId);
}
