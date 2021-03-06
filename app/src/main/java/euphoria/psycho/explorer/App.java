package euphoria.psycho.explorer;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.tencent.bugly.crashreport.CrashReport;

public class App extends Application {
    private static Context sContext;

    public static Context getContext() {
        return sContext;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        CrashReport.UserStrategy userStrategy = new CrashReport.UserStrategy(this);
        CrashReport.initCrashReport(this, "97528631ea", true, userStrategy);
    }


}