package euphoria.psycho.downloader;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import androidx.annotation.Nullable;
import euphoria.psycho.downloader.RequestQueue.RequestEvent;
import euphoria.psycho.downloader.RequestQueue.RequestEventListener;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.KeyShare;

import static euphoria.psycho.downloader.VideoHelper.showNotification;

public class DownloaderService extends Service implements RequestEventListener {

    public static final String CHECK_UNFINISHED_VIDEO_TASKS = "CheckUnfinishedVideoTasks";
    public static final String DOWNLOAD_CHANNEL = "DOWNLOAD";
    public static final String KEY_VIDEO_LIST = "video_list";
    private RequestQueue mQueue;
    private NotificationManager mNotificationManager;

    private File mVideoDirectory;

    public void checkUncompletedVideoTasks() {
        Log.e("B5aOx2", "checkUncompletedVideoTasks");
        // Query all tasks that have not been completed
        List<DownloaderTask> videoTasks = DownloadManager
                .newInstance(this)
                .getDatabase()
                .getPendingDownloadTasks();
        if (videoTasks.size() == 0) {
            stopSelf();
            return;
        }
        for (DownloaderTask videoTask : videoTasks) {
            submitTask(videoTask);
        }
    }

    private DownloaderTask createTask(String uri, String fileName, String content) {
        Log.e("B5aOx2", "createTask");
        DownloaderTask videoTask = new DownloaderTask();
        videoTask.Uri = uri;
        videoTask.FileName = fileName;
        videoTask.Directory = new File(mVideoDirectory, fileName).getAbsolutePath();
        long result = DownloadManager
                .getInstance()
                .getDatabase()
                .insertDownloadTask(videoTask);
        if (result == -1) {
            return null;
        }
        videoTask.Id = result;
        return videoTask;
    }

    private void submitRequest(String uri) {
        Log.e("B5aOx2", "submitRequest");
        new Thread(() -> {
            // Check whether the task has been added to the task queue
            if (VideoHelper.checkTask(this, mQueue,
                    KeyShare.md5(uri))) {
                return;
            }
            // Query task from the database
//            DownloadTask videoTask = DownloadManager.getInstance()
//                    .getDatabase()
//                    .getDownloadTask(infos[1]);
//            if (videoTask == null) {
//                videoTask = createTask(uri, infos[1], infos[0]);
//                if (videoTask == null) {
//                    toastTaskFailed(getString(R.string.failed_to_create_task));
//                    return;
//                }
//            } else {
//                if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
//                    toastTaskFinished();
//                    return;
//                }
//                videoTask.DownloadedFiles = 0;
//            }
            // submitTask(videoTask);
        }).start();
    }

    private void submitTask(DownloaderTask videoTask) {
        Log.e("B5aOx2", "submitTask");
        DownloadManager.post(() -> {
            Request request = new Request(DownloaderService.this, videoTask, DownloadManager.getInstance(), euphoria.psycho.tasks.VideoManager.getInstance().getHandler());
            request.setRequestQueue(mQueue);
            mQueue.add(request);
        });
    }

    private void toastTaskFailed(String message) {
        Log.e("B5aOx2", "toastTaskFailed");
        euphoria.psycho.tasks.VideoManager.post(() -> {
            Toast.makeText(DownloaderService.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void toastTaskFinished() {
        Log.e("B5aOx2", "toastTaskFinished");
        DownloadManager.post(() -> {
            Toast.makeText(DownloaderService.this, "视频已下载", Toast.LENGTH_SHORT).show();
        });
    }

    private void tryStop() {
        Log.e("B5aOx2", "tryStop");
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
        // Send a task finished broadcast
        // to the activity for display the download progress
        // if it is already opened it should close
        sendBroadcast(new Intent(DownloaderActivity.ACTION_FINISH));
        // Try to open the video list
        // because the new version of the Android system
        // may restrict the app to open activity from the service
        euphoria.psycho.tasks.VideoHelper.startVideoListActivity(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("B5aOx2", "onBind");
        return null;
    }

    public static File createVideoDownloadDirectory() {
        Log.e("B5aOx2", "createVideoDownloadDirectory");
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Videos");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    @Override
    public void onCreate() {
        Log.e("B5aOx2", "onCreate");
        super.onCreate();
        mVideoDirectory = createVideoDownloadDirectory();
        mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            VideoHelper.createNotificationChannel(this, mNotificationManager);
        }
        mQueue = DownloadManager.newInstance(this).getQueue();
        mQueue.addRequestEventListener(this);
        startForeground(android.R.drawable.stat_sys_download,
                VideoHelper.getBuilder(this)
                        .setContentText(getString(R.string.download_ready))
                        .build());
    }

    @Override
    public void onDestroy() {
        Log.e("B5aOx2", "onDestroy");
        mQueue.removeRequestEventListener(this);
        super.onDestroy();
    }

    @Override
    public void onRequestEvent(Request Request, int event) {
        Log.e("B5aOx2", "onRequestEvent");
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

    // If the video address array is not received,
    // try to continue the last unfinished download tasks
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("B5aOx2", "onStartCommand");
        if (intent == null) {
            return START_NOT_STICKY;
        }
        String[] videoList = intent.getStringArrayExtra(KEY_VIDEO_LIST);
        if (videoList != null) {
            for (String s : videoList) {
                submitRequest(s);
            }
            return START_NOT_STICKY;
        } else {
            checkUncompletedVideoTasks();
        }
        return super.onStartCommand(intent, flags, startId);
    }

}
