
package euphoria.psycho.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import euphoria.psycho.share.VideoShare;

@AnyThread
public class ImageLoader {


    private final BitmapCache mBitmapCache = new BitmapCache();
    private final Set<Map.Entry<Executor, Callback>> mCallbacks = new ArraySet<>();
    private final Executor mExecutor;
    private final Map<String, List<Map.Entry<Executor, Callback>>> mLoadCallbacks = new ArrayMap<>();

    public ImageLoader(@NonNull Executor executor) {
        mExecutor = executor;
    }

    public void addCallback(@NonNull Callback callback) {
        addCallback(callback, Executors.uiThreadExecutor());
    }

    public void addCallback(@NonNull Callback callback, @NonNull Executor executor) {
        synchronized (this) { // TODO(b/123708613) other lock
            if (!mCallbacks.add(new SimpleEntry<>(executor, callback))) {
                throw new IllegalArgumentException("Callback " + callback + " already added");
            }
        }
    }

    public void loadImage(@NonNull String uri, @NonNull Callback callback) {
        loadImage(uri, callback, Executors.uiThreadExecutor());
    }

    public void loadImage(@NonNull String uri, @NonNull Callback callback,
                          @NonNull Executor executor) {
        Bitmap bitmap;
        Runnable loader = null;
        synchronized (this) { // TODO(b/123708613) other lock
            bitmap = mBitmapCache.get(uri);
            if (bitmap == null) {
                List<Map.Entry<Executor, Callback>> callbacks = mLoadCallbacks.get(uri);
                if (callbacks == null) {
                    callbacks = new LinkedList<>();
                    mLoadCallbacks.put(uri, callbacks);
                    loader = new ImageLoaderTask(uri);
                }
                callbacks.add(new SimpleEntry<>(executor, callback));
            }
        }
        if (bitmap != null) {
            executor.execute(() -> callback.onImageLoaded(uri, bitmap));
        } else if (loader != null) {
            mExecutor.execute(loader);
        }
    }

    public void removeCallback(@NonNull Callback callback) {
        removeCallback(callback, Executors.uiThreadExecutor());
    }

    public void removeCallback(@NonNull Callback callback, @NonNull Executor executor) {
        synchronized (this) { // TODO(b/123708613) other lock
            if (!mCallbacks.remove(new SimpleEntry<>(executor, callback))) {
                throw new IllegalArgumentException("Callback " + callback + " not found");
            }
        }
    }

    @FunctionalInterface
    public interface Callback {
        void onImageLoaded(@NonNull String uri, @Nullable Bitmap bitmap);
    }

    private class ImageLoaderTask implements Runnable {
        private final String mUri;

        private ImageLoaderTask(@NonNull String uri) {
            mUri = uri;
        }

        private @Nullable
        Bitmap decodeBitmapFromByteArray(@NonNull byte[] data) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);
            options.inJustDecodeBounds = false;
            options.inSampleSize = 1; // TODO(b/123708796) add scaling
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        }

        @Override
        public void run() {
            Bitmap bitmap;
            // TODO This will always return a bitmap which is inconsistent with Q.
            bitmap = VideoShare.createVideoThumbnail(mUri);
            Set<Map.Entry<Executor, Callback>> callbacks;
            List<Map.Entry<Executor, Callback>> loadCallbacks;
            synchronized (ImageLoader.this) { // TODO(b/123708613) proper lock
                if (bitmap != null) {
                    mBitmapCache.put(mUri, bitmap);
                }
                callbacks = new ArraySet<>(mCallbacks);
                loadCallbacks = mLoadCallbacks.remove(mUri);
            }
            for (Map.Entry<Executor, Callback> callback : callbacks) {
                callback.getKey().execute(() ->
                        callback.getValue().onImageLoaded(mUri, bitmap));
            }
            for (Map.Entry<Executor, Callback> callback : loadCallbacks) {
                callback.getKey().execute(() ->
                        callback.getValue().onImageLoaded(mUri, bitmap));
            }

        }


    }
}
