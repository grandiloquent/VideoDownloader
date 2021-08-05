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
    /*

     <!--

				   document.write(strencode2("%3c%73%6f%75%72%63%65%20%73%72%63%3d%27%68%74%74%70%73%3a%2f%2f%63%64%6e%2e%39%31%70%30%37%2e%63%6f%6d%2f%2f%6d%33%75%38%2f%35%30%32%35%30%30%2f%35%30%32%35%30%30%2e%6d%33%75%38%3f%73%74%3d%64%65%42%2d%36%30%63%54%67%45%39%62%63%45%64%30%48%53%49%4f%62%67%26%65%3d%31%36%32%38%30%39%30%36%37%38%27%20%74%79%70%65%3d%27%61%70%70%6c%69%63%61%74%69%6f%6e%2f%78%2d%6d%70%65%67%55%52%4c%27%3e"));
			 //-->
     */
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
        Log.e("TAG/", "Debug: getUrl, \n" + uri);
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        urlConnection.setRequestProperty("Accept-Encoding", "gzip");
        urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        urlConnection.setRequestProperty("Cache-Control", "max-age=0");
        urlConnection.setRequestProperty("Connection", "keep-alive");
        urlConnection.setRequestProperty("Referer", "https://91porn.com");
        urlConnection.setRequestProperty("Sec-Fetch-Dest", "document");
        urlConnection.setRequestProperty("Sec-Fetch-Mode", "navigate");
        urlConnection.setRequestProperty("Sec-Fetch-Site", "cross-site");
        urlConnection.setRequestProperty("Sec-Fetch-User", "?1");
        urlConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1");
        int code = urlConnection.getResponseCode();
        Map<String, List<String>> listMap = urlConnection.getHeaderFields();
        for (Entry<String, List<String>> header : listMap.entrySet()) {
            Log.e("TAG/", header.getKey() + ": " + Share.join(",", header.getValue()));

        }
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }

        // 
    }
}
