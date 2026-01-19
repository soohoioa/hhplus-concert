package kr.hhplus.be.server.concert;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.concert.application.dto.GetSchedulesQuery;
import kr.hhplus.be.server.concert.application.service.ConcertQueryUseCase;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.port.out.FindSchedulesPort;
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
public class ConcertSchedulesCacheIT extends RedisContainerTestBase {

    @Autowired
    ConcertQueryUseCase concertQueryUseCase;
    @Autowired
    CacheManager cacheManager;
    @Autowired
    EntityManager entityManager;
    @Autowired
    PlatformTransactionManager txManager;

    @MockitoSpyBean
    FindSchedulesPort findSchedulesPort;

    Long concertId;

    @BeforeEach
    void setUp() {
        // 캐시 초기화 (테스트 간 간섭 방지)
        var cache = cacheManager.getCache("concert:schedules");
        if (cache != null) cache.clear();

        concertId = new TransactionTemplate(txManager).execute(status -> {
            Concert concert = Concert.create("concert");
            entityManager.persist(concert);

            entityManager.persist(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(1)));
            entityManager.persist(ConcertSchedule.create(concert, LocalDateTime.now().plusDays(2)));

            entityManager.flush();
            entityManager.clear();
            return concert.getId();
        });
    }

    @Test
    void getSchedules_같은요청_2번이면_두번째는_캐시HIT() {
        // when: 같은 요청 2번
        concertQueryUseCase.getSchedules(new GetSchedulesQuery(concertId));
        concertQueryUseCase.getSchedules(new GetSchedulesQuery(concertId));

        // then: Port 호출은 1번만 (2번째는 캐시에서 반환)
        verify(findSchedulesPort, times(1))
                .findByConcertIdOrderByStartAtAsc(concertId);
    }

}
