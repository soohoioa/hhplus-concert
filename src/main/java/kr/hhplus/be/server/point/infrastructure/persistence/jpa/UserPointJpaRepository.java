package kr.hhplus.be.server.point.infrastructure.persistence.jpa;

import kr.hhplus.be.server.point.domain.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPointJpaRepository extends JpaRepository<UserPoint, Long> {
}
