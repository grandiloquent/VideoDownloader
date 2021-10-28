package euphoria.psycho.bilibili;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import androidx.annotation.Nullable;

public class BilibiliService extends Service {

    public static final String BILIBILI_CHANNEL = "Bilibili";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("B5aOx2", String.format("onCreate, %s", ""));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    BILIBILI_CHANNEL,
                    "Bilibili", NotificationManager.IMPORTANCE_LOW
            );
//            Notification.Builder builder = new Builder(this, BILIBILI_CHANNEL)
//                    .setCustomContentView();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        List<BilibiliTask> bilibiliTasks = new BilibiliDatabase(this).queryUnfinishedTasks();
        for (BilibiliTask bilibiliTask : bilibiliTasks) {
            Log.e("B5aOx2", String.format("onStartCommand, %s", bilibiliTask.Filename));
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
