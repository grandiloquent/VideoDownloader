package euphoria.psycho.explorer;

import android.app.ProgressDialog;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;

public class XVideosShare {

    public static final String DEFAULT_SESSION_TOKEN = "5af577b26ae5f6bdxfygNhQgx7oZQu05cfbg8ZXU7xwOuBnWLCzP1p6MJAMWJ6H71IXjdZN3zMeVqiyqF_AtIbks7seX5P9bgh2MTLutiKCwucH7efUlG85EzoJZnCAvok2efoArP2wCrS1vm1VLQeUj1rvz3DEbP9oBOmP5IbCDfbITo9zcQ9gTZWO4SO2F__QuD9GR_0BHbkRbgt1WSTa4rFOj4ibROL6su9w2AEHoMXgf6uA-mLmM8Hwarx43nC9kEAYadnXeXvS2kNmYZTPaGroDhNT9kbT2NZHw7xiveTyb0H2fBcmwTVt_p9KcpxUx_N9hQ4sXYFqTzT83_ojnZ59JRP310dKC5o0Tq_1uZv3nwDSETWieIycK8B8M5f46BqCJFtoeYaPAqGlZYOe8nd5V1LU83CiRCn-rbsMf1No0I9GCwgprfw_1r1HDvhBCCy-n9dkUShZzmVW9FhBcPisp3eNteZtgmOJx3WP3iiMJNDtvCAyhks9dvYyJs2awRuMOfxzuhjqHZngXctAyv2RqN8uBc2zj6w%3D%3D";
    public static final String URL_MP_4_HD = "URL_MP4HD";

    public interface Callback {
        void run(String value);
    }

    public static boolean parsingXVideos(MainActivity mainActivity) {
        String uri = mainActivity.getWebView().getUrl();
        if (uri.contains(".xvideos.")) {
            ProgressDialog progressDialog = DialogShare.createProgressDialog(mainActivity);
            XVideosShare.performTask(uri, value -> mainActivity.runOnUiThread(() -> {
                if (value != null) {
                    mainActivity.getVideo(value);
                } else {
                    Toast.makeText(mainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
            }));
            return true;
        }
        return false;
    }

    public static void performTask(String uri, Callback callback) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String url = XVideosShare.getUrl(uri, null);
            if (callback != null)
                callback.run(url);
        }).start();
    }

    private static String getVideoUrl(String uri, String token) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        NetShare.addDefaultRequestHeaders(urlConnection);
        urlConnection.setRequestProperty("Cookie", "session_token=" + (token == null ? DEFAULT_SESSION_TOKEN : token));
        urlConnection.setRequestProperty("Referer", "https://www.xvideos.com/");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }
    }

    private static String getUrl(String url, String token) {
        Pattern pattern = Pattern.compile("(?<=/video)\\d+(?=/)");
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find()) {
            return null;
        }
        String videoId = matcher.group();
        String jsonString = null;
        try {
            jsonString = getVideoUrl("https://www.xvideos.red/video-download/" + videoId + "/", token);
            JSONObject object = new JSONObject(jsonString);
            if (object.has(URL_MP_4_HD))
                return object.getString(URL_MP_4_HD);
            else if (object.has("URL"))
                return object.getString("URL");
        } catch (IOException | JSONException e) {
            Logger.d(String.format("错误: url = %s, %s", url, e.getMessage()));
        }
        return null;
    }
}

