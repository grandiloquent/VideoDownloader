package euphoria.psycho;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

import euphoria.psycho.explorer.App;

public class VideoManager implements VideoTaskListener {

    private static WeakReference<VideoManager> sVideoManager;
    private final Context mContext;
    private final VideoTaskDatabase mDatabase;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public VideoManager(Context context) {
        mContext = context;
        mDatabase = VideoTaskDatabase.getInstance(context);
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

    public static VideoManager newInstance(Context context) {
        if (sVideoManager.get() == null) {
            sVideoManager = new WeakReference<>(new VideoManager(context));
        }
        return sVideoManager.get();
    }

    @Override
    public void synchronizeTask(VideoTask videoTask) {
        mDatabase.updateVideoTask(videoTask);
    }

    @Override
    public void taskStarted(VideoTask videoTask) {
    }
}
