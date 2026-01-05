package kr.hhplus.be.server.common.error;

import java.time.LocalDateTime;

public record ApiErrorResponse(String code, String message, int status, String path, LocalDateTime timestamp) {

    public static ApiErrorResponse of(ErrorCode ec, String message, int status, String path) {
        return new ApiErrorResponse(ec.code(), message, status, path, LocalDateTime.now());
    }

}
