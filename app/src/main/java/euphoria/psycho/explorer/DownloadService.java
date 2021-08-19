package euphoria.psycho.explorer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;

public class DownloadService extends Service {
    private static final String ACTION_DISMISS_DOWNLOAD = "euphoria.psycho.explorer.ACTION_DISMISS_DOWNLOAD";
    private static final String DOWNLOAD = "DOWNLOAD";
    private static final int NOTIFICATION_ID = android.R.drawable.stat_sys_download;
    private BroadcastReceiver mDismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopForeground(true);
            stopSelf();
        }
    };
    private boolean mRegistered = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final NotificationChannel notificationChannel = new NotificationChannel(
                    DOWNLOAD,
                    "下载视频频道",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager mgr = (NotificationManager) this
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            mgr.createNotificationChannel(notificationChannel);
            builder = new Notification.Builder(this,
                    DOWNLOAD);

        } else {
            builder = new Notification.Builder(this);
        }
        builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setLocalOnly(true);
        PendingIntent dismissIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_DISMISS_DOWNLOAD), PendingIntent.FLAG_ONE_SHOT);
        Notification.Action dismissAction = new Notification.Action.Builder(0, "取消", dismissIntent).build();
        builder.setContentTitle("下载视频")
                .setContentText("")
                //.setContentIntent(pairIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setColor(getColor(android.R.color.primary_text_dark))
                //.addAction(pairAction)
                .addAction(dismissAction);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DISMISS_DOWNLOAD);
        registerReceiver(mDismissReceiver, filter);
        mRegistered = true;
        startForeground(NOTIFICATION_ID, builder.getNotification());
        return super.onStartCommand(intent, flags, startId);

    }
}