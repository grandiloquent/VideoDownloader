package euphoria.psycho.downloader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
public class RequestQueue {

    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 4;


    private final Set<DownloaderRequest> mCurrentRequests = new HashSet<>();
    private final NetworkDispatcher[] mDispatchers;
    private final List<RequestEventListener> mEventListeners = new ArrayList<>();
    private final PriorityBlockingQueue<DownloaderRequest> mNetworkQueue = new PriorityBlockingQueue<>();
    private final AtomicInteger mSequenceGenerator = new AtomicInteger();

    public RequestQueue() {
        mDispatchers = new NetworkDispatcher[DEFAULT_NETWORK_THREAD_POOL_SIZE];
    }

    public void add(DownloaderRequest request) {
        // Tag the request as belonging to this queue and add it to the set of current requests.
        request.setRequestQueue(this);
        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }
        request.setSequence(getSequenceNumber());
        // Process requests in the order they are added.
        sendRequestEvent(request, RequestEvent.REQUEST_QUEUED);
        mNetworkQueue.add(request);
    }

    public void addRequestEventListener(RequestEventListener listener) {
        //Logger.e(String.format("addRequestEventListener, %s", listener.getClass().getSimpleName()));
        synchronized (mEventListeners) {
            mEventListeners.add(listener);
        }
    }

    public Set<DownloaderRequest> getCurrentRequests() {
        return mCurrentRequests;
    }

    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }

    public void removeRequestEventListener(RequestEventListener listener) {
        synchronized (mEventListeners) {
            mEventListeners.remove(listener);
        }
    }

    public void start() {
        stop(); // Make sure any currently running dispatchers are stopped.
        // Create network dispatchers (and corresponding threads) up to the pool size.
        for (int i = 0; i < mDispatchers.length; i++) {
            NetworkDispatcher networkDispatcher =
                    new NetworkDispatcher(mNetworkQueue);
            mDispatchers[i] = networkDispatcher;
            networkDispatcher.start();
        }
    }

    public void stop() {
        for (final NetworkDispatcher mDispatcher : mDispatchers) {
            if (mDispatcher != null) {
                mDispatcher.quit();
            }
        }
    }

    public interface RequestEventListener {
        void onRequestEvent(DownloaderRequest Request, int event);
    }

    public @interface RequestEvent {
        /**
         * All the work associated with the request is finished and request is removed from all the
         * queues.
         */
        public static final int REQUEST_FINISHED = 5;
        /**
         * The network dispatch finished for the request and response (if any) is delivered.
         */
        public static final int REQUEST_NETWORK_DISPATCH_FINISHED = 4;
        /**
         * Network dispatch started for the request.
         */
        public static final int REQUEST_NETWORK_DISPATCH_STARTED = 3;
        /**
         * The request was added to the queue.
         */
        public static final int REQUEST_QUEUED = 0;
    }

    void sendRequestEvent(DownloaderRequest request, @RequestEvent int event) {
        synchronized (mEventListeners) {
            for (RequestEventListener listener : mEventListeners) {
                listener.onRequestEvent(request, event);
            }
        }
    }

    int[] count() {
        int running = 0;
        int total = 0;
        synchronized (mCurrentRequests) {
            total = mCurrentRequests.size();
            for (DownloaderRequest request : mCurrentRequests) {
                if (request.getDownloaderTask().Status != 7 && request.getDownloaderTask().Status > -1) {
                    running++;
                }
            }
        }
        return new int[]{total, running};
    }

    void removeVideoTask(DownloaderTask videoTask) {
        synchronized (mCurrentRequests) {
          DownloaderRequest src = null;
            for (DownloaderRequest request : mCurrentRequests) {
                if (request.getDownloaderTask().FileName.equals(videoTask.FileName)) {
                    src = request;
                    break;
                }
            }
            if (src != null) {
                mCurrentRequests.remove(src);
            }
        }
    }

    List<DownloaderTask> getVideoTasks() {
        List<DownloaderTask> videoTasks = new ArrayList<>();
        synchronized (mCurrentRequests) {
            for (DownloaderRequest request : mCurrentRequests) {
                videoTasks.add(request.getDownloaderTask());
            }
        }
        return videoTasks;
    }
    boolean taskExists(String fileName) {
        synchronized (mCurrentRequests) {
            for (DownloaderRequest request : mCurrentRequests) {
                if (request.getDownloaderTask().FileName.equals(fileName)) return true;
            }
            return false;
        }
    }

    void finish(DownloaderRequest request) {
        // Remove from the set of requests currently being processed.
//        synchronized (mCurrentRequests) {
//            mCurrentRequests.remove(request);
//        }
        sendRequestEvent(request, RequestEvent.REQUEST_FINISHED);
    }
}
