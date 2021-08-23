package euphoria.psycho.utils;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION_CODES;

import androidx.annotation.RequiresApi;
import euphoria.psycho.explorer.DownloadTaskDatabase.DownloadTaskInfo;
import euphoria.psycho.explorer.MovieActivity;
import euphoria.psycho.explorer.VideoListActivity;
import euphoria.psycho.share.Logger;

public class NotificationUtils {
    @RequiresApi(api = VERSION_CODES.O)
    public static void createNotificationChannel(Context context, String notificationChannelName) {
        final NotificationChannel notificationChannel = new NotificationChannel(
                notificationChannelName,
                "下载视频频道",
                NotificationManager.IMPORTANCE_LOW);
        NotificationManager mgr = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.createNotificationChannel(notificationChannel);
    }


    public static void downloadProgress(Context context, String notificationChannel, DownloadTaskInfo downloadTaskInfo, NotificationManager manager, int percent, String fileName) {
        Builder builder = getBuilder(context, notificationChannel);
        builder.setContentTitle("正在下载")
                .setContentText(fileName)
                .setProgress(100, percent, false);
        manager.notify(downloadTaskInfo.FileName, 0, builder.build());
    }

    public static void downloadFailed(Context context, String notificationChannel, DownloadTaskInfo downloadTaskInfo, NotificationManager manager) {
        Builder builder = getBuilder(context, notificationChannel);
        builder.setContentTitle("下载失败")
                .setContentText(downloadTaskInfo.Uri)
                .setOngoing(false)
                .setAutoCancel(true);
        ;
        manager.notify(downloadTaskInfo.FileName, 0, builder.build());
    }

    public static void mergeVideoCompleted(Context context, String notificationChannel, DownloadTaskInfo downloadTaskInfo, NotificationManager manager, String fileName) {
        Builder builder = getBuilder(context, notificationChannel);
        Intent viewIntent = new Intent(context, MovieActivity.class);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        viewIntent.setDataAndType(Uri.parse(fileName), "video/mp4");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), viewIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentTitle("合并完成")
                .setContentText(fileName)
                .setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(false)
                .setAutoCancel(true);
        manager.notify(downloadTaskInfo.FileName, 0, builder.build());
    }

    public static void updateMergeVideoFailedNotification(Context context, String notificationChannel, DownloadTaskInfo downloadTaskInfo, NotificationManager manager) {
        Logger.d(String.format("updateMergeVideoFailedNotification: %s", downloadTaskInfo.FileName));
        Builder builder = getBuilder(context, notificationChannel);
        builder.setContentTitle("合并视频错误")
                .setContentText(downloadTaskInfo.Uri)
                .setSmallIcon(android.R.drawable.stat_sys_download_done);
        manager.notify(downloadTaskInfo.FileName, 0, builder.build());
    }

    public static void updateDownloadStartNotification(Context context, String notificationChannel, DownloadTaskInfo downloadTaskInfo, NotificationManager manager) {
        Builder builder = getBuilder(context, notificationChannel);
        builder.setContentTitle("开始下载")
                .setContentText(downloadTaskInfo.Uri);
        manager.notify(downloadTaskInfo.FileName, 0, builder.build());
    }

    public static void updateDownloadCompletedNotification(Context context, String notificationChannel, DownloadTaskInfo downloadTaskInfo, NotificationManager manager) {
        Builder builder = getBuilder(context, notificationChannel);
        builder.setContentTitle("已完成下载")
                .setContentText(downloadTaskInfo.Uri)
                .setSmallIcon(android.R.drawable.stat_sys_download_done);
        manager.notify(downloadTaskInfo.FileName, 0, builder.build());
    }

    private static Notification.Builder getBuilder(Context context, String notificationChannel) {
        Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            builder = new Builder(context,
                    notificationChannel);
        } else {
            builder = new Builder(context);
        }
        builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setLocalOnly(true)
                .setColor(context.getColor(android.R.color.primary_text_dark))
                .setOngoing(true);
        return builder;
    }
    //.addAction(pairAction)
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ACTION_DISMISS_DOWNLOAD);
//        registerReceiver(mDismissReceiver, filter);
//        mRegistered = true;
//        builder.setProgress(100, 20, false);
}
// 添加系统程序 Gallery2 的