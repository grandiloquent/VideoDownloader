package euphoria.psycho.downloader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.explorer.App;

public class DownloadManager implements VideoTaskListener {

    private static DownloadManager sVideoManager;
    private final DownloadTaskDatabase mDatabase;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private RequestQueue mQueue;
    private final List<VideoTaskListener> mVideoTaskListeners = new ArrayList<>();

    public DownloadManager(Context context) {
        mDatabase = DownloadTaskDatabase.getInstance(context);
    }


    public void addVideoTaskListener(VideoTaskListener taskListener) {
        mVideoTaskListeners.add(taskListener);
    }

    public DownloadTaskDatabase getDatabase() {
        return mDatabase;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public static DownloadManager getInstance() {
        if (sVideoManager == null) {
            sVideoManager = new DownloadManager(App.getContext());
        }
        return sVideoManager;
    }

    public RequestQueue getQueue() {
        if (mQueue == null) {
            mQueue = new RequestQueue();
            mQueue.start();
        }
        return mQueue;
    }


    public static DownloadManager newInstance(Context context) {
        if (sVideoManager == null) {
            sVideoManager = new DownloadManager(context);
        }
        return sVideoManager;
    }


    public void removeVideoTaskListener(VideoTaskListener videoTaskListener) {
        mVideoTaskListeners.remove(videoTaskListener);
    }

    @Override
    public void synchronizeTask(DownloaderTask videoTask) {
        mDatabase.updateDownloadTask(videoTask);
        mVideoTaskListeners.forEach(videoTaskListener -> videoTaskListener.synchronizeTask(videoTask));
        if (videoTask.Status < 0 || videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
            videoTask.Request.finish();
        }
    }

    @Override
    public void taskProgress(DownloaderTask videoTask) {
        mVideoTaskListeners.forEach(videoTaskListener -> videoTaskListener.taskProgress(videoTask));
    }

    public static void post(Runnable runnable) {
        getInstance().getHandler().post(runnable);
    }


}
