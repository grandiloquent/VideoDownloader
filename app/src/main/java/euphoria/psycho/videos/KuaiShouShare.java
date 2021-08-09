package euphoria.psycho.videos;

import android.app.ProgressDialog;
import android.os.Process;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

public class KuaiShouShare {
    //
    // https://v.kuaishouapp.com/s/mbILefKd

    public interface Callback {
        void run(String videoUrl);
    }

    public static boolean parsingVideo(String string, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("https://v.kuaishouapp.com/s/\\S+");
        Matcher matcher = pattern.matcher(string);
        if (!matcher.find()) return false;
        ProgressDialog progressDialog = DialogShare.createProgressDialog(mainActivity);
        performTask(matcher.group(), value -> mainActivity.runOnUiThread(() -> {
            if (value != null) {
                Helper.viewVideo(mainActivity,value);
            } else {
                Toast.makeText(mainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
            }
            progressDialog.dismiss();
        }));
        return true;

    }

    private static void performTask(String uri, Callback callback) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                String resposne = KuaiShouShare.getString(uri);
                if (resposne != null) {
                    resposne = StringShare.substring(resposne, "\"srcNoMark\":\"", "\",");
                    if (resposne != null) {
                        if (callback != null)
                            callback.run(resposne);
                        return;
                    }
                }


            } catch (Exception e) {
                Logger.d(String.format("performTask: %s", e.getMessage()));
            }
            if (callback != null)
                callback.run(null);
        }).start();
    }


    public static String getString(String uri) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        urlConnection.setRequestProperty("Cache-Control", "no-cache");
        urlConnection.setRequestProperty("Connection", "keep-alive");
        urlConnection.setRequestProperty("Cookie", "did=web_400614f9c80d4f9dbc777dbc7288d96d; didv=1628359904000");
        urlConnection.setRequestProperty("Pragma", "no-cache");
        urlConnection.setRequestProperty("Sec-Fetch-Dest", "document");
        urlConnection.setRequestProperty("Sec-Fetch-Mode", "navigate");
        urlConnection.setRequestProperty("Sec-Fetch-Site", "none");
        urlConnection.setRequestProperty("Sec-Fetch-User", "?1");
        urlConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }

    }

}
