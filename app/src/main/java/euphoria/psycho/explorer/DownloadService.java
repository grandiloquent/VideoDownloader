package euphoria.psycho.explorer;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import euphoria.psycho.share.Logger;

public class DownloadService extends Service implements DownloadNotifier {
    private static final String ACTION_DISMISS_DOWNLOAD = "euphoria.psycho.explorer.ACTION_DISMISS_DOWNLOAD";
    private static final String DOWNLOAD = "DOWNLOAD";
    private static final int NOTIFICATION_ID = android.R.drawable.stat_sys_download;
    private NotificationManager mNotificationManager;
    private BroadcastReceiver mDismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(String.format("onReceive: %s", ""));
            stopSelf();
        }
    };
    private boolean mRegistered = false;

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

    private void updateNotification() {
        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            builder = new Notification.Builder(this,
                    DOWNLOAD);

        } else {
            builder = new Notification.Builder(this);
        }
        builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setLocalOnly(true);
        builder.setContentTitle("下载视频")
                .setContentText("")
                //.setContentIntent(pairIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setColor(getColor(android.R.color.primary_text_dark))
                .setOngoing(true);
        //.addAction(pairAction)
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DISMISS_DOWNLOAD);
        registerReceiver(mDismissReceiver, filter);
        mRegistered = true;
        builder.setProgress(100, 20, false);
        mNotificationManager.notify("1", 0, builder.build());
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
    public void downloadStart(String uri, int total) {
    }

    @Override
    public void mergeVideoCompleted(String outPath) {
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
        return super.onStartCommand(intent, flags, startId);
    }
}