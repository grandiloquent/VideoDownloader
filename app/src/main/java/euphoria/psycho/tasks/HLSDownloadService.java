package euphoria.psycho.tasks;

import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.Native;

import static euphoria.psycho.tasks.HLSDownloadHelpers.createNotificationChannel;

public class HLSDownloadService extends Service {


    public static final String CHECK_UNFINISHED_VIDEO_TASKS = "CHECK_UNFINISHED_VIDEO_TASKS";
    public static final String CLOSE_SERVICE = "CLOSE_SERVICE";

    public static final String DOWNLOAD_VIDEO = "DOWNLOAD_VIDEO";
    public static final String KEY_VIDEO_LIST = "VIDEO_LIST";
    private NotificationManager mNotificationManager;

    public static void launchHLSDownloadActivity(Context context) {
        Intent i = new Intent(context, HLSDownloadActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    private void showNotification() {
        Builder builder;
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            builder = new Builder(this, DOWNLOAD_VIDEO);
        } else {
            builder = new Builder(this);
        }
        Intent i = new Intent(this, HLSDownloadActivity.class);
        builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("下载")
                .setContentIntent(PendingIntent.getActivity(this, 1, i, 0));
        mNotificationManager.notify(1, builder.build());
    }

    private void tryStopService() {
        stopSelf();
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
            createNotificationChannel(this, DOWNLOAD_VIDEO, "下载视频频道");
        }
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String intentAction = intent.getAction();
        if (intentAction != null) {
            switch (intentAction) {
                case CLOSE_SERVICE:
                    tryStopService();
                    break;
                case CHECK_UNFINISHED_VIDEO_TASKS:
                    List<HLSDownloadTask> tasks = HLSDownloadManager.getInstance(this)
                            .getDatabase().queryTasks();
                    int j = 0;
                    for (int i = 0; i < tasks.size(); i++) {
                        if (tasks.get(i).getStatus() == 5) {
                            Native.deleteDirectory(tasks.get(i).getDirectory().getAbsolutePath());
                            continue;
                        }
                        HLSDownloadManager.getInstance(this).submit(tasks.get(i));
                        j++;
                    }
                    if (j > 0) {
                        launchHLSDownloadActivity(this);
                        showNotification();
                    } else {
                        tryStopService();
                    }
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
