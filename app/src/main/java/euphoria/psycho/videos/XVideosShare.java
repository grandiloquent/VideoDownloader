package euphoria.psycho.videos;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Process;
import android.util.Pair;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

public class XVideosShare {
    public interface Callback {
        void run(List<Pair<String, String>> videoList);
    }

    public static boolean parsingVideo(MainActivity mainActivity, String url) {
        String uri = url == null ? mainActivity.getWebView().getUrl() : url;
        if (uri.contains("https://www.xvideos.com/video")) {
            ProgressDialog progressDialog = DialogShare.createProgressDialog(mainActivity);
            performTask(uri, value -> mainActivity.runOnUiThread(() -> {
                if (value != null) {
                    try {
                        launchDialog(mainActivity, value);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(mainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
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
            try {
                List<Pair<String, String>> videoList = new ArrayList<>();
                String hls = parseWebpage(uri, videoList);
                if (hls != null) {
                    parseHls(hls, videoList);
                    if (callback != null)
                        callback.run(videoList);
                    return;
                }

            } catch (Exception e) {
                Logger.d(String.format("performTask: %s", e.getMessage()));
            }
            if (callback != null)
                callback.run(null);
        }).start();
    }

    private static void launchDialog(MainActivity mainActivity, List<Pair<String, String>> videoList) throws IOException {
        String[] names = new String[videoList.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = videoList.get(i).first;
        }
        new AlertDialog.Builder(mainActivity)
                .setItems(names, (dialog, which) -> {
                    Helper.viewVideo(mainActivity, videoList.get(which).second);
                })
                .show();

    }

    private static void parseHls(String hlsUri, List<Pair<String, String>> videoList) throws IOException {
        String hls = getString(hlsUri);
        if (hls == null) return;
        String[] pieces = hls.split("\n");
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].startsWith("#EXT-X-STREAM-INF")) {
                String name = StringShare.substring(pieces[i], "NAME=\"", "\"");
                String url = StringShare.substringBeforeLast(hlsUri, "/") + "/" + pieces[i + 1];
                videoList.add(Pair.create(name, url));
                i++;
            }
        }

    }

    private static String parseWebpage(String pageUri, List<Pair<String, String>> videoList) throws IOException {
        String htmlCode = getString(pageUri);
        if (htmlCode == null) return null;
        String low = StringShare.substring(htmlCode, "html5player.setVideoUrlLow('", "'");
        if (low != null) {
            videoList.add(Pair.create("标清", low));
        }
        String high = StringShare.substring(htmlCode, "html5player.setVideoUrlHigh('", "'");
        if (high != null) {
            videoList.add(Pair.create("高清", high));
        }
        return StringShare.substring(htmlCode, "html5player.setVideoHLS('", "'");
    }

    private static String getString(String uri) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        urlConnection.setRequestProperty("Cache-Control", "max-age=0");
        urlConnection.setRequestProperty("Connection", "keep-alive");
        urlConnection.setRequestProperty("Referer", "https://www.xvideos.cn/");
        urlConnection.setRequestProperty("sec-ch-ua", "Chromium\";v=\"92\", \" Not A;Brand\";v=\"99\", \"Google Chrome\";v=\"92");
        urlConnection.setRequestProperty("sec-ch-ua-mobile", "?0");
        urlConnection.setRequestProperty("Sec-Fetch-Dest", "document");
        urlConnection.setRequestProperty("Sec-Fetch-Mode", "navigate");
        urlConnection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        urlConnection.setRequestProperty("Sec-Fetch-User", "?1");
        urlConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }
    }

}

