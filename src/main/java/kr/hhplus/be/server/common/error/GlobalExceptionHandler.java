package kr.hhplus.be.server.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleApp(AppException e, HttpServletRequest req) {
        ErrorCode ec = e.getErrorCode();
        return ResponseEntity.status(ec.status())
                .body(ApiErrorResponse.of(ec, e.getMessage(), ec.status().value(), req.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(ec.status())
                .body(ApiErrorResponse.of(ec, e.getMessage(), ec.status().value(), req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOthers(Exception e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(ec.status())
                .body(ApiErrorResponse.of(ec, ec.message(), ec.status().value(), req.getRequestURI()));
    }

}
