package euphoria.psycho.tasks;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class HLSDownloadService extends Service {


    public static final String CHECK_UNFINISHED_VIDEO_TASKS = "CHECK_UNFINISHED_VIDEO_TASKS";
    public static final String KEY_VIDEO_LIST = "VIDEO_LIST";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
