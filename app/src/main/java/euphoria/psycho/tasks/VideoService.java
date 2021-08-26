package euphoria.psycho.tasks;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.utils.M3u8Utils;
import euphoria.psycho.utils.NotificationUtils;

public class VideoService extends Service {

    private static final String DOWNLOAD_CHANNEL = "DOWNLOAD";
    private RequestQueue mQueue;

    @RequiresApi(api = VERSION_CODES.O)
    public static void createNotificationChannel(Context context, String channelName) {
        final NotificationChannel notificationChannel = new NotificationChannel(
                channelName,
                context.getString(R.string.channel_download_videos),
                NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(notificationChannel);
    }

    private static Notification.Builder getBuilder(Context context, String notificationChannel) {
        Builder builder;
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
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

    private VideoTask createTask(String uri, String fileName, String content) {
        VideoTask videoTask = new VideoTask();
        videoTask.Uri = uri;
        videoTask.FileName = fileName;
        videoTask.Content = content;
        long result = VideoManager
                .getInstance()
                .getDatabase()
                .insertVideoTask(videoTask);
        if (result == -1) {
            VideoManager.getInstance().getHandler().post(() -> Toast.makeText(VideoService.this, getString(R.string.insert_task_failed), Toast.LENGTH_LONG).show());
            return null;
        }
        videoTask.Id = result;
        return videoTask;
    }

    private void submitTask(VideoTask videoTask) {
        VideoManager.getInstance().getHandler()
                .post(() -> {
                    VideoManager.getInstance().addTask(videoTask);
                    Request request = new Request(VideoService.this, videoTask, VideoManager.getInstance(), VideoManager.getInstance().getHandler());
                    request.setRequestQueue(mQueue);
                    mQueue.add(request);
                });
    }

    private void toastTaskFailed() {
        VideoManager.getInstance().getHandler()
                .post(() -> {
                    Toast.makeText(VideoService.this, "视频已下载", Toast.LENGTH_LONG).show();
                    tryStop();
                });
    }

    private void toastTaskFinished() {
        VideoManager.getInstance().getHandler()
                .post(() -> {
                    Toast.makeText(VideoService.this, "视频已下载", Toast.LENGTH_LONG).show();
                    tryStop();
                });
    }

    private void tryStop() {
        if (mQueue.getCurrentRequests().size() == 0) {
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            stopSelf();
            sendBroadcast(new Intent("euphoria.psycho.tasks.FINISH"));
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
            createNotificationChannel(this, DOWNLOAD_CHANNEL);
        }
        VideoManager.newInstance(this);
        mQueue = new RequestQueue();
        VideoManager.getInstance().setQueue(mQueue);
        mQueue.start();
        startForeground(android.R.drawable.stat_sys_download, getBuilder(this,
                DOWNLOAD_CHANNEL).build());

    }

    @Override
    public void onDestroy() {
        if (mQueue != null) {
            mQueue.stop();
            mQueue = null;
        }
        super.onDestroy();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        Uri uri = intent.getData();
        if (uri == null) {
            return START_NOT_STICKY;
        }
        new Thread(() -> {
            String m3u8String;
            String fileName;
            try {
                m3u8String = M3u8Utils.getString(uri.toString());
                if (m3u8String == null) {
                    toastTaskFailed();
                    return;
                }
                fileName = KeyShare.toHex(KeyShare.md5encode(m3u8String));
            } catch (Exception ignored) {
                toastTaskFailed();
                return;
            }
            if (fileName == null) {
                toastTaskFailed();
                return;
            }
            VideoTask videoTask = VideoManager.getInstance().getDatabase().getVideoTask(fileName);
            if (videoTask == null) {
                videoTask = createTask(uri.toString(), fileName, m3u8String);
            } else {
                if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
                    toastTaskFinished();
                    return;
                }
            }
            if (videoTask == null) {
                toastTaskFailed();
                return;
            }
            submitTask(videoTask);

        }).start();
        return super.onStartCommand(intent, flags, startId);
    }


}
