package euphoria.psycho.utils;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@AnyThread
public class UniqueExecutor extends ThreadPoolExecutor {
    private final Collection<Runnable> mRunning = Collections.synchronizedSet(new HashSet<>());

    public UniqueExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            @NonNull TimeUnit unit, @NonNull BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public UniqueExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            @NonNull TimeUnit unit, @NonNull BlockingQueue<Runnable> workQueue,
            @NonNull ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public UniqueExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            @NonNull TimeUnit unit, @NonNull BlockingQueue<Runnable> workQueue,
            @NonNull RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public UniqueExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            @NonNull TimeUnit unit, @NonNull BlockingQueue<Runnable> workQueue,
            @NonNull ThreadFactory threadFactory, @NonNull RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory,
                handler);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        if (!mRunning.add(command)) {
            return;
        }
        super.execute(command);
    }

    @Override
    protected void afterExecute(@NonNull Runnable r, @Nullable Throwable t) {
        super.afterExecute(r, t);
        mRunning.remove(r);
    }
}
