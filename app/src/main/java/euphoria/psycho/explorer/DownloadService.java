package euphoria.psycho.explorer;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.DownloadTaskDatabase.DownloadTaskInfo;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.utils.DownloadUtils;
import euphoria.psycho.utils.NotificationUtils;

import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_ERROR_MERGE_FILE;
import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_FATAL;
import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_SUCCESS;

public class DownloadService extends Service implements DownloadNotifier {
    private static final String DOWNLOAD_CHANNEL = "DOWNLOAD";
    private static final Handler mHandler = new Handler();
    private static final Object mLock = new Object();
    private final BroadcastReceiver mDismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    };
    private NotificationManager mNotificationManager;
    private Executor mExecutor;
    private boolean mRegistered = false;
    private DownloadTaskDatabase mDatabase;

    public static File getFinalVideoFile(File videoFile) {
        File dstFile = new File(videoFile.getParentFile().getParentFile(), videoFile.getName());
        return dstFile;
    }

    private void checkTasks() {
        List<DownloadTaskInfo> taskInfos = mDatabase.getDownloadTaskInfos();
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
        Toast.makeText(this, "成功创建下载任务：" + downloadUri, Toast.LENGTH_LONG).show();
        return taskInfo;
    }

    private void scanFile(String outputPath) {
        MediaScannerConnection.scanFile(this,
                new String[]{
                        outputPath
                }, new String[]{
                        "video/mp4"
                }, new MediaScannerConnectionClient() {
                    @Override
                    public void onMediaScannerConnected() {
                    }

                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                    }
                });
    }

    @Override
    public void downloadCompleted(DownloadTaskInfo downloadTaskInfo) {
        synchronized (mLock) {
            NotificationUtils.downloadCompleted(this,
                    DOWNLOAD_CHANNEL, downloadTaskInfo, mNotificationManager);
        }
    }

    @Override
    public void downloadFailed(DownloadTaskInfo taskInfo) {
        synchronized (mLock) {
            mDatabase.updateDownloadTaskInfo(taskInfo);
            NotificationUtils.downloadFailed(this, DOWNLOAD_CHANNEL, taskInfo, mNotificationManager);
        }
    }

    @Override
    public void downloadProgress(DownloadTaskInfo taskInfo, int currentSize, int total, long downloadBytes, long speed, String fileName) {
        synchronized (mLock) {
            NotificationUtils.downloadProgress(this,
                    DOWNLOAD_CHANNEL, taskInfo, mNotificationManager,
                    (int) ((currentSize / (float) total) * 100), fileName
            );
        }
    }

    @Override
    public void downloadStart(DownloadTaskInfo downloadTaskInfo) {
        synchronized (mLock) {
            NotificationUtils.downloadStart(this,
                    DOWNLOAD_CHANNEL, downloadTaskInfo, mNotificationManager);
        }
    }

    @Override
    public void mergeVideoCompleted(DownloadTaskInfo taskInfo, String outPath) {
        synchronized (mLock) {
            taskInfo.Status = STATUS_SUCCESS;
            mDatabase.updateDownloadTaskInfo(taskInfo);

            // Move the merged video to the upper level directory
            File videoFile = new File(outPath);
            File destinationFile = getFinalVideoFile(videoFile);
            boolean result = videoFile.renameTo(destinationFile);
            if (!result) {
                return;
            }
            // Delete the directory contains the cache files of the video
            if (videoFile.getParentFile() == null) {
                Logger.d(String.format("'%s' is null.", "videoFile.getParentFile()"));
                return;
            }
            FileShare.recursivelyDeleteFile(videoFile.getParentFile(), h -> true);
            // Request the system to scan the video
            // so that it can be shared by other programs
            scanFile(destinationFile.getAbsolutePath());

            NotificationUtils.mergeVideoCompleted(this, DOWNLOAD_CHANNEL, taskInfo, mNotificationManager, destinationFile.getAbsolutePath());
        }
        mHandler.post(() -> {
            Toast.makeText(DownloadService.this, "成功合并文件：" + outPath, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void mergeVideoFailed(DownloadTaskInfo taskInfo, String message) {
        synchronized (mLock) {
            taskInfo.Status = STATUS_ERROR_MERGE_FILE;
            mDatabase.updateDownloadTaskInfo(taskInfo);
            NotificationUtils.mergeVideoFailed(this,
                    DOWNLOAD_CHANNEL, taskInfo, mNotificationManager);
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
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            NotificationUtils.createNotificationChannel(this, DOWNLOAD_CHANNEL);
        }
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mDatabase = new DownloadTaskDatabase(this, DownloadUtils.getDatabasePath(this));
        mExecutor = Executors.newFixedThreadPool(3);


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
            Logger.d(String.format("onStartCommand: %s", "---------------->"));
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
// ^(?:[\t ]*(?:\r?\n|\r))+
// Logger.e\([^;]+\);
// // $0
