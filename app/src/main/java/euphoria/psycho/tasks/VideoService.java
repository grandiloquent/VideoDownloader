package euphoria.psycho.tasks;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import euphoria.psycho.explorer.R;
import euphoria.psycho.explorer.VideoListActivity;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.tasks.RequestQueue.RequestEvent;
import euphoria.psycho.tasks.RequestQueue.RequestEventListener;
import euphoria.psycho.utils.M3u8Utils;

public class VideoService extends Service implements RequestEventListener {

    private static final String DOWNLOAD_CHANNEL = "DOWNLOAD";
    private RequestQueue mQueue;
    private NotificationManager mNotificationManager;
    private File mDirectory;

    private static Notification.Builder getBuilder(Context context) {
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

    @RequiresApi(api = VERSION_CODES.O)
    private void createNotificationChannel(Context context) {
        final NotificationChannel notificationChannel = new NotificationChannel(
                VideoService.DOWNLOAD_CHANNEL,
                context.getString(R.string.channel_download_videos),
                NotificationManager.IMPORTANCE_LOW);
        mNotificationManager.createNotificationChannel(notificationChannel);
    }

    private VideoTask createTask(String uri, String fileName, String content) {
        VideoTask videoTask = new VideoTask();
        videoTask.Uri = uri;
        videoTask.FileName = fileName;
        videoTask.Directory = new File(mDirectory, fileName).getAbsolutePath();
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
        if (mQueue != null && mQueue.getCurrentRequests().size() == 0) {
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            stopSelf();
            // Send a task finished broadcast
            // to the activity for display the download progress
            // if it is already open, then it should be closed now
            sendBroadcast(new Intent("euphoria.psycho.tasks.FINISH"));
            // Try to open the video list
            // because the new version of the Android system
            // may restrict the app to open activity from the service
            Intent intent = new Intent(this, VideoListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
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
        FileShare.initialize(this);
        mDirectory = FileShare.isHasSD() ? new File(FileShare.getExternalStoragePath(this), "Videos") :
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (!mDirectory.exists()) {
            mDirectory.mkdirs();
        }
        mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            createNotificationChannel(this);
        }
        VideoManager.newInstance(this);
        mQueue = new RequestQueue();
        VideoManager.getInstance().setQueue(mQueue);
        mQueue.addRequestEventListener(this);
        mQueue.start();
        startForeground(android.R.drawable.stat_sys_download, getBuilder(this)
                .setContentText(getString(R.string.download_ready))
                .build());

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
    public void onRequestEvent(Request Request, int event) {
        if (
                event == RequestEvent.REQUEST_FINISHED
                        || event == RequestEvent.REQUEST_QUEUED) {
            Notification.Builder builder = getBuilder(this);
            builder.setContentText(String.format("正在下载 %s 个视频", mQueue.getCurrentRequests().size()));
            mNotificationManager.notify(android.R.drawable.stat_sys_download, builder.build());
        }
        if (event == RequestEvent.REQUEST_FINISHED && mQueue.getCurrentRequests().size() == 0) {
            tryStop();
            toastTaskFinished();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get the video download address from the intent
        Uri uri;
        if (intent == null || ((uri = intent.getData()) == null)) {
            return START_NOT_STICKY;
        }
        new Thread(() -> {
            // Calculate the hash value of the m3u8 content
            // as the file name and unique Id,
            // try to avoid downloading the video repeatedly
            String m3u8String;
            String fileName;
            try {
                m3u8String = M3u8Utils.getString(uri.toString());
                if (m3u8String == null) {
                    toastTaskFailed();
                    return;
                }
                fileName = KeyShare.toHex(KeyShare.md5encode(m3u8String));
                if (fileName == null) {
                    toastTaskFailed();
                    return;
                }
            } catch (Exception ignored) {
                toastTaskFailed();
                return;
            }
            // Query task from the database
            VideoTask videoTask = VideoManager.getInstance().getDatabase().getVideoTask(fileName);
            if (videoTask == null) {
                videoTask = createTask(uri.toString(), fileName, m3u8String);
                if (videoTask == null) {
                    toastTaskFailed();
                    return;
                }
            } else {
                if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
                    toastTaskFinished();
                    return;
                }
            }
            submitTask(videoTask);

        }).start();
        return super.onStartCommand(intent, flags, startId);
    }


}
