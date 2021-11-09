package euphoria.psycho.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HLSDownloadManager {

    private final ExecutorService mExecutor;
    private static HLSDownloadManager sManager;
    private List<HLSDownloadRequest> mRequests = new ArrayList<>();
    private List<HLSDownloadRequestListener> mRequestListeners = new ArrayList<>();

    public HLSDownloadManager() {
        mExecutor = Executors.newFixedThreadPool(3);
    }

    public void submit(HLSDownloadRequest request) {
        synchronized (this) {
            if (mRequests.contains(request)) {
                return;
            }
            mRequests.add(request);
            mExecutor.submit(request);
            mRequestListeners.forEach(r -> r.onSubmit(request));
        }
    }

    public void finish(HLSDownloadRequest request) {
        synchronized (this) {
            if (mRequests.contains(request)) {
                mRequests.remove(request);
                mRequestListeners.forEach(r -> r.onFinish(request));
            }
        }
    }

    public static HLSDownloadManager getInstance() {
        if (sManager == null)
            sManager = new HLSDownloadManager();
        return sManager;
    }
}
