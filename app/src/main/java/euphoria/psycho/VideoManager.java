package euphoria.psycho;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.explorer.App;
import euphoria.psycho.share.Logger;

public class VideoManager implements VideoTaskListener {

    private static WeakReference<VideoManager> sVideoManager;
    private final Context mContext;
    private final VideoTaskDatabase mDatabase;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private RequestQueue mQueue;
    private List<VideoTask> mVideoTasks = new ArrayList<>();
    private List<Listener> mListeners = new ArrayList<>();
    private List<VideoTaskListener> mVideoTaskListeners = new ArrayList<>();

    public VideoManager(Context context) {
        mContext = context;
        mDatabase = VideoTaskDatabase.getInstance(context);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void addTask(VideoTask videoTask) {
        mVideoTasks.add(videoTask);
        for (Listener listener : mListeners) {
            listener.addTask();
        }
    }

    public void addVideoTaskListener(VideoTaskListener taskListener) {
        mVideoTaskListeners.add(taskListener);
    }

    public VideoTaskDatabase getDatabase() {
        return mDatabase;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public static VideoManager getInstance() {
        if (sVideoManager.get() == null) {
            sVideoManager = new WeakReference<>(new VideoManager(App.getContext()));
        }
        return sVideoManager.get();
    }

    public RequestQueue getQueue() {
        return mQueue;
    }

    public void setQueue(RequestQueue queue) {
        mQueue = queue;
    }

    public List<VideoTask> getVideoTasks() {
        return mVideoTasks;
    }

    public static VideoManager newInstance(Context context) {
        if (sVideoManager == null || sVideoManager.get() == null) {
            sVideoManager = new WeakReference<>(new VideoManager(context));
        }
        return sVideoManager.get();
    }

    public void removeListener(Listener listener) {
        Logger.d(String.format("removeListener: %s", "listener"));
        mListeners.remove(listener);
    }

    public void removeTask(VideoTask videoTask) {
        mVideoTasks.remove(videoTask);
    }

    public void removeVideoTaskListener(VideoTaskListener videoTaskListener) {
        mVideoTaskListeners.remove(videoTaskListener);
    }

    @Override
    public void synchronizeTask(VideoTask videoTask) {
        Logger.d(String.format("synchronizeTask: %s, %s", videoTask.Id, videoTask.Status));
        mDatabase.updateVideoTask(videoTask);
    }

    @Override
    public void taskProgress(VideoTask videoTask) {
        for (VideoTaskListener listener : mVideoTaskListeners) {
            listener.taskProgress(videoTask);
        }
        Logger.d(String.format("taskProgress: %s, %s", videoTask.Id, videoTask.Status));
    }

    @Override
    public void taskStart(VideoTask videoTask) {
        Logger.d(String.format("taskStart: %s, %s", videoTask.Id, videoTask.Status));
    }

    public interface Listener {
        void addTask();
    }
}
