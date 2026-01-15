package kr.hhplus.be.server.concert.port.out;

import kr.hhplus.be.server.concert.domain.Concert;

import java.util.Optional;

public interface LoadConcertPort {
    Optional<Concert> findById(Long concertId);
}
