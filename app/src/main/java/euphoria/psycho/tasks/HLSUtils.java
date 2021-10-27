package euphoria.psycho.tasks;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;

public class HLSUtils {

    public static File createDownloadVideoFile(VideoTask videoTask) {
        return new File(videoTask.Directory,
                videoTask.FileName + ".mp4");
    }

    public static String createVideoDownloadDirectory(Context context, String fileName) {
        File directory = new File(getVideoDirectory(context), fileName);
        if (!directory.exists()) {
            boolean result = directory.mkdirs();
            if (!result) return null;
        }
        return directory.getAbsolutePath();
    }

    public static HLSInfo getHLSInfo(String uri) {
        String m3u8String = null;
        String fileName = null;
        try {
            m3u8String = getString(uri);
        } catch (Exception ignored) {
        }
        if (m3u8String == null) {
            return null;
        }
        try {
            fileName = KeyShare.toHex(KeyShare.md5encode(m3u8String));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (fileName == null) {
            return null;
        }
        return new HLSInfo(fileName, m3u8String);
    }

    public static String getString(String uri) throws IOException {
        URL url = new URL(uri);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, null, new java.security.SecureRandom());
            sc.createSSLEngine();
            urlConnection.setSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            Logger.e(String.format("getString, %s %s", uri, code));
            return null;
        }
    }

    public static File getVideoDirectory(Context context) {
        File directory =
                //FileShare.isHasSD() ? new File(FileShare.getExternalStoragePath(this), "Videos") :
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!directory.exists()) {
            boolean result = directory.mkdirs();
            if (!result) return null;
        }
        return directory;
    }
}
