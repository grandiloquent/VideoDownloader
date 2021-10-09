package euphoria.psycho.downloader;

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
import java.util.List;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.Logger;
import euphoria.psycho.tasks.Request;
import euphoria.psycho.tasks.RequestQueue;
import euphoria.psycho.tasks.RequestQueue.RequestEvent;
import euphoria.psycho.tasks.RequestQueue.RequestEventListener;
import euphoria.psycho.tasks.TaskStatus;
import euphoria.psycho.tasks.VideoActivity;
import euphoria.psycho.tasks.VideoHelper;
import euphoria.psycho.tasks.VideoManager;
import euphoria.psycho.tasks.VideoTask;

import static euphoria.psycho.tasks.VideoHelper.showNotification;


public class VideoService extends Service implements RequestEventListener {

    public static final String CHECK_UNFINISHED_VIDEO_TASKS = "CheckUnfinishedVideoTasks";
    public static final String DOWNLOAD_CHANNEL = "DOWNLOAD";
    public static final String KEY_VIDEO_LIST = "video_list";
    private RequestQueue mQueue;
    private NotificationManager mNotificationManager;

    private File mVideoDirectory;

    public void checkUncompletedVideoTasks() {
        // Query all tasks that have not been completed
        List<VideoTask> videoTasks = euphoria.psycho.tasks.VideoManager
                .newInstance(this)
                .getDatabase()
                .getPendingVideoTasks();
        if (videoTasks.size() == 0) {
            stopSelf();
            return;
        }
        for (VideoTask videoTask : videoTasks) {
            videoTask.DownloadedFiles = 0;
            submitTask(videoTask);
        }
    }

    private VideoTask createTask(String uri, String fileName, String content) {
        VideoTask videoTask = new VideoTask();
        videoTask.Uri = uri;
        videoTask.FileName = fileName;
        videoTask.Directory = new File(mVideoDirectory, fileName).getAbsolutePath();
        videoTask.Content = content;
        long result = euphoria.psycho.tasks.VideoManager
                .getInstance()
                .getDatabase()
                .insertVideoTask(videoTask);
        if (result == -1) {
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
            String[] infos = euphoria.psycho.tasks.VideoHelper.getInfos(uri);
            if (infos == null) {
                toastTaskFailed(getString(R.string.failed_to_get_video_list));
                return;
            }
            // Check whether the task has been added to the task queue
            if (euphoria.psycho.tasks.VideoHelper.checkTask(this, mQueue, infos[1])) {
                return;
            }
            // Query task from the database
            VideoTask videoTask = euphoria.psycho.tasks.VideoManager.getInstance().getDatabase().getVideoTask(infos[1]);
            if (videoTask == null) {
                videoTask = createTask(uri, infos[1], infos[0]);
                if (videoTask == null) {
                    toastTaskFailed(getString(R.string.failed_to_create_task));
                    return;
                }
            } else {
                if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
                    toastTaskFinished();
                    return;
                }
                videoTask.DownloadedFiles = 0;
            }
            submitTask(videoTask);

        }).start();
    }

    private void submitTask(VideoTask videoTask) {
        euphoria.psycho.tasks.VideoManager.post(() -> {
            euphoria.psycho.tasks.Request request = new euphoria.psycho.tasks.Request(VideoService.this, videoTask, euphoria.psycho.tasks.VideoManager.getInstance(), euphoria.psycho.tasks.VideoManager.getInstance().getHandler());
            request.setRequestQueue(mQueue);
            mQueue.add(request);
        });
    }

    private void toastTaskFailed(String message) {
        euphoria.psycho.tasks.VideoManager.post(() -> {
            Toast.makeText(VideoService.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void toastTaskFinished() {
        euphoria.psycho.tasks.VideoManager.post(() -> {
            Toast.makeText(VideoService.this, "视频已下载", Toast.LENGTH_SHORT).show();
        });
    }

    private void tryStop() {
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
        // Send a task finished broadcast
        // to the activity for display the download progress
        // if it is already opened it should close
        sendBroadcast(new Intent(VideoActivity.ACTION_FINISH));
        // Try to open the video list
        // because the new version of the Android system
        // may restrict the app to open activity from the service
        euphoria.psycho.tasks.VideoHelper.startVideoListActivity(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mVideoDirectory = euphoria.psycho.tasks.VideoHelper.setVideoDownloadDirectory(this);
        mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            euphoria.psycho.tasks.VideoHelper.createNotificationChannel(this, mNotificationManager);
        }
        mQueue = VideoManager.newInstance(this).getQueue();
        mQueue.addRequestEventListener(this);
        startForeground(android.R.drawable.stat_sys_download,
                VideoHelper.getBuilder(this)
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
        if (event == RequestEvent.REQUEST_QUEUED) {
            int[] counts = mQueue.count();
            showNotification(this, mNotificationManager, counts);
        } else if (event == RequestEvent.REQUEST_FINISHED) {
            int[] counts = mQueue.count();
            if (counts[1] > 0) {
                showNotification(this, mNotificationManager, counts);
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
            if (intent.getAction() != null && intent.getAction().equals(CHECK_UNFINISHED_VIDEO_TASKS))
                checkUncompletedVideoTasks();
            return START_NOT_STICKY;
        }
        submitRequest(uri.toString());
        return super.onStartCommand(intent, flags, startId);
    }
}