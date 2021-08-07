package euphoria.psycho.explorer;

import android.os.Process;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.XVideosRedShare.Callback;
import euphoria.psycho.share.NetShare;

public class DouYinShare {
    public static void performTask(String uri, Callback callback) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String response = null;
            try {
                response = DouYinShare.getVideoId("https://v.douyin.com/" + uri);
                Pattern pattern = Pattern.compile("video/(\\d+)");
                Matcher matcher = null;
                if (response != null) {
                    matcher = pattern.matcher(response);
                }
                if (matcher != null && matcher.find()) {
                    String requestUri = "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=" + matcher.group(1);
                    response = DouYinShare.getUrl(requestUri);
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray itemList = jsonObject.getJSONArray("item_list");
                    jsonObject = itemList.getJSONObject(0).getJSONObject("video");
                    jsonObject = jsonObject.getJSONObject("play_addr");
                    itemList = jsonObject.getJSONArray("url_list");
                    response = itemList.getString(0).replace("playwm", "play");

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (callback != null)
                callback.run(response);
        }).start();
    }

    private static String getVideoId(String uri) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Connection", "keep-alive");
        urlConnection.setRequestProperty("sec-ch-ua", " Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Microsoft Edge\";v=\"90");
        urlConnection.setRequestProperty("Accept", "*/*");
        urlConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        urlConnection.setRequestProperty("sec-ch-ua-mobile", "?0");
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36 Edg/90.0.818.66");
        urlConnection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        urlConnection.setRequestProperty("Sec-Fetch-Mode", "cors");
        urlConnection.setRequestProperty("Sec-Fetch-Dest", "empty");
        urlConnection.setRequestProperty("Referer", "https://www.iesdouyin.com/share/video/6561991332561161476/?region=CN&mid=6561671254439365390&u_code=0&titleType=title&did=MS4wLjABAAAA2Cy8LTQsppRk4gci9RcF18kdcuNyaQRtZcZt0BGbylg&iid=MS4wLjABAAAAWHQavP6vURszBFMcxNrThBB0wrNEDWNzLdTKiuW5cI_cOJvn7h0u20Uz8R292pd2&with_sec_did=1&utm_source=copy_link&utm_campaign=client_share&utm_medium=android&app=aweme&scheme_type=1");
        urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
        urlConnection.setRequestProperty("Cookie", "MONITOR_WEB_ID=4843f090-b627-46db-bbe2-f757b4ea21a0; _tea_utm_cache_1243={%22utm_source%22:%22copy_link%22%2C%22utm_medium%22:%22android%22%2C%22utm_campaign%22:%22client_share%22}");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }
    }

    private static String getUrl(String uri) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Connection", "keep-alive");
        urlConnection.setRequestProperty("sec-ch-ua", " Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Microsoft Edge\";v=\"90");
        urlConnection.setRequestProperty("Accept", "*/*");
        urlConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        urlConnection.setRequestProperty("sec-ch-ua-mobile", "?0");
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36 Edg/90.0.818.66");
        urlConnection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        urlConnection.setRequestProperty("Sec-Fetch-Mode", "cors");
        urlConnection.setRequestProperty("Sec-Fetch-Dest", "empty");
        urlConnection.setRequestProperty("Referer", "https://www.iesdouyin.com/share/video/6561991332561161476/?region=CN&mid=6561671254439365390&u_code=0&titleType=title&did=MS4wLjABAAAA2Cy8LTQsppRk4gci9RcF18kdcuNyaQRtZcZt0BGbylg&iid=MS4wLjABAAAAWHQavP6vURszBFMcxNrThBB0wrNEDWNzLdTKiuW5cI_cOJvn7h0u20Uz8R292pd2&with_sec_did=1&utm_source=copy_link&utm_campaign=client_share&utm_medium=android&app=aweme&scheme_type=1");
        urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
        urlConnection.setRequestProperty("Cookie", "MONITOR_WEB_ID=4843f090-b627-46db-bbe2-f757b4ea21a0; _tea_utm_cache_1243={%22utm_source%22:%22copy_link%22%2C%22utm_medium%22:%22android%22%2C%22utm_campaign%22:%22client_share%22}");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }
    }

    public static String matchTikTokVideoId(String input) {
        if (input == null) return null;
        Pattern pattern = Pattern.compile("(?<=douyin.com/).+(?=/)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
} //
