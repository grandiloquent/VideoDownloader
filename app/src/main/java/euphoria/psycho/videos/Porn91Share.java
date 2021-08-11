package euphoria.psycho.videos;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.DownloadActivity;
import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.videos.XVideosRedShare.Callback;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.NetShare;

public class Porn91Share {
    private static void process91PornVideo(String value, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("(?<=src=').*?(?=')");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            value = matcher.group();
            viewVideo(mainActivity, value);
        }
    }

    public static AlertDialog.Builder createAlertDialogBuilder(Context context, String title, DialogInterface.OnClickListener p, DialogInterface.OnClickListener n) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, p)
                .setNegativeButton("下载", n);
    }

    public static void viewVideo(MainActivity mainActivity, String value) {
        try {
            String uri = "https://hxz315.com/?v=" + URLEncoder.encode(value, "UTF-8");
            createAlertDialogBuilder(mainActivity, "询问", (dialog, which) -> {
                dialog.dismiss();
                if (PreferenceShare.getPreferences().getBoolean("chrome", false)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setPackage("com.android.chrome");
                    intent.setData(Uri.parse(uri));
                    mainActivity.startActivity(intent);
                } else {
                    Helper.videoChooser(mainActivity, uri);
                }
            }, (dialog, which) -> {
                dialog.dismiss();
                Intent intent = new Intent(mainActivity, DownloadActivity.class);
                intent.setData(Uri.parse(uri));
                mainActivity.startActivity(intent);
            })
                    .setMessage("是否使用浏览器打开视频链接")
                    .show();
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    public static boolean parsing91Porn(MainActivity mainActivity, String url) {
        String uri = url == null ? mainActivity.getWebView().getUrl() : url;
        if (uri.contains("91porn.com/view_video.php?viewkey=")) {
            ProgressDialog progressDialog = DialogShare.createProgressDialog(mainActivity);
            Porn91Share.performTask(uri, encodedHtml -> mainActivity.runOnUiThread(() -> {
                if (encodedHtml == null) {
                    Toast.makeText(mainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                    return;
                }
                String script = FileShare.readAssetString(mainActivity, "encode.js");
                mainActivity.getWebView().evaluateJavascript(script + encodedHtml, videoUri -> {
                    if (videoUri != null) {
                        process91PornVideo(videoUri, mainActivity);
                    } else {
                        Toast.makeText(mainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
                    }
                });
                progressDialog.dismiss();

            }));
            return true;
        }
        return false;
    }

    private static void performTask(String uri, Callback callback) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String result = null;
            try {
                result = substringKeyCode(fetchHtml(uri));
            } catch (IOException e) {
                Log.e("TAG", "Error: performTask, " + e.getMessage() + " " + e.getCause());
            }
            if (callback != null)
                callback.run(result);
        }).start();

    }

    private static String substringKeyCode(String response) {
        if (response == null) return null;
        Pattern pattern = Pattern.compile("(?<=document\\.write\\()strencode2\\(\".*?\"\\)(?=\\);)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    //

    private static String fetchHtml(String uri) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        NetShare.addDefaultRequestHeaders(urlConnection);
        urlConnection.setRequestProperty("Referer", "https://91porn.com");
        urlConnection.setRequestProperty("X-Forwarded-For", NetShare.randomIp());
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }
    }
}
