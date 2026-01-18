package kr.hhplus.be.server.support;

import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;

public abstract class ConcurrencyTestSupport {

    public record TaskResult(boolean success, ErrorCode errorCode, Throwable raw) {}

    protected TaskResult runCatching(Runnable task) {
        try {
            task.run();
            return new TaskResult(true, null, null);
        } catch (AppException ex) {
            return new TaskResult(false, ex.getErrorCode(), ex);
        } catch (Throwable t) {
            return new TaskResult(false, null, t);
        }
    }

}
