package euphoria.psycho.videos;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Process;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.videos.XVideosRedShare.Callback;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

public class AcFunShare {

    public static boolean parsingVideo(MainActivity mainActivity) {
        String uri = mainActivity.getWebView().getUrl();
        if (uri.contains(".acfun.cn")) {
            ProgressDialog progressDialog = DialogShare.createProgressDialog(mainActivity);
            performTask(String.format("https://www.acfun.cn/v/ac%s", Uri.parse(uri).getQueryParameter("ac")), value -> mainActivity.runOnUiThread(() -> {
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
            String resposne = null;
            try {
                resposne = getVideoUrl(uri);
                if (resposne == null) {
                    if (callback != null)
                        callback.run(null);
                    return;
                }
                String json = StringShare.substring(resposne, "window.pageInfo = window.videoInfo =", ";");
                String ksPlayJson = getKsPlayJson(json);
                if (ksPlayJson == null) {
                    if (callback != null)
                        callback.run(null);
                    return;
                }
                JSONObject object = new JSONObject(ksPlayJson);
                if (object.has("adaptationSet")) {
                    JSONObject adaptationSet = object.getJSONArray("adaptationSet")
                            .getJSONObject(0);
                    if (adaptationSet.has("representation")) {
                        JSONArray representation = adaptationSet.getJSONArray("representation");
                        resposne = representation.getJSONObject(0).getString("url");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (callback != null)
                callback.run(resposne);
        }).start();
    }


    private static String getKsPlayJson(String json) throws JSONException {
        JSONObject object = new JSONObject(json);
        if (object.has("currentVideoInfo")) {
            JSONObject currentVideoInfo = object.getJSONObject("currentVideoInfo");
            if (currentVideoInfo.has("ksPlayJson")) {
                return currentVideoInfo.getString("ksPlayJson");
            }
        }
        return null;
    }

    public static String getVideoUrl(String uri) throws IOException {
        URL url = new URL(uri);
        Logger.d(String.format("getVideoUrl: %s", uri));
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        urlConnection.setRequestProperty("Cache-Control", "max-age=0");
        urlConnection.setRequestProperty("Connection", "keep-alive");
        urlConnection.setRequestProperty("Cookie", "_did=web_7905657322D3709C; csrfToken=dCdPp3KoTot2nYxsvfu9fVNW; session_id=435802354EBAABE1; webp_supported=%7B%22lossy%22%3Atrue%2C%22lossless%22%3Atrue%2C%22alpha%22%3Atrue%2C%22animation%22%3Atrue%7D; lsv_js_player_v2_main=60a848; safety_id=AALY1oxvByNzxYDsSvsVLxu0; _did=web_7905657322D3709C; Hm_lvt_2af69bc2b378fb58ae04ed2a04257ed1=1628217964,1628289436; cur_req_id=5125551399237364_self_b92bea96ee6da9fec621bfd132e8a9f9; cur_group_id=5125551399237364_self_b92bea96ee6da9fec621bfd132e8a9f9_0; Hm_lpvt_2af69bc2b378fb58ae04ed2a04257ed1=1628289457; WEBLOGGER_INCREAMENT_ID_KEY=44; WEBLOGGER_HTTP_SEQ_ID=42");
        urlConnection.setRequestProperty("Referer", "https://www.acfun.cn/");
        urlConnection.setRequestProperty("sec-ch-ua", "Chromium\";v=\"92\", \" Not A;Brand\";v=\"99\", \"Google Chrome\";v=\"92");
        urlConnection.setRequestProperty("sec-ch-ua-mobile", "?0");
        urlConnection.setRequestProperty("Sec-Fetch-Dest", "document");
        urlConnection.setRequestProperty("Sec-Fetch-Mode", "navigate");
        urlConnection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        urlConnection.setRequestProperty("Sec-Fetch-User", "?1");
        urlConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36");
        urlConnection.setInstanceFollowRedirects(false);
//        Map<String, List<String>> listMap = urlConnection.getHeaderFields();
//        for (Entry<String, List<String>> header : listMap.entrySet()) {
//            Logger.d(String.format("getVideoUrl: %s = %s", header.getKey(), Share.join(";", header.getValue())));
//        }
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }
    }

}
