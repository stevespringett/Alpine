package alpine.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Utility methods for working with {@link ExecutorService}s.
 *
 * @since 2.3.0
 */
public final class ExecutorUtil {

    public record ExecutorStats(boolean terminated, Integer queueSize, Integer activeThreads) {
    }

    private ExecutorUtil() {
    }

    /**
     * Gathers {@link ExecutorStats} about a given {@link ExecutorService}.
     *
     * @param executor The {@link ExecutorService} to collect stats for
     * @return The collected {@link ExecutorStats}
     */
    public static ExecutorStats getExecutorStats(final ExecutorService executor) {
        if (executor instanceof final ThreadPoolExecutor tpExecutor) {
            return new ExecutorStats(tpExecutor.isTerminated(), tpExecutor.getQueue().size(), tpExecutor.getActiveCount());
        } else if (executor instanceof final ForkJoinPool fjpExecutor) {
            return new ExecutorStats(fjpExecutor.isTerminated(), fjpExecutor.getQueuedSubmissionCount(), fjpExecutor.getActiveThreadCount());
        }

        return new ExecutorStats(executor.isTerminated(), null, null);
    }

}
