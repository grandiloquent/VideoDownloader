package euphoria.psycho.tasks;

import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;
import euphoria.psycho.tasks.RequestQueue.RequestEvent;
import euphoria.psycho.tasks.RequestQueue.RequestEventListener;


public class VideoService extends Service implements RequestEventListener {

    public static final String DOWNLOAD_CHANNEL = "DOWNLOAD";
    public static final String KEY_VIDEO_LIST = "video_list";
    private RequestQueue mQueue;
    private NotificationManager mNotificationManager;
    private File mDirectory;


    private VideoTask createTask(String uri, String fileName, String content) {
        VideoTask videoTask = new VideoTask();
        videoTask.Uri = uri;
        videoTask.FileName = fileName;
        videoTask.Directory = new File(mDirectory, fileName).getAbsolutePath();
        videoTask.Content = content;
        long result = VideoManager
                .getInstance()
                .getDatabase()
                .insertVideoTask(videoTask);
        if (result == -1) {
            VideoManager.post(() -> Toast.makeText(VideoService.this, getString(R.string.insert_task_failed), Toast.LENGTH_LONG).show());
            return null;
        }
        videoTask.Id = result;
        return videoTask;
    }

    private void submitRequest(String uri) {
        new Thread(() -> {
            // Calculate the hash value of the m3u8 content
            // as the file name and unique Id,
            // try to avoid downloading the video repeatedly
            String[] infos = VideoHelper.getInfos(uri);
            if (infos == null) {
                toastTaskFailed();
                return;
            }
            if (VideoHelper.checkTask(this, mQueue, infos[1])) {
                return;
            }
            // Query task from the database
            VideoTask videoTask = VideoManager.getInstance().getDatabase().getVideoTask(infos[1]);
            if (videoTask == null) {
                videoTask = createTask(uri, infos[1], infos[0]);
                if (videoTask == null) {
                    toastTaskFailed();
                    return;
                }
            } else {
                if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
                    toastTaskFinished();
                    return;
                }
            }
            submitTask(videoTask);

        }).start();
    }

    private void submitTask(VideoTask videoTask) {
        VideoManager.post(() -> {
            Request request = new Request(VideoService.this, videoTask, VideoManager.getInstance(), VideoManager.getInstance().getHandler());
            request.setRequestQueue(mQueue);
            mQueue.add(request);
        });
    }

    private void toastTaskFailed() {
        VideoManager.post(() -> {
            Toast.makeText(VideoService.this, "视频下载失败", Toast.LENGTH_LONG).show();
            tryStop();
        });
    }

    private void toastTaskFinished() {
        VideoManager.post(() -> {
            Toast.makeText(VideoService.this, "视频已下载", Toast.LENGTH_LONG).show();
            tryStop();
        });
    }

    private void tryStop() {
        if (mQueue != null && mQueue.getCurrentRequests().size() == 0) {
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            stopSelf();
            // Send a task finished broadcast
            // to the activity for display the download progress
            // if it is already open, then it should be closed now
            sendBroadcast(new Intent(VideoActivity.ACTION_FINISH));
            // Try to open the video list
            // because the new version of the Android system
            // may restrict the app to open activity from the service
            VideoHelper.startVideoListActivity(this);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDirectory = VideoHelper.setVideoDownloadDirectory(this);
        mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            VideoHelper.createNotificationChannel(this, mNotificationManager);
        }
        mQueue = VideoManager.newInstance(this).getQueue();
        mQueue.addRequestEventListener(this);
        startForeground(android.R.drawable.stat_sys_download, VideoHelper.getBuilder(this)
                .setContentText(getString(R.string.download_ready))
                .build());

    }


    @Override
    public void onDestroy() {
        mQueue.removeRequestEventListener(this);
        super.onDestroy();
    }

    @Override
    public void onRequestEvent(Request Request, int event) {
        if (
                event == RequestEvent.REQUEST_FINISHED
                        || event == RequestEvent.REQUEST_QUEUED) {
            Builder builder = VideoHelper.getBuilder(this);
            int[] counts = mQueue.count();
            builder.setContentText(String.format("正在下载 %s/%s 个视频",
                    counts[1],
                    counts[0]));
            mNotificationManager.notify(android.R.drawable.stat_sys_download, builder.build());
        }
        if (event == RequestEvent.REQUEST_FINISHED) {
            if (VideoHelper.checkIfExistsRunningTask(mQueue)) {
                return;
            }
            tryStop();
            toastTaskFinished();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        String[] videoList = intent.getStringArrayExtra(KEY_VIDEO_LIST);
        if (videoList != null) {
            for (String s : videoList) {
                submitRequest(s);
            }
            return START_NOT_STICKY;
        }
        // Get the video download address from the intent
        Uri uri = intent.getData();
        if (uri == null) {
            return START_NOT_STICKY;
        }
        submitRequest(uri.toString());
        return super.onStartCommand(intent, flags, startId);
    }
}
