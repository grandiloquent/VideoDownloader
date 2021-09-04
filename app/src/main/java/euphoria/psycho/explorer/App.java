package euphoria.psycho.explorer;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.StrictMode;

import java.io.File;

import euphoria.psycho.utils.FileLog;

public class App extends Application {
    private static Context sContext;

    public static Context getContext() {
        return sContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        //FileShare.initialize(this);
        //VideoHelper.deleteVideoDirectory(this);
        File logDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "log");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        FileLog.setDir(logDir);
        // adb pull /storage/emulated/0/Android/data/euphoria.psycho.explorer/files/Documents/log/. log
    }
}