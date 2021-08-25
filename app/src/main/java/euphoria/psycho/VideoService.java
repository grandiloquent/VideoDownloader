package euphoria.psycho;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;

public class VideoService extends Service {

    private RequestQueue mQueue;


    private VideoTask createTask(String uri) {
        VideoTask videoTask = new VideoTask();
        videoTask.Uri = uri;
        long result = VideoManager
                .getInstance()
                .getDatabase()
                .insertVideoTask(videoTask);
        if (result == -1) {
            Toast.makeText(this, getString(R.string.insert_task_failed), Toast.LENGTH_LONG).show();
            return null;
        }
        videoTask.Id = result;
        return videoTask;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        Uri uri = intent.getData();
        if (uri == null) {
            return START_NOT_STICKY;
        }
        VideoTask videoTask = VideoManager.getInstance().getDatabase().getVideoTask(uri.toString());
        if (videoTask == null) {
            videoTask = createTask(uri.toString());
        }
        if (videoTask == null) {
            return START_NOT_STICKY;
        }
        VideoManager.getInstance().addTask(videoTask);
        mQueue.add(new Request(this, videoTask, VideoManager.getInstance(), VideoManager.getInstance().getHandler()));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        VideoManager.newInstance(this);
        mQueue = new RequestQueue();
        mQueue.start();


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mQueue != null) {
            mQueue.stop();
            mQueue = null;
        }
        super.onDestroy();

    }
}
