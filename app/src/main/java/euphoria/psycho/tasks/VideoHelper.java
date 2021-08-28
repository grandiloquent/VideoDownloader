package euphoria.psycho.tasks;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;

import java.io.File;

import androidx.annotation.RequiresApi;
import euphoria.psycho.explorer.R;
import euphoria.psycho.explorer.VideoListActivity;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.utils.M3u8Utils;

public class VideoHelper {
    public static void deleteVideoDirectory(Context context) {
        File directory =
                //FileShare.isHasSD() ? new File(FileShare.getExternalStoragePath(this), "Videos") :
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        FileShare.recursivelyDeleteFile(directory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public static void startVideoActivity(Context context) {
        Intent v = new Intent(context, VideoActivity.class);
        v.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(v);
    }

    public static boolean checkTask(Context context, RequestQueue q, String fileName) {
        if (q.getCurrentRequests()
                .stream()
                .anyMatch(r -> r.getVideoTask().FileName.equals(fileName))) {
            context.sendBroadcast(new Intent(VideoActivity.ACTION_REFRESH));
            return true;
        }
        return false;
    }

    @RequiresApi(api = VERSION_CODES.O)
    public static void createNotificationChannel(Context context, NotificationManager manager) {
        final NotificationChannel notificationChannel = new NotificationChannel(
                VideoService.DOWNLOAD_CHANNEL,
                context.getString(R.string.channel_download_videos),
                NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(notificationChannel);
    }

    public static Notification.Builder getBuilder(Context context) {
        Builder builder;
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            builder = new Builder(context,
                    VideoService.DOWNLOAD_CHANNEL);
        } else {
            builder = new Builder(context);
        }
        builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setLocalOnly(true)
                .setColor(context.getColor(android.R.color.primary_text_dark))
                .setOngoing(true);
        return builder;
    }

    public static String[] getInfos(String uri) {
        String m3u8String;
        String fileName;
        try {
            m3u8String = M3u8Utils.getString(uri);
            if (m3u8String == null) {
                return null;
            }
            fileName = KeyShare.toHex(KeyShare.md5encode(m3u8String));
            if (fileName == null) {
                return null;
            }
        } catch (Exception ignored) {
            return null;
        }
        return new String[]{m3u8String, fileName};
    }

    public static File setVideoDownloadDirectory(Context context) {
        File directory =
                //FileShare.isHasSD() ? new File(FileShare.getExternalStoragePath(this), "Videos") :
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (!directory.exists()) {
            boolean result = directory.mkdirs();
            if (!result) return null;
        }
        return directory;
    }

    public static void startVideoListActivity(Context context) {
        Intent intent = new Intent(context, VideoListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
