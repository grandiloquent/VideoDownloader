package euphoria.psycho.explorer;

import android.app.Application;
import android.os.StrictMode;

public class App  extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }
}
