package kr.hhplus.be.server.concert.controller;

import kr.hhplus.be.server.queue.infrastructure.QueueRedisRepository;
import kr.hhplus.be.server.queue.support.QueueKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rank")
public class RankingController {

    private final QueueRedisRepository queueRedisRepository;

    @GetMapping("/soldout/top")
    public SoldoutTopResponse topSoldout(@RequestParam(defaultValue = "10") int limit) {
        Set<String> top = queueRedisRepository.getTopSoldout(QueueKeys.soldoutRankKey(), limit);
        return new SoldoutTopResponse(List.copyOf(top));
    }

    public record SoldoutTopResponse(List<String> scheduleIds) {}
}
