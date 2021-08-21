package euphoria.psycho.explorer;

import android.app.Application;
import android.os.StrictMode;

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import euphoria.psycho.utils.Executors;
import euphoria.psycho.utils.ImageLoader;

public class App extends Application {
    private Executor mExecutor;
    private ImageLoader mImageLoader;

    private @NonNull
    Executor getExecutor() {
        if (mExecutor == null) {
            // TODO Adjust pool size
            mExecutor = Executors.newFixedUniqueThreadPool(
                    Runtime.getRuntime().availableProcessors() * 2 + 1);
        }
        return mExecutor;
    }

    public @NonNull
    ImageLoader getImageLoader() {
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(getExecutor());
        }
        return mImageLoader;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }
}
