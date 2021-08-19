package euphoria.psycho.utils;


import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build.VERSION_CODES;

import euphoria.psycho.explorer.DownloadTaskDatabase.DownloadTaskInfo;

public class NotificationUtils {
    public static void updateNotification(Context context, String notificationChannel, DownloadTaskInfo downloadTaskInfo, NotificationManager manager, String title, String content) {
        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            builder = new Notification.Builder(context,
                    notificationChannel);

        } else {
            builder = new Notification.Builder(context);
        }
        builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setLocalOnly(true);
        builder.setContentTitle(title)
                .setContentText(content)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setColor(context.getColor(android.R.color.primary_text_dark))
                .setOngoing(true);
        //.addAction(pairAction)
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ACTION_DISMISS_DOWNLOAD);
//        registerReceiver(mDismissReceiver, filter);
//        mRegistered = true;
//        builder.setProgress(100, 20, false);
        manager.notify(downloadTaskInfo.FileName, 0, builder.build());
    }

}
