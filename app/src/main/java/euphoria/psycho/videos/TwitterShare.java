package euphoria.psycho.videos;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Process;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import androidx.annotation.NonNull;
import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

public class TwitterShare {


    public interface Callback {
        void run(List<TwitterVideo> videoList);
    }


    public static List<TwitterVideo> extractTwitterVideo(String id) throws IOException, JSONException {
        URL url = new URL("https://twittervideodownloaderpro.com/twittervideodownloadv2/index.php");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.56 Mobile Safari/537.36");
        HashMap<String, String> postDataParams = new HashMap<>();
        postDataParams.put("id", id);
        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(getPostDataString(postDataParams));
        writer.flush();
        writer.close();
        os.close();
        int statusCode = connection.getResponseCode();
        if (statusCode == 200) {
            StringBuilder sb = new StringBuilder();
            InputStream in;
            String contentEncoding = connection.getHeaderField("Content-Encoding");
            if (contentEncoding != null && contentEncoding.equals("gzip")) {
                in = new GZIPInputStream(connection.getInputStream());
            } else {
                in = connection.getInputStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
            reader.close();
            JSONObject object = new JSONObject(sb.toString());
            if (object.has("state") && object.getString("state").equals("success")) {
                if (object.has("videos")) {
                    JSONArray videos = object.getJSONArray("videos");
                    List<TwitterVideo> twitterVideos = new ArrayList<>();
                    for (int i = 0; i < videos.length(); i++) {
                        JSONObject video = videos.getJSONObject(i);
                        TwitterVideo twitterVideo = new TwitterVideo();
                        if (video.has("duration")) {
                            twitterVideo.duration = video.getLong("duration");
                        }
                        if (video.has("size")) {
                            twitterVideo.size = video.getLong("size");
                        }
                        if (video.has("url")) {
                            twitterVideo.url = video.getString("url");
                        }
                        twitterVideos.add(twitterVideo);
                    }
                    return twitterVideos;
                }
            }
        }
        return null;
    }

    private static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }


    public static class TwitterVideo {
        public long duration;
        public long size;
        public String url;

        @NonNull
        @Override
        public String toString() {
            return super.toString();
        }
    }

    private static void performTask(String uri, Callback callback) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                List<TwitterVideo> videoList = extractTwitterVideo(uri);
                if (callback != null)
                    callback.run(videoList);
            } catch (Exception e) {
                Logger.d(String.format("performTask: %s", e.getMessage()));
            }
            if (callback != null)
                callback.run(null);
        }).start();
    }

    public static boolean parsingVideo(MainActivity mainActivity) {
        String uri = mainActivity.getWebView().getUrl();
        if (uri.contains(".twitter.com/i/")) {
            ProgressDialog progressDialog = DialogShare.createProgressDialog(mainActivity);
            performTask(StringShare.substringAfterLast(mainActivity.getWebView().getUrl(), "/"), value -> mainActivity.runOnUiThread(() -> {
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

    private static void launchDialog(MainActivity mainActivity, List<TwitterVideo> videoList) throws IOException {
        String[] names = new String[videoList.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = FileShare.formatFileSize(videoList.get(i).size);
        }
        new AlertDialog.Builder(mainActivity)
                .setItems(names, (dialog, which) -> {
                    Helper.viewVideo(mainActivity, videoList.get(which).url);
                })
                .show();

    }
    //
}
