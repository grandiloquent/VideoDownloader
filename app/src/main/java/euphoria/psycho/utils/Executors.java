package euphoria.psycho.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@AnyThread
public final class Executors {
    private Executors() { }

    private static final Executor DIRECT_EXECUTOR = Runnable::run;
    private static final Executor MAIN_THREAD_EXECUTOR = new Executor() {
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            if (mHandler.getLooper() != Looper.myLooper()) {
                mHandler.post(command);
            } else {
                command.run();
            }
        }
    };
    private static final Executor UI_THREAD_EXECUTOR = MAIN_THREAD_EXECUTOR;

    public static @NonNull Executor directExecutor() {
        return DIRECT_EXECUTOR;
    }

    public static @NonNull Executor mainThreadExecutor() {
        return MAIN_THREAD_EXECUTOR;
    }

    public static @NonNull Executor uiThreadExecutor() {
        return UI_THREAD_EXECUTOR;
    }

    public static @NonNull ExecutorService newFixedUniqueThreadPool(int nThreads) {
        return new UniqueExecutor(nThreads, nThreads, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    public static @NonNull ExecutorService newFixedUniqueThreadPool(int nThreads,
            @NonNull ThreadFactory threadFactory) {
        return new UniqueExecutor(nThreads, nThreads, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), threadFactory);
    }

    public static @NonNull ExecutorService newCachedUniqueThreadPool() {
        return new UniqueExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }

    public static @NonNull ExecutorService newCachedUniqueThreadPool(
            @NonNull ThreadFactory threadFactory) {
        return new UniqueExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), threadFactory);
    }
}
