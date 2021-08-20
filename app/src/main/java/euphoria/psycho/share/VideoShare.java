package euphoria.psycho.share;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.lang.reflect.Method;

public class VideoShare {


    public static Bitmap createVideoThumbnail(String filePath) {
        // MediaMetadataRetriever is available on API Level 8
        // but is hidden until API Level 10
        Class<?> clazz = null;
        Object instance = null;
        try {
            clazz = Class.forName("android.media.MediaMetadataRetriever");
            instance = clazz.newInstance();
            Method method = clazz.getMethod("setDataSource", String.class);
            method.invoke(instance, filePath);
            // The method name changes between API Level 9 and 10.
            byte[] data = (byte[]) clazz.getMethod("getEmbeddedPicture").invoke(instance);
            if (data != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (bitmap != null) return bitmap;
            }
            return (Bitmap) clazz.getMethod("getFrameAtTime").invoke(instance);
        } catch (Exception ignored) {
        } finally {
            try {
                if (instance != null) {
                    clazz.getMethod("release").invoke(instance);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

}
