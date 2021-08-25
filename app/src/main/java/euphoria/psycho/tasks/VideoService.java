package euphoria.psycho.tasks;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.utils.M3u8Utils;

public class VideoService extends Service {

    private RequestQueue mQueue;

    private VideoTask createTask(String uri, String fileName, String content) {
        VideoTask videoTask = new VideoTask();
        videoTask.Uri = uri;
        videoTask.FileName = fileName;
        videoTask.Content = content;
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

    private void submitTask(VideoTask videoTask) {
        VideoManager.getInstance().getHandler()
                .post(() -> {
                    VideoManager.getInstance().addTask(videoTask);
                    Request request = new Request(VideoService.this, videoTask, VideoManager.getInstance(), VideoManager.getInstance().getHandler());
                    request.setRequestQueue(mQueue);
                    mQueue.add(request);
                });
    }

    private void toastTaskFailed() {
        VideoManager.getInstance().getHandler()
                .post(() -> {
                    Toast.makeText(VideoService.this, "视频已下载", Toast.LENGTH_LONG).show();
                });
    }

    private void toastTaskFinished() {
        VideoManager.getInstance().getHandler()
                .post(() -> {
                    Toast.makeText(VideoService.this, "视频已下载", Toast.LENGTH_LONG).show();
                });
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
        new Thread(() -> {
            String m3u8String = null;
            String fileName = null;
            try {
                m3u8String = M3u8Utils.getString(uri.toString());
                if (m3u8String == null) {
                    toastTaskFailed();
                    return;
                }
                fileName = KeyShare.toHex(KeyShare.md5encode(m3u8String));
            } catch (Exception ignored) {
                toastTaskFailed();
                return;
            }
            if (fileName == null) {
                toastTaskFailed();
                return;
            }
            VideoTask videoTask = VideoManager.getInstance().getDatabase().getVideoTask(fileName);
            if (videoTask == null) {
                videoTask = createTask(uri.toString(), fileName, m3u8String);
            } else {
                if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
                    toastTaskFinished();
                    return;
                }
            }
            if (videoTask == null) {
                return;
            }
            submitTask(videoTask);

        }).start();
        return super.onStartCommand(intent, flags, startId);
    }
}
