package euphoria.psycho.tasks;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import androidx.annotation.RequiresApi;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;

public class HLSDownloadHelpers {
    public static final char[] InvalidFileNameChars = {'\"', '<', '>', '|', '\0', (char) 1, (char) 2, (char) 3,
            (char) 4, (char) 5, (char) 6, (char) 7, (char) 8, (char) 9, (char) 10, (char) 11, (char) 12, (char) 13,
            (char) 14, (char) 15, (char) 16, (char) 17, (char) 18, (char) 19, (char) 20, (char) 21, (char) 22,
            (char) 23, (char) 24, (char) 25, (char) 26, (char) 27, (char) 28, (char) 29, (char) 30, (char) 31, ':', '*',
            '?', '\\', '/'};

    public static void checkUnfinishedVideoTasks(Context context) {
        Intent service = new Intent(context, HLSDownloadService.class);
        service.setAction(HLSDownloadService.CHECK_UNFINISHED_VIDEO_TASKS);
        context.startService(service);
    }

    @RequiresApi(api = VERSION_CODES.O)
    public static void createNotificationChannel(Context context, String id, CharSequence name) {
        NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW);
        context.getSystemService(NotificationManager.class)
                .createNotificationChannel(channel);
    }

    public static File createVideoDownloadDirectory(Context context, String uniqueId) {
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), uniqueId);
        if (!directory.exists())
            directory.mkdirs();
        return directory;
    }

    public static File createVideoFile(Context context, String uniqueId, String fileName) {
        File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (fileName == null) {
            return new File(uniqueId, uniqueId + ".mp4");
        }
        File file = new File(directory, fileName + ".mp4");
        int i = 1;
        while (file.exists()) {
            file = new File(directory, String.format("%s-%02d.mp4", fileName, i++));
        }
        return file;
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

    private static String getValidFileName(String title) {
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : title.toCharArray()) {
            boolean founded = false;
            for (char invalidFileNameChar : InvalidFileNameChars) {
                if (invalidFileNameChar == c) {
                    founded = true;
                    break;
                }
            }
            if (!founded)
                stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }
}
