package euphoria.psycho.explorer;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import euphoria.psycho.tasks.VideoHelper;

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
        VideoHelper.deleteVideoDirectory(this);

    }
}
