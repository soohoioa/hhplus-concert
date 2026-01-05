package kr.hhplus.be.server.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-001", "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-404", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다."),

    // Concert / Schedule
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT-404", "공연 일정이 존재하지 않습니다."),
    CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT-405", "공연 정보를 찾을 수 없습니다."),

    // Seat
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "SEAT-404", "좌석 정보를 찾을 수 없습니다."),
    SEAT_NO_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "SEAT-400", "좌석 번호는 1번부터 50번까지만 가능합니다."),
    SEAT_ALREADY_RESERVED(HttpStatus.CONFLICT, "SEAT-409", "이미 예약된 좌석입니다."),
    SEAT_HELD_BY_OTHER(HttpStatus.CONFLICT, "SEAT-410", "다른 사용자가 선점한 좌석입니다."),
    SEAT_NOT_HELD(HttpStatus.CONFLICT, "SEAT-411", "선점되지 않은 좌석입니다."),
    HOLD_EXPIRED(HttpStatus.CONFLICT, "SEAT-412", "좌석 선점 시간이 만료되었습니다."),

    // Point
    USER_POINT_NOT_FOUND(HttpStatus.NOT_FOUND, "POINT-404", "포인트 정보가 존재하지 않습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "POINT-409", "포인트 잔액이 부족합니다."),

    // payment
    PAYMENT_USER_MISMATCH(HttpStatus.CONFLICT, "PAYMENT-409", "좌석을 선점한 사용자만 결제할 수 있습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String message() { return message; }

}
