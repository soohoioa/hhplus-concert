package kr.hhplus.be.server.concert;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.concert.application.dto.GetAvailableSeatsQuery;
import kr.hhplus.be.server.concert.application.service.ConcertQueryUseCase;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.port.out.FindAvailableSeatsPort;
import kr.hhplus.be.server.reservation.application.dto.HoldSeatCommand;
import kr.hhplus.be.server.reservation.application.service.HoldSeatUseCase;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.support.RedisContainerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class AvailableSeatsCacheIT extends RedisContainerTestBase {

    @Autowired
    ConcertQueryUseCase concertQueryUseCase;
    @Autowired
    HoldSeatUseCase holdSeatUseCase;
    @Autowired
    CacheManager cacheManager;
    @Autowired
    EntityManager entityManager;
    @Autowired
    PlatformTransactionManager txManager;

    @MockitoSpyBean
    FindAvailableSeatsPort findAvailableSeatsPort;

    Long concertId;
    Long scheduleId;

    @BeforeEach
    void setUp() {
        var cache = cacheManager.getCache("concert:available-seats");
        if (cache != null) cache.clear();

        new TransactionTemplate(txManager).execute(status -> {
            Concert concert = Concert.create("concert");
            entityManager.persist(concert);
            concertId = concert.getId();

            ConcertSchedule schedule = ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1));
            entityManager.persist(schedule);
            scheduleId = schedule.getId();

            entityManager.persist(ScheduleSeat.create(schedule, 1));
            entityManager.persist(ScheduleSeat.create(schedule, 2));
            entityManager.persist(ScheduleSeat.create(schedule, 3));

            entityManager.flush();
            entityManager.clear();
            return null;
        });
    }

    @Test
    void getAvailableSeats_캐시HIT_후_HOLD성공시_Evict로_캐시MISS재발생() {
        // 1) 같은 조회 2번 -> 2번째는 캐시 HIT
        concertQueryUseCase.getAvailableSeats(new GetAvailableSeatsQuery(concertId, scheduleId));
        concertQueryUseCase.getAvailableSeats(new GetAvailableSeatsQuery(concertId, scheduleId));

        verify(findAvailableSeatsPort, times(1))
                .findAvailableSeatNos(org.mockito.ArgumentMatchers.eq(scheduleId), org.mockito.ArgumentMatchers.any());

        // 2) HOLD 성공 -> @CacheEvict 로 available-seats 캐시 삭제
        holdSeatUseCase.hold(new HoldSeatCommand(1L, scheduleId, 1));

        // 3) 다시 조회 -> 캐시 MISS -> Port 호출 1번 추가됨 (총 2번)
        concertQueryUseCase.getAvailableSeats(new GetAvailableSeatsQuery(concertId, scheduleId));

        verify(findAvailableSeatsPort, times(2))
                .findAvailableSeatNos(org.mockito.ArgumentMatchers.eq(scheduleId), org.mockito.ArgumentMatchers.any());
    }
}
