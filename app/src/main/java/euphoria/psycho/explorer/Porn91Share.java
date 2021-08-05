package euphoria.psycho.explorer;

import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import euphoria.psycho.explorer.XVideosShare.Callback;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

public class Porn91Share {
    public static void performTask(String uri, Callback callback) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String url = null;
            try {
                url = getUrl(uri);
                Pattern pattern = Pattern.compile("(?<=document\\.write\\()strencode2\\(\".*?\"\\)(?=\\);)");
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    url = matcher.group();
                }
            } catch (IOException e) {
                Log.e("TAG", "Error: performTask, " + e.getMessage() + " " + e.getCause());

            }
            if (callback != null)
                callback.run(url);
        }).start();
    }

    private static String getUrl(String uri) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        NetShare.addDefaultRequestHeaders(urlConnection);
        urlConnection.setRequestProperty("Referer", "https://91porn.com");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }
    }
}
