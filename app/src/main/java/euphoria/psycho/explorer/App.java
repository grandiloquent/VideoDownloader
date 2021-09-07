package euphoria.psycho.explorer;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.StrictMode;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

import euphoria.psycho.share.Logger;
import euphoria.psycho.utils.FileLog;
import xcrash.ICrashCallback;
import xcrash.TombstoneParser;
import xcrash.XCrash;

public class App extends Application {
    private static Context sContext;

    public static Context getContext() {
        return sContext;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ICrashCallback callback = new ICrashCallback() {
            @Override
            public void onCrash(String logPath, String emergency) {
                if (emergency != null) {
                    debug(logPath, emergency);

                } else {
                    debug(logPath, null);
                }
            }
        };
        XCrash.init(this, new XCrash.InitParameters()
                .setJavaCallback(callback)
                .setLogDir(getExternalFilesDir("xcrash").toString())
                .setLogFileMaintainDelayMs(1000)
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        //FileShare.initialize(this);
        // VideoHelper.deleteVideoDirectory(this);
        File logDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "log");
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                throw new IllegalStateException();
            }
        }
        FileLog.setDir(logDir);
        Logger.e(String.format("onCreate, %s", XCrash.getLogDir()));
        // adb pull /storage/emulated/0/Android/data/euphoria.psycho.explorer/files/Documents/log/. log
    }

    private void debug(String logPath, String emergency) {
        // Parse and save the crash info to a JSON file for debugging.
        FileWriter writer = null;
        try {
            File debug = new File(XCrash.getLogDir() + "/debug.json");
            if (!debug.createNewFile()) {
                throw new IllegalStateException();
            }
            writer = new FileWriter(debug, false);
            writer.write(new JSONObject(TombstoneParser.parse(logPath, emergency)).toString());
        } catch (Exception ignored) {
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}