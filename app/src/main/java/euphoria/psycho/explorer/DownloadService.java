package euphoria.psycho.explorer;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.widget.Toast;


import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.DownloadTaskDatabase.DownloadTaskInfo;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.utils.DownloadUtils;
import euphoria.psycho.utils.NotificationUtils;

import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_FATAL;
import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_SUCCESS;

public class DownloadService extends Service implements DownloadNotifier {
    private static final String DOWNLOAD_CHANNEL = "DOWNLOAD";
    private static final Object mLock = new Object();
    private final BroadcastReceiver mDismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(String.format("onReceive: %s", ""));
            stopSelf();
        }
    };
    private NotificationManager mNotificationManager;
    private Executor mExecutor;
    private boolean mRegistered = false;
    private DownloadTaskDatabase mDatabase;

    private void checkTasks() {
        Logger.d(String.format("checkTasks: %s", ""));
        List<DownloadTaskInfo> taskInfos = mDatabase.getDownloadTaskInfos(20);
        boolean founded = false;
        for (DownloadTaskInfo taskInfo : taskInfos) {
            if (taskInfo.Status == 1) {
                continue;
            }
            DownloadThread thread = new DownloadThread(this, taskInfo, this);
            mExecutor.execute(thread);
            founded = true;
        }
        if (!founded)
            stopSelf();
    }

    private DownloadTaskInfo createNewTask(Uri downloadUri) {
        DownloadTaskInfo taskInfo;
        taskInfo = new DownloadTaskInfo();
        taskInfo.Uri = downloadUri.toString();
        synchronized (mLock) {
            taskInfo.Id = mDatabase.insertDownloadTaskInfo(taskInfo);
        }
        return taskInfo;
    }

    @Override
    public void downloadCompleted(DownloadTaskInfo downloadTaskInfo) {
        NotificationUtils.updateDownloadCompletedNotification(this,
                DOWNLOAD_CHANNEL, downloadTaskInfo, mNotificationManager);
    }

    @Override
    public void downloadFailed(DownloadTaskInfo taskInfo) {
        NotificationUtils.downloadFailed(this, DOWNLOAD_CHANNEL, taskInfo, mNotificationManager);
        synchronized (mLock) {
            mDatabase.updateDownloadTaskInfo(taskInfo);
        }
    }

    @Override
    public void downloadProgress(DownloadTaskInfo taskInfo, int currentSize, int total, long downloadBytes, long speed, String fileName) {
        NotificationUtils.downloadProgress(this,
                DOWNLOAD_CHANNEL, taskInfo, mNotificationManager,
                (int) ((currentSize / (float) total) * 100), fileName
        );
    }

    @Override
    public void downloadStart(DownloadTaskInfo downloadTaskInfo) {
        NotificationUtils.updateDownloadStartNotification(this,
                DOWNLOAD_CHANNEL, downloadTaskInfo, mNotificationManager);
    }

    @Override
    public void mergeVideoCompleted(DownloadTaskInfo taskInfo, String outPath) {
        NotificationUtils.mergeVideoCompleted(this,
                DOWNLOAD_CHANNEL, taskInfo, mNotificationManager, outPath);
        taskInfo.Status = STATUS_SUCCESS;
        synchronized (mLock) {
            Logger.d(String.format("mergeVideoCompleted: %s", taskInfo.toString()));
            mDatabase.updateDownloadTaskInfo(taskInfo);
        }
    }

    @Override
    public void mergeVideoFailed(DownloadTaskInfo taskInfo, String message) {
        NotificationUtils.updateMergeVideoFailedNotification(this,
                DOWNLOAD_CHANNEL, taskInfo, mNotificationManager);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            NotificationUtils.createNotificationChannel(this, DOWNLOAD_CHANNEL);
        }
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mDatabase = new DownloadTaskDatabase(this, DownloadUtils.getDatabasePath(this));
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onDestroy() {
        if (mRegistered) {
            unregisterReceiver(mDismissReceiver);
            mRegistered = false;
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if the network is available,
        // but some websites may be blocked and must be accessed via VPN,
        // in the future we must also check if there is an available VPN
        if (!NetShare.isNetworkAvailable(this)) {
            Toast.makeText(this, "网络不可用", Toast.LENGTH_LONG).show();
            return START_NOT_STICKY;
        }
        // the download uri should be passed by `intent.setData`
        // otherwise we should stop immediately
        Uri downloadUri = intent.getData();
        if (downloadUri == null) {
            checkTasks();
            return START_NOT_STICKY;
        }
        DownloadTaskInfo taskInfo;
        DownloadThread thread = null;
        // in the further we should use
        // multiple threads to download videos,
        // so use a lock to ensure thread safety
        synchronized (mLock) {
            taskInfo = mDatabase.getDownloadTaskInfo(downloadUri.toString());
        }
        // If the download task does not exist in the database,
        // we will create a new task,
        // but if the video task has been executed before,
        // only the data in the database is deleted,
        // we will also check the cache file to reuse the downloaded data
        // under the premise of ensuring data consistency
        if (taskInfo == null) {
            taskInfo = createNewTask(downloadUri);
            thread = new DownloadThread(this, taskInfo, this);
        }
        // If the video cannot be downloaded due to unrecoverable conditions,
        // such as a wrong download uri or lack of permission to access the video,
        // we will terminate the task immediately
        if (taskInfo.Status == STATUS_FATAL) {
            Toast.makeText(this, "无法下载此视频", Toast.LENGTH_LONG).show();
            return START_NOT_STICKY;

        } else if (taskInfo.Status == STATUS_SUCCESS) {
            Toast.makeText(this, "视频已成功下载", Toast.LENGTH_LONG).show();
            return START_NOT_STICKY;
        }
        // Check whether the directory generated based on
        // the hash value of the download uri exists,
        // and create the directory if it does not exist
        //  checkTaskDirectory(taskInfo);
        if (thread == null) {
            thread = new DownloadThread(this, taskInfo, this);
        }
        // Submit the download task to the thread pool
        mExecutor.execute(thread);
        return super.onStartCommand(intent, flags, startId);


    }

    @Override
    public void updateDatabase(DownloadTaskInfo taskInfo) {
        synchronized (mLock) {
            mDatabase.updateDownloadTaskInfo(taskInfo);
        }
    }
}