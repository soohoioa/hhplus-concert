package kr.hhplus.be.server.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.hhplus.be.server.common.error.ApiErrorResponse;
import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import kr.hhplus.be.server.queue.application.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class QueueTokenFilter extends OncePerRequestFilter {

    private final QueueService queueService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 대기열 API는 토큰 검사 제외
        if (uri.startsWith("/api/v1/queue")) return true;

        // 필요 시 swagger / actuator 제외
        if (uri.startsWith("/swagger") || uri.startsWith("/v3/api-docs") || uri.startsWith("/actuator")) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = request.getHeader("X-QUEUE-TOKEN");
            if (token == null || token.isBlank()) {
                throw new AppException(ErrorCode.QUEUE_TOKEN_MISSING);
            }

            // 대기열 통과 여부 검증 (내 차례인지)
            queueService.validateReady(token);

            filterChain.doFilter(request, response);

        } catch (AppException e) {
            writeErrorResponse(response, request, e.getErrorCode());
        } catch (Exception e) {
            writeErrorResponse(response, request, ErrorCode.INTERNAL_ERROR);
        }
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            ErrorCode errorCode
    ) throws IOException {

        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse body = ApiErrorResponse.of(
                errorCode,
                errorCode.message(),
                errorCode.status().value(),
                request.getRequestURI()
        );

        response.getWriter().write(
                objectMapper.writeValueAsString(body)
        );
    }
}
