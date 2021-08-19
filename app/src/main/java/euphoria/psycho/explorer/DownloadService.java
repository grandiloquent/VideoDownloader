package euphoria.psycho.explorer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import euphoria.psycho.explorer.DownloadTaskDatabase.DownloadTaskInfo;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.utils.DownloadUtils;
import euphoria.psycho.utils.NotificationUtils;

import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_FATAL;
import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_SUCCESS;

public class DownloadService extends Service implements DownloadNotifier {
    private static final String DOWNLOAD = "DOWNLOAD";
    private static final Object mLock = new Object();
    private NotificationManager mNotificationManager;
    private Executor mExecutor;
    private BroadcastReceiver mDismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(String.format("onReceive: %s", ""));
            stopSelf();
        }
    };
    private boolean mRegistered = false;
    private DownloadTaskDatabase mDatabase;


    @Override
    public void downloadCompleted(DownloadTaskInfo downloadTaskInfo) {
        NotificationUtils.updateDownloadCompletedNotification(this,
                DOWNLOAD, downloadTaskInfo, mNotificationManager);
    }

    @Override
    public void downloadFailed(String uri, String message) {
    }


    @Override
    public void downloadProgress(DownloadTaskInfo taskInfo, int currentSize, int total, long downloadBytes, long speed, String fileName) {
        NotificationUtils.updateDownloadProgressNotification(this,
                DOWNLOAD, taskInfo, mNotificationManager,
                currentSize / total * 100, fileName
        );
    }

    @Override
    public void downloadStart(DownloadTaskInfo downloadTaskInfo) {
        NotificationUtils.updateDownloadStartNotification(this,
                DOWNLOAD, downloadTaskInfo, mNotificationManager);
    }

    @Override
    public void mergeVideoCompleted(DownloadTaskInfo taskInfo, String outPath) {
        NotificationUtils.updateMergeVideoCompletedNotification(this,
                DOWNLOAD, taskInfo, mNotificationManager, outPath);
        taskInfo.Status = 0;
        synchronized (mLock) {
            mDatabase.updateDownloadTaskInfo(taskInfo);
        }
    }

    @Override
    public void mergeVideoFailed(DownloadTaskInfo taskInfo, String message) {
        NotificationUtils.updateMergeVideoFailedNotification(this,
                DOWNLOAD, taskInfo, mNotificationManager);
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
            NotificationUtils.createNotificationChannel(this, DOWNLOAD);
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
        Uri downloadUri = intent.getData();
        if (downloadUri == null)
            return START_NOT_STICKY;
        DownloadTaskInfo taskInfo;
        DownloadThread thread = null;
        synchronized (mLock) {
            taskInfo = mDatabase.getDownloadTaskInfo(downloadUri.toString());
        }
        if (taskInfo == null) {
            taskInfo = new DownloadTaskInfo();
            taskInfo.Uri = downloadUri.toString();
            taskInfo.FileName = DownloadUtils.getDownloadFileName(this, taskInfo.Uri).toString();
            checkTaskDirectory(taskInfo);
            thread = new DownloadThread(this, taskInfo, this);
            synchronized (mLock) {
                long result = mDatabase.insertDownloadTaskInfo(taskInfo);
            }

        } else {
            Logger.d(String.format("onStartCommand: %s", taskInfo));

        }
        if (taskInfo.Status == STATUS_FATAL) {
            Toast.makeText(this, "无法下载此视频", Toast.LENGTH_LONG).show();
            return START_NOT_STICKY;
        } else if (taskInfo.Status == STATUS_SUCCESS) {
            return START_NOT_STICKY;
        }
        checkTaskDirectory(taskInfo);
        if (thread == null) {
            thread = new DownloadThread(this, taskInfo, this);
        }
        mExecutor.execute(thread);
        return super.onStartCommand(intent, flags, startId);
    }

    private void checkTaskDirectory(DownloadTaskInfo taskInfo) {
        File directory = new File(taskInfo.FileName);
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }
    }


}