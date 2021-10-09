package euphoria.psycho.downloader;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import androidx.annotation.Nullable;
import euphoria.psycho.downloader.RequestQueue.RequestEvent;
import euphoria.psycho.downloader.RequestQueue.RequestEventListener;
import euphoria.psycho.explorer.R;

import static euphoria.psycho.downloader.DownloaderHelper.showNotification;

public class DownloaderService extends Service implements RequestEventListener {

    public static final String CHECK_UNFINISHED_VIDEO_TASKS = "CheckUnfinishedVideoTasks";
    public static final String DOWNLOAD_CHANNEL = "DOWNLOAD";
    private RequestQueue mQueue;
    private NotificationManager mNotificationManager;

    private void submitTask(DownloaderTask videoTask) {
        DownloaderManager.post(() -> {
            Request request = new Request(DownloaderService.this,
                    videoTask,
                    DownloaderManager.getInstance(),
                    DownloaderManager.getInstance().getHandler());
            request.setRequestQueue(mQueue);
            mQueue.add(request);
        });
    }

    private void toastTaskFailed(String message) {
        euphoria.psycho.tasks.VideoManager.post(() -> {
            Toast.makeText(DownloaderService.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void toastTaskFinished() {
        DownloaderManager.post(() -> {
            Toast.makeText(DownloaderService.this, "视频已下载", Toast.LENGTH_SHORT).show();
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
        sendBroadcast(new Intent(DownloaderActivity.ACTION_FINISH));
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

    public static File createVideoDownloadDirectory() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Videos");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            DownloaderHelper.createNotificationChannel(this, mNotificationManager);
        }
        mQueue = DownloaderManager.newInstance(this).getQueue();
        mQueue.addRequestEventListener(this);
        startForeground(android.R.drawable.stat_sys_download,
                DownloaderHelper.getBuilder(this)
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

    // If the video address array is not received,
    // try to continue the last unfinished download tasks
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        List<DownloaderTask> downloadTasks = DownloaderManager
                .newInstance(this)
                .getDatabase()
                .getPendingDownloadTasks();
        if (downloadTasks.size() == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }
        for (DownloaderTask videoTask : downloadTasks) {
            submitTask(videoTask);
        }
        return super.onStartCommand(intent, flags, startId);
    }

}
