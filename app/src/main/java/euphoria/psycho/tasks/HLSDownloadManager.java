package euphoria.psycho.tasks;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HLSDownloadManager implements HLSDownloadRequestListener {

    private static HLSDownloadManager sManager;
    private final ExecutorService mExecutor;
    private List<HLSDownloadRequest> mRequests = new ArrayList<>();
    private List<HLSDownloadListener> mListeners = new ArrayList<>();
    private HLSDownloadDatabase mDatabase;

    public HLSDownloadManager(Context context) {
        mExecutor = Executors.newFixedThreadPool(3);
        mDatabase = new HLSDownloadDatabase(context, new File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "task.db"
        ).getAbsolutePath());
    }

    public void finish(HLSDownloadRequest request) {
        synchronized (this) {
            if (mRequests.contains(request)) {
                mRequests.remove(request);
                mListeners.forEach(r -> r.onFinish(request));
            }
        }
    }

    public HLSDownloadDatabase getDatabase() {
        return mDatabase;
    }

    public static HLSDownloadManager getInstance(Context context) {
        if (sManager == null)
            sManager = new HLSDownloadManager(context);
        return sManager;
    }

    @Override
    public void onProgress(HLSDownloadRequest hlsDownloadRequest) {
    }

    public void submit(HLSDownloadTask task) {
        synchronized (this) {
            if (mRequests.stream().anyMatch(m -> m.getTask().getUniqueId().equals(task.getUniqueId()))) {
                return;
            }
            HLSDownloadRequest request = new HLSDownloadRequest(task, this);
            mRequests.add(request);
            mExecutor.submit(request);
            mListeners.forEach(r -> r.onSubmit(request));
        }
    }
}
