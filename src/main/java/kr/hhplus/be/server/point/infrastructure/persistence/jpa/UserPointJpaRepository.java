package kr.hhplus.be.server.point.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.point.domain.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPointJpaRepository extends JpaRepository<UserPoint, Long> {
    // (선택) 비관적 락 방식으로도 확장 가능
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPoint p where p.userId = :userId")
    Optional<UserPoint> findByUserIdForUpdate(@Param("userId") Long userId);
}
