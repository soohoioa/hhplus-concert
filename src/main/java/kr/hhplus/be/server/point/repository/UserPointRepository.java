package kr.hhplus.be.server.point.repository;

import kr.hhplus.be.server.point.domain.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
}
