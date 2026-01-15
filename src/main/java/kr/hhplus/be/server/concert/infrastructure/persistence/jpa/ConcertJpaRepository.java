package kr.hhplus.be.server.concert.infrastructure.persistence.jpa;

import kr.hhplus.be.server.concert.domain.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcertJpaRepository extends JpaRepository<Concert, Long> {
}
