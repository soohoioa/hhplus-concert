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

        // 2) 만료된 hold면 해제
        if (seat.isHoldExpired(now)) {
            seat.releaseHold();
        }

        // 3) hold 유효성 체크
        if (seat.getStatus() != SeatStatus.HELD) {
            throw new AppException(ErrorCode.SEAT_NOT_HELD);
        }
        if (!Objects.equals(seat.getHoldUserId(), paymentRequest.getUserId())) {
            throw new AppException(ErrorCode.SEAT_HELD_BY_OTHER);
        }
        if (seat.getHoldExpiresAt() == null || seat.getHoldExpiresAt().isBefore(now)) {
            throw new AppException(ErrorCode.HOLD_EXPIRED);
        }

        // 4) 포인트 차감 (@Version 낙관 락)
        UserPoint point = userPointRepository.findById(paymentRequest.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_POINT_NOT_FOUND));
        point.spend(paymentRequest.getAmount());

        // 5) 좌석 RESERVED 확정
        seat.reserve(paymentRequest.getUserId(), now);

        // 6) 결제내역 생성
        Payment payment = paymentRepository.save(
                Payment.of(paymentRequest.getUserId(), paymentRequest.getScheduleId(), paymentRequest.getSeatNo(), paymentRequest.getAmount(), now)
        );

        // 7) (추후) 대기열 토큰 만료 훅
        // queueTokenService.expire(req.userId());

        return new PaymentResponse(payment.getId(), paymentRequest.getUserId(), paymentRequest.getScheduleId(), paymentRequest.getSeatNo(), paymentRequest.getAmount(), now);
    }

    private void validateSeatNo(Integer seatNo) {
        if (seatNo == null || seatNo < 1 || seatNo > 50) {
            throw new AppException(ErrorCode.SEAT_NO_OUT_OF_RANGE);
        }
    }

}
