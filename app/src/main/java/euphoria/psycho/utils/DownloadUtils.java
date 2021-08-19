package euphoria.psycho.utils;


import android.content.Context;
import android.os.Environment;

import java.io.File;

import euphoria.psycho.share.KeyShare;

public class DownloadUtils {
    public static File getDownloadFileName(Context context, String uri) {
        String directoryName = null;
        try {
            directoryName = KeyShare.toHex(KeyShare.md5encode(uri));
        } catch (Exception e) {
            e.printStackTrace();
        }
        File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        directory = new File(directory, directoryName);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }
    public static String getDatabasePath(Context context) {
        return new File(context.getExternalCacheDir(), "tasks.db").getAbsolutePath();
    }
}
