package euphoria.psycho.videos;

import android.app.ProgressDialog;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.videos.XVideosRedShare.Callback;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.NetShare;

public class Porn91Share {
    private static void get91PornVideo(String value, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("(?<=src=').*?(?=')");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            value = matcher.group();
            Helper.viewVideo(mainActivity,value);
        }
    }

    public static boolean parsing91Porn(MainActivity mainActivity) {
        String uri = mainActivity.getWebView().getUrl();
        if (uri.contains("91porn.com/")) {
            ProgressDialog progressDialog = DialogShare.createProgressDialog(mainActivity);
            Porn91Share.performTask(uri, value -> mainActivity.runOnUiThread(() -> {
                if (value != null) {
                    String script = FileShare.readAssetString(mainActivity, "encode.js");
                    mainActivity.getWebView().evaluateJavascript(script + value, value1 -> {
                        if (value1 != null) {
                            get91PornVideo(value1, mainActivity);
                        }
                    });
                }
                progressDialog.dismiss();
            }));
            return true;
        }
        return false;
    }

    private static void performTask(String uri, Callback callback) {
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
