package euphoria.psycho.utils;

import android.graphics.Bitmap;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.core.graphics.BitmapCompat;

@AnyThread
class BitmapCache {
    private static final int CACHE_SIZE =
            (int) Math.min(Runtime.getRuntime().maxMemory() / 8, Integer.MAX_VALUE / 4);

    private final MemoryCache mMemoryCache = new MemoryCache(CACHE_SIZE);

    void put(@NonNull String key, @NonNull Bitmap bitmap) {
        mMemoryCache.put(key, bitmap);
    }

    @Nullable Bitmap get(@NonNull String key) {
        return mMemoryCache.get(key);
    }

    void clear() {
        mMemoryCache.evictAll();
    }

    private static class MemoryCache extends LruCache<String, Bitmap> {
        private MemoryCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
            return BitmapCompat.getAllocationByteCount(bitmap);
        }
    }
}
