package kr.hhplus.be.server.payment.service;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.dto.PaymentRequest;
import kr.hhplus.be.server.payment.dto.PaymentResponse;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import kr.hhplus.be.server.reservation.domain.ScheduleSeat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final ScheduleSeatRepository seatRepository;
    private final UserPointRepository userPointRepository;
    private final PaymentRepository paymentRepository;

    public PaymentResponse pay(PaymentRequest paymentRequest) {
        validateSeatNo(paymentRequest.getSeatNo());

        LocalDateTime now = LocalDateTime.now();

        // 1) 좌석 row 락 (결제 시점에도 최종 검증 필요)
        ScheduleSeat seat = seatRepository.findByScheduleIdAndSeatNoForUpdate(paymentRequest.getScheduleId(), paymentRequest.getSeatNo())
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));

        // 2) RESERVED면 즉시 실패
        if (seat.getStatus() == SeatStatus.RESERVED) {
            throw new AppException(ErrorCode.SEAT_ALREADY_RESERVED);
        }

        // 3) 만료면 결제 실패 (정리 작업은 스케줄러가 담당)
        if (seat.isHoldExpired(now) || seat.getHoldExpiresAt() == null || seat.getHoldExpiresAt().isBefore(now)) {
            throw new AppException(ErrorCode.HOLD_EXPIRED);
        }

        // 4) HELD 아니면 실패
        if (seat.getStatus() != SeatStatus.HELD) {
            throw new AppException(ErrorCode.SEAT_NOT_HELD);
        }

        // 5) 홀드 사용자 불일치
        if (!Objects.equals(seat.getHoldUserId(), paymentRequest.getUserId())) {
            throw new AppException(ErrorCode.SEAT_HELD_BY_OTHER);
        }

        // 6) 포인트 차감 (@Version 낙관 락)
        UserPoint point = userPointRepository.findById(paymentRequest.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_POINT_NOT_FOUND));
        point.spend(paymentRequest.getAmount());

        // 7) 좌석 RESERVED 확정
        seat.reserve(paymentRequest.getUserId(), now);

        // 8) 결제내역 생성
        Payment payment = paymentRepository.save(
                Payment.of(paymentRequest.getUserId(), paymentRequest.getScheduleId(), paymentRequest.getSeatNo(), paymentRequest.getAmount(), now)
        );

        // 9) (추후) 대기열 토큰 만료 훅
        // queueTokenService.expire(req.userId());

        return new PaymentResponse(payment.getId(), paymentRequest.getUserId(), paymentRequest.getScheduleId(), paymentRequest.getSeatNo(), paymentRequest.getAmount(), now);
    }

    private void validateSeatNo(Integer seatNo) {
        if (seatNo == null || seatNo < 1 || seatNo > 50) {
            throw new AppException(ErrorCode.SEAT_NO_OUT_OF_RANGE);
        }
    }

}
