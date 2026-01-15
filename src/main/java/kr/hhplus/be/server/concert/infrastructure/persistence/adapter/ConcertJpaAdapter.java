package kr.hhplus.be.server.concert.infrastructure.persistence.adapter;

import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.infrastructure.persistence.jpa.ConcertJpaRepository;
import kr.hhplus.be.server.concert.port.out.LoadConcertPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertJpaAdapter implements LoadConcertPort {

    private final ConcertJpaRepository concertJpaRepository;

    @Override
    public Optional<Concert> findById(Long concertId) {
        return concertJpaRepository.findById(concertId);
    }
}
