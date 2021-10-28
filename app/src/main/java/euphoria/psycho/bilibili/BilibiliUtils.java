package euphoria.psycho.bilibili;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import euphoria.psycho.share.KeyShare;

public class BilibiliUtils {

    public static String getBilibiliDatabaseName(Context context) {
        return new File(getBilibiliDirectory(context), "Bilibili.db").getAbsolutePath();
    }

    public static File getBilibiliVideoFile(Context context, String url) {
        String key = KeyShare.md5(url);
        File dir = new File(getBilibiliDirectory(context), key);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, key + ".mp4");
    }

    public static File getBilibiliThreadFile(Context context, String url) {
        String key = KeyShare.md5(url);
        File dir = new File(getBilibiliDirectory(context), key);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, key + "-01");
    }

    public static File getBilibiliDirectory(Context context) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Bilibili");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

}
