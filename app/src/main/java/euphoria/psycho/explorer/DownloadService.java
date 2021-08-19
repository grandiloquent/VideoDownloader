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

import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_FATAL;

public class DownloadService extends Service implements DownloadNotifier {
    private static final String ACTION_DISMISS_DOWNLOAD = "euphoria.psycho.explorer.ACTION_DISMISS_DOWNLOAD";
    private static final String DOWNLOAD = "DOWNLOAD";
    private static final int NOTIFICATION_ID = android.R.drawable.stat_sys_download;
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

    @RequiresApi(api = VERSION_CODES.O)
    private static void createNotificationChannel(Context context) {
        final NotificationChannel notificationChannel = new NotificationChannel(
                DOWNLOAD,
                "下载视频频道",
                NotificationManager.IMPORTANCE_LOW);
        NotificationManager mgr = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.createNotificationChannel(notificationChannel);
    }


    @Override
    public void downloadCompleted(String uri, String directory) {
    }

    @Override
    public void downloadFailed(String uri, String message) {
    }

    @Override
    public void downloadProgress(String uri, String fileName) {
    }

    @Override
    public void downloadProgress(String uri, int currentSize, int total, long downloadBytes, long speed) {
    }

    @Override
    public void downloadStart(DownloadTaskInfo downloadTaskInfo) {
    }

    private File mDirectory;

    @Override
    public void mergeVideoCompleted(String outPath) {
    }

    private void initializeRootDirectory() {
        //mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getParentFile()
        mDirectory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (!mDirectory.exists()) {
            mDirectory.mkdirs();
        }
    }

    @Override
    public void mergeVideoFailed(String message) {
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
            createNotificationChannel(this);
        }
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mDatabase = new DownloadTaskDatabase(this, getDatabasePath());
        mExecutor = Executors.newSingleThreadExecutor();
        initializeRootDirectory();
    }

    private String getDatabasePath() {
        return new File(getExternalCacheDir(), "tasks.db").getAbsolutePath();
    }

    @Override
    public void onDestroy() {
        if (mRegistered) {
            unregisterReceiver(mDismissReceiver);
            mRegistered = false;
        }
        super.onDestroy();
    }

    private static final Object mLock = new Object();


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
            thread = new DownloadThread(this, taskInfo, this);
            taskInfo = new DownloadTaskInfo();
            taskInfo.Uri = downloadUri.toString();
            taskInfo.FileName = DownloadUtils.getDownloadFileName(this, taskInfo.Uri).toString();
            synchronized (mLock) {
                long result = mDatabase.insertDownloadTaskInfo(taskInfo);
            }

        }
        if (taskInfo.Status == STATUS_FATAL) {
            Toast.makeText(this, "无法下载此视频", Toast.LENGTH_LONG).show();
            return START_NOT_STICKY;
        }
        if (thread == null) {
            thread = new DownloadThread(this, taskInfo, this);
        }
        mExecutor.execute(thread);
        return super.onStartCommand(intent, flags, startId);
    }


}