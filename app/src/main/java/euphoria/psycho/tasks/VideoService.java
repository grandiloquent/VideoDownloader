package euphoria.psycho.tasks;

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        VideoManager.newInstance(this);
        mQueue = new RequestQueue();
        VideoManager.getInstance().setQueue(mQueue);
        mQueue.start();
    }

    @Override
    public void onDestroy() {
        if (mQueue != null) {
            mQueue.stop();
            mQueue = null;
        }
        super.onDestroy();

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
        } else {
            if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
                Toast.makeText(this, "视频已下载", Toast.LENGTH_LONG).show();
                return START_NOT_STICKY;
            }
        }
        if (videoTask == null) {
            return START_NOT_STICKY;
        }
        VideoManager.getInstance().addTask(videoTask);
        Request request = new Request(this, videoTask, VideoManager.getInstance(), VideoManager.getInstance().getHandler());
        request.setRequestQueue(mQueue);
        mQueue.add(request);
        return super.onStartCommand(intent, flags, startId);
    }
}
